package io.github.turn_based_example_game.server;

import com.esotericsoftware.kryonet.Connection;
import io.github.turn_based_example_game.Card;
import io.github.turn_based_example_game.CardColor;
import io.github.turn_based_example_game.CardRules;
import io.github.turn_based_example_game.CardStyle;
import io.github.turn_based_example_game.CardSymbol;
import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.Network.JoinGameRequest;
import io.github.turn_based_example_game.server.game.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameManager {
    private static final CardColor[] CARD_COLORS = CardColor.values();
    private static final CardStyle[] CARD_STYLES = CardStyle.values();
    private static final CardSymbol[] COLORED_ACTION_SYMBOLS = {
        CardSymbol.SKIP,
        CardSymbol.SWITCH_ORDER,
        CardSymbol.PLUS_2
    };
    private static final int WILD_CARD_COUNT = 4;
    private static final int COLORED_ACTION_CARD_COUNT = 2;
    private static final int DEFAULT_HAND_SIZE = 7;
    private static final long DRAW_STEP_DELAY_MS = 250L;
    private static final long GAME_END_DELAY_MS = 500L;
    private static final long BOT_DUO_DELAY_MS = 2000L;

    private final Random random = new Random();
    private final Opponent opponent = new Opponent();
    private final Set<Game> games;
    private final List<Account> waitList;
    private final List<Account> teamsWaitList;
    private final Map<Account, LobbyGameSession> lobbyGamesByAccount;
    private final Set<LobbyGameSession> lobbyGames;

    /** Initializes all game queues and active game tracking collections. */
    public GameManager() {
        games = new HashSet<>();
        waitList = new ArrayList<>();
        teamsWaitList = new ArrayList<>();
        lobbyGamesByAccount = new HashMap<>();
        lobbyGames = new HashSet<>();
    }

    /** Removes a finished legacy game from active tracking. */
    public void gameEnded(Game game) {
        System.out.println("Game ended: " + game + "winner: " + game.getWinner());
        game.dispose();
        games.remove(game);
    }

    /** Removes a player from matchmaking, lobby games, or active legacy games. */
    public synchronized void handlePlayerLeave(Account account) {
        waitList.remove(account);
        teamsWaitList.remove(account);
        account.setInGame(false);
        LobbyGameSession lobbyGame = lobbyGamesByAccount.get(account);
        if (lobbyGame != null) {
            disposeLobbyGame(lobbyGame);
            return;
        }
        for (Game game : games) {
            if (game.getPlayers().contains(account)) {
                game.handlePlayerLeave(account);
                break;
            }
        }
    }

    /** Creates and starts a card game from the lobby player order. */
    public synchronized void startLobbyGame(List<Account> humanAccounts, List<Network.LobbyPlayer> playersInOrder) {
        if (humanAccounts == null || humanAccounts.isEmpty() || playersInOrder == null || playersInOrder.isEmpty()) {
            return;
        }

        LobbyGameSession session = new LobbyGameSession();
        session.drawPile.addAll(createShuffledDrawPile());
        session.topPlayPileCard = drawOpeningPlayPileCard(session.drawPile);

        int humanIndex = 0;
        for (Network.LobbyPlayer player : playersInOrder) {
            Account account = null;
            if (!player.bot) {
                if (humanIndex >= humanAccounts.size()) {
                    return;
                }
                account = humanAccounts.get(humanIndex++);
            }

            LobbyGamePlayer gamePlayer = new LobbyGamePlayer(player.username, player.bot, account);
            dealStartingHand(session, gamePlayer);
            session.players.add(gamePlayer);
            if (account != null) {
                account.setInGame(true);
                lobbyGamesByAccount.put(account, session);
            }
        }

        session.currentTurnIndex = 0;
        session.turnDirection = 1;
        session.currentPlayerCanDraw = true;
        session.turnActionsLocked = false;
        refreshPlayersWithTwoCards(session);
        lobbyGames.add(session);
        broadcastLobbyGameState(session);
        resolveCurrentTurn(session);
    }

    /** Routes authenticated gameplay packets to the correct game handler. */
    public synchronized void handlePackets(Connection connection, Object object) {
        Account account = Database.findAccount(connection);
        if (account == null) {
            return;
        }

        if (object instanceof Network.GameTurnActionRequest request) {
            handleTurnAction(account, request);
            return;
        }

        if (object instanceof Network.GameDuoRequest) {
            handleGameDuo(account);
            return;
        }

        if (object instanceof Network.JoinGameRequest) {
            Network.JoinGameRequest request = (JoinGameRequest) object;

            if (account.getInGame()) {
                return;
            }
            if (waitList.contains(account)) {
                waitList.remove(account);
            }
            if (teamsWaitList.contains(account)) {
                teamsWaitList.remove(account);
            }

            if (request.mode == Network.JoinGameRequest.Mode.COMPUTER) {
                account.setInGame(true);
            } else if (request.mode == Network.JoinGameRequest.Mode.HUMAN) {
                waitList.add(account);
                if (waitList.size() == 2) {
                    for (Account acc : waitList) {
                        acc.setInGame(true);
                    }
                    List<Player> players = new ArrayList<>(waitList);
                    Game newGame = new Game(players, this);
                    games.add(newGame);
                    newGame.start();
                    waitList.clear();
                }
            } else if (request.mode == Network.JoinGameRequest.Mode.TEAMS) {
                teamsWaitList.add(account);
                if (teamsWaitList.size() == 4) {
                    for (Account acc : teamsWaitList) {
                        acc.setInGame(true);
                    }
                    List<Player> players = new ArrayList<>(teamsWaitList);
                    Game newGame = new Game(players, this);
                    games.add(newGame);
                    newGame.start();
                    teamsWaitList.clear();
                }
            }
        }

        if (object instanceof Network.LeaveGameRequest) {
            handlePlayerLeave(account);
        }
    }

    /** Validates and applies a human player's requested turn action. */
    private void handleTurnAction(Account account, Network.GameTurnActionRequest request) {
        LobbyGameSession session = lobbyGamesByAccount.get(account);
        if (session == null || session.players.isEmpty() || request == null || request.actionType == null || session.turnActionsLocked) {
            return;
        }

        LobbyGamePlayer currentPlayer = session.players.get(session.currentTurnIndex);
        if (currentPlayer.bot || currentPlayer.account != account) {
            return;
        }

        if (request.actionType == Network.GameTurnActionRequest.ActionType.PLAY_CARD) {
            handleHumanPlay(session, currentPlayer, request);
        } else if (request.actionType == Network.GameTurnActionRequest.ActionType.DRAW_CARD) {
            handleHumanDraw(session, currentPlayer);
        }
    }

    /** Handles a DUO call from a human player. */
    private void handleGameDuo(Account account) {
        LobbyGameSession session = lobbyGamesByAccount.get(account);
        if (session == null) {
            return;
        }

        LobbyGamePlayer caller = findPlayerByAccount(session, account);
        if (caller == null || session.players.isEmpty()) {
            return;
        }

        processDuoCall(session, caller, false);
    }

    /** Plays a valid human card and advances game state. */
    private void handleHumanPlay(LobbyGameSession session, LobbyGamePlayer currentPlayer, Network.GameTurnActionRequest request) {
        if (request.handIndex < 0 || request.handIndex >= currentPlayer.handCards.size()) {
            return;
        }

        Card handCard = currentPlayer.handCards.get(request.handIndex);
        if (!CardRules.canPlay(session.topPlayPileCard, handCard)) {
            return;
        }

        Card resolvedPlayedCard = CardRules.resolvePlayedCard(handCard, request.chosenColor);
        if (resolvedPlayedCard == null) {
            return;
        }

        currentPlayer.handCards.remove(request.handIndex);
        refreshPlayersWithTwoCards(session);
        session.topPlayPileCard = resolvedPlayedCard;
        if (currentPlayer.handCards.isEmpty()) {
            broadcastLobbyGameState(session);
            finishLobbyGame(session, currentPlayer.username);
            return;
        }
        if (CardRules.reversesDirection(resolvedPlayedCard)) {
            session.turnDirection *= -1;
        }
        session.pendingDrawCount = CardRules.drawPenalty(resolvedPlayedCard);
        // Add missing functions
        maybeHandleBotDuoCallout(session);
        // Here the turn needs to be resolved at the end
    }

    /** Draws cards for a human player according to turn rules. */
    private void handleHumanDraw(LobbyGameSession session, LobbyGamePlayer currentPlayer) {
        if (!session.currentPlayerCanDraw) {
            return;
        }

        if (playerHasPlayableCard(session.topPlayPileCard, currentPlayer.handCards)) {
            drawCardToPlayer(session, currentPlayer);
            advanceToNextTurn(session);
            broadcastLobbyGameState(session);
            maybeHandleBotDuoCallout(session);
            resolveCurrentTurn(session);
            return;
        }

        session.currentPlayerCanDraw = false;
        session.turnActionsLocked = true;
        while (true) {
            Card drawnCard = drawCardToPlayer(session, currentPlayer);
            boolean playable = drawnCard != null && CardRules.canPlay(session.topPlayPileCard, drawnCard);
            session.turnActionsLocked = !playable;
            broadcastLobbyGameState(session);
            if (playable) {
                session.turnActionsLocked = false;
                break;
            }
            sleepDrawDelay();
        }
    }

    /** Resolves automatic effects and bot actions for the current turn. */
    private void resolveCurrentTurn(LobbyGameSession session) {
        if (session.players.isEmpty()) {
            return;
        }

        if (session.pendingDrawCount > 0) {
            applyPendingDrawPenalty(session);
        }
        if (!session.players.isEmpty() && session.players.get(session.currentTurnIndex).bot) {
            resolveBotTurns(session);
        }
    }

    /** Applies queued draw penalties to the current player. */
    private void applyPendingDrawPenalty(LobbyGameSession session) {
        LobbyGamePlayer penalizedPlayer = session.players.get(session.currentTurnIndex);
        session.currentPlayerCanDraw = false;
        session.turnActionsLocked = true;

        int remainingPenalty = session.pendingDrawCount;
        while (remainingPenalty-- > 0) {
            drawCardToPlayer(session, penalizedPlayer);
            broadcastLobbyGameState(session);
            if (remainingPenalty > 0) {
                sleepDrawDelay();
            }
        }

        session.pendingDrawCount = 0;
        session.currentPlayerCanDraw = true;
        session.turnActionsLocked = false;
        broadcastLobbyGameState(session);
        maybeHandleBotDuoCallout(session);
    }

    /** Lets bot players act until a human turn is reached. */
    private void resolveBotTurns(LobbyGameSession session) {
        int safetyCounter = 0;
        while (!session.players.isEmpty() && session.players.get(session.currentTurnIndex).bot && safetyCounter++ < 100) {
            LobbyGamePlayer bot = session.players.get(session.currentTurnIndex);
            boolean pendingBotDuoAfterPlay = shouldBotDeclareDuoAfterPlay(session, bot);
            Opponent.Decision decision = opponent.chooseAction(session.topPlayPileCard, bot.handCards);
            if (decision.action == Opponent.Action.PLAY) {
                if (decision.handIndex < 0 || decision.handIndex >= bot.handCards.size()) {
                    break;
                }
                bot.handCards.remove(decision.handIndex);
                refreshPlayersWithTwoCards(session);
                session.topPlayPileCard = decision.playedCard;
                if (bot.handCards.isEmpty()) {
                    broadcastLobbyGameState(session);
                    finishLobbyGame(session, bot.username);
                    return;
                }
                if (CardRules.reversesDirection(decision.playedCard)) {
                    session.turnDirection *= -1;
                }
                session.pendingDrawCount = CardRules.drawPenalty(decision.playedCard);
                advanceToNextTurn(session, CardRules.turnAdvanceCount(decision.playedCard));
                broadcastLobbyGameState(session);
                if (pendingBotDuoAfterPlay && bot.handCards.size() == 2) {
                    sleepBotDuoDelay();
                    processDuoCall(session, bot, true);
                } else {
                    maybeHandleBotDuoCallout(session);
                }
                resolveCurrentTurn(session);
                return;
            }

            session.currentPlayerCanDraw = false;
            session.turnActionsLocked = true;
            while (true) {
                Card drawnCard = drawCardToPlayer(session, bot);
                boolean playable = drawnCard != null && CardRules.canPlay(session.topPlayPileCard, drawnCard);
                session.turnActionsLocked = !playable;
                broadcastLobbyGameState(session);
                if (playable) {
                    session.turnActionsLocked = false;
                    break;
                }
                sleepDrawDelay();
            }
        }
    }

    /** Sends each human player their current view of the game state. */
    private void broadcastLobbyGameState(LobbyGameSession session) {
        refreshPlayersWithTwoCards(session);
        String currentTurnUsername = session.players.get(session.currentTurnIndex).username;
        for (int i = 0; i < session.players.size(); i++) {
            LobbyGamePlayer recipient = session.players.get(i);
            if (recipient.account == null) {
                continue;
            }

            Network.GameStateUpdate update = new Network.GameStateUpdate();
            // Something is missing here
            update.topPlayPileCard = session.topPlayPileCard;
            update.currentTurnUsername = currentTurnUsername;
            update.currentPlayerCanDraw = session.currentPlayerCanDraw;
            update.turnActionsLocked = session.turnActionsLocked;
            update.showDuoButton = shouldShowDuoButton(session, recipient, currentTurnUsername);
            for (Card handCard : recipient.handCards) {
                update.playerHandCards.add(handCard);
            }
            for (LobbyGamePlayer player : session.players) {
                update.playerUsernames.add(player.username);
                update.playerHandCounts.add(player.handCards.size());
            }
            recipient.account.sendPacket(update);
        }
    }

    /** Notifies all human players that someone called DUO. */
    private void broadcastDuoEvent(LobbyGameSession session, Network.GameDuoEvent duoEvent) {
        for (LobbyGamePlayer player : session.players) {
            if (player.account != null) {
                player.account.sendPacket(duoEvent);
            }
        }
    }

    /** Advances the turn by one player. */
    private void advanceToNextTurn(LobbyGameSession session) {
        advanceToNextTurn(session, 1);
    }

    /** Advances the turn by the requested number of player steps. */
    private void advanceToNextTurn(LobbyGameSession session, int stepCount) {
        if (session.players.isEmpty()) {
            return;
        }
        int playerCount = session.players.size();
        int normalizedSteps = Math.max(1, stepCount) % playerCount;
        int nextIndex = session.currentTurnIndex + (normalizedSteps * session.turnDirection);
        session.currentTurnIndex = ((nextIndex % playerCount) + playerCount) % playerCount;
        session.currentPlayerCanDraw = true;
        session.turnActionsLocked = false;
    }

    /** Deals the default starting hand to one player. */
    private void dealStartingHand(LobbyGameSession session, LobbyGamePlayer player) {
        for (int i = 0; i < DEFAULT_HAND_SIZE; i++) {
            drawCardToPlayer(session, player);
        }
    }

    /** Draws one card from the pile into a player's hand. */
    private Card drawCardToPlayer(LobbyGameSession session, LobbyGamePlayer player) {
        Card drawnCard = drawCardFromPile(session);
        if (drawnCard != null) {
            player.handCards.add(drawnCard);
        }
        return drawnCard;
    }

    /** Rebuilds the set of players currently eligible for DUO calls. */
    private void refreshPlayersWithTwoCards(LobbyGameSession session) {
        session.playersWithTwoCards.clear();
        for (LobbyGamePlayer player : session.players) {
            if (player.handCards.size() == 2) {
                session.playersWithTwoCards.add(player.username);
            }
        }
        session.playersWhoCalledDuo.retainAll(session.playersWithTwoCards);
    }

    /** Finds the game player object tied to an account. */
    private LobbyGamePlayer findPlayerByAccount(LobbyGameSession session, Account account) {
        for (LobbyGamePlayer player : session.players) {
            if (player.account == account) {
                return player;
            }
        }
        return null;
    }

    /** Applies DUO call rules for the caller and possible penalties. */
    private void processDuoCall(LobbyGameSession session, LobbyGamePlayer caller, boolean bypassVisibilityCheck) {
        if (session == null || caller == null || session.players.isEmpty()) {
            return;
        }

        String currentTurnUsername = session.players.get(session.currentTurnIndex).username;
        if (!bypassVisibilityCheck
            && !shouldShowDuoButton(session, caller, currentTurnUsername)
            && !canSelfDeclareDuo(session, caller)) {
            return;
        }

        Network.GameDuoEvent duoEvent = new Network.GameDuoEvent();
        duoEvent.username = caller.username;
        broadcastDuoEvent(session, duoEvent);

        refreshPlayersWithTwoCards(session);
        if (session.playersWithTwoCards.contains(caller.username)) {
            session.playersWhoCalledDuo.add(caller.username);
        } else {
            applyDuoPenalty(session);
        }
        broadcastLobbyGameState(session);
    }

    /** Checks whether a player can protect themself with a DUO call. */
    private boolean canSelfDeclareDuo(LobbyGameSession session, LobbyGamePlayer caller) {
        refreshPlayersWithTwoCards(session);
        return session.playersWithTwoCards.contains(caller.username)
            && !session.playersWhoCalledDuo.contains(caller.username);
    }

    /** Gives penalty cards to players who failed to call DUO. */
    private void applyDuoPenalty(LobbyGameSession session) {
        List<LobbyGamePlayer> penalizedPlayers = new ArrayList<>();
        for (LobbyGamePlayer player : session.players) {
            if (session.playersWithTwoCards.contains(player.username) && !session.playersWhoCalledDuo.contains(player.username)) {
                penalizedPlayers.add(player);
            }
        }

        for (LobbyGamePlayer penalizedPlayer : penalizedPlayers) {
            for (int drawCount = 0; drawCount < 3; drawCount++) {
                drawCardToPlayer(session, penalizedPlayer);
                broadcastLobbyGameState(session);
                if (drawCount < 2) {
                    sleepDrawDelay();
                }
            }
        }
    }

    /** Lets a bot call out an undeclared DUO when possible. */
    private void maybeHandleBotDuoCallout(LobbyGameSession session) {
        LobbyGamePlayer botCaller = findBotDuoCalloutCandidate(session);
        if (botCaller == null) {
            return;
        }

        sleepBotDuoDelay();
        processDuoCall(session, botCaller, false);
    }

    /** Finds a bot that is allowed to call DUO on another player. */
    private LobbyGamePlayer findBotDuoCalloutCandidate(LobbyGameSession session) {
        refreshPlayersWithTwoCards(session);
        if (!hasUndeclaredTwoCardPlayer(session)) {
            return null;
        }

        String currentTurnUsername = session.players.get(session.currentTurnIndex).username;
        for (LobbyGamePlayer player : session.players) {
            if (!player.bot) {
                continue;
            }
            if (session.playersWithTwoCards.contains(player.username)) {
                continue;
            }
            if (shouldShowDuoButton(session, player, currentTurnUsername)) {
                return player;
            }
        }
        return null;
    }

    /** Checks whether any two-card player has not declared DUO. */
    private boolean hasUndeclaredTwoCardPlayer(LobbyGameSession session) {
        for (String username : session.playersWithTwoCards) {
            if (!session.playersWhoCalledDuo.contains(username)) {
                return true;
            }
        }
        return false;
    }

    /** Checks whether a bot should plan to call DUO after playing. */
    private boolean shouldBotDeclareDuoAfterPlay(LobbyGameSession session, LobbyGamePlayer bot) {
        return bot != null
            && bot.handCards.size() == 3
            && playerHasPlayableCard(session.topPlayPileCard, bot.handCards);
    }

    /** Determines whether a player should see the DUO button. */
    private boolean shouldShowDuoButton(LobbyGameSession session, LobbyGamePlayer recipient, String currentTurnUsername) {
        if (recipient == null) {
            return false;
        }

        if (anyOtherPlayerHasUndeclaredDuo(session, recipient.username)) {
            return true;
        }

        if (!recipient.username.equals(currentTurnUsername)) {
            return false;
        }

        int handSize = recipient.handCards.size();
        if (session.playersWhoCalledDuo.contains(recipient.username)) {
            return false;
        }
        return handSize == 2 || (handSize == 3 && playerHasPlayableCard(session.topPlayPileCard, recipient.handCards));
    }

    /** Checks whether another player can be challenged for missing DUO. */
    private boolean anyOtherPlayerHasUndeclaredDuo(LobbyGameSession session, String usernameToExclude) {
        for (String username : session.playersWithTwoCards) {
            if (!username.equals(usernameToExclude) && !session.playersWhoCalledDuo.contains(username)) {
                return true;
            }
        }
        return false;
    }

    /** Removes and returns one card from the draw pile. */
    private Card drawCardFromPile(LobbyGameSession session) {
        if (session.drawPile.isEmpty()) {
            // The drawPile should be populated with new cards here (use addAll on the drawPile)
        }
        if (session.drawPile.isEmpty()) { // Safety check
            return null;
        }
        return session.drawPile.remove(session.drawPile.size() - 1);
    }

    /** Selects a numbered starting card for the play pile. */
    private Card drawOpeningPlayPileCard(List<Card> drawPile) {
        for (int i = drawPile.size() - 1; i >= 0; i--) {
            Card card = drawPile.get(i);
            if (card.symbol().isNumbered()) {
                drawPile.remove(i);
                return card;
            }
        }
        return pickRandomNumberedCard();
    }

    /** Builds and shuffles a fresh draw pile. */
    private List<Card> createShuffledDrawPile() {
        List<Card> drawPile = new ArrayList<>();
        CardStyle cardStyle = CARD_STYLES[random.nextInt(CARD_STYLES.length)]; // Chooses a card style to use
        for (CardColor color : CARD_COLORS) {
            for (int value = 0; value <= 9; value++) {
                drawPile.add(new Card(color, CardSymbol.numbered(value), cardStyle));
            }
            for (CardSymbol actionSymbol : COLORED_ACTION_SYMBOLS) {
                for (int count = 0; count < COLORED_ACTION_CARD_COUNT; count++) {
                    drawPile.add(new Card(color, actionSymbol, cardStyle));
                }
            }
        }
        for (int count = 0; count < WILD_CARD_COUNT; count++) {
            drawPile.add(new Card(null, CardSymbol.CHANGE_COLOR, null));
            drawPile.add(new Card(null, CardSymbol.CHANGE_COLOR_PLUS_4, null));
        }
        // The deck should be shuffled here

        return drawPile;
    }

    /** Checks whether a hand contains any card playable on the pile. */
    private boolean playerHasPlayableCard(Card topPlayPileCard, List<Card> handCards) {
        for (Card handCard : handCards) {
            if (CardRules.canPlay(topPlayPileCard, handCard)) {
                return true;
            }
        }
        return false;
    }

    /** Creates a fallback random numbered card ID. */
    private Card pickRandomNumberedCard() {
        CardColor color = CARD_COLORS[random.nextInt(CARD_COLORS.length)];
        int value = random.nextInt(10);
        return new Card(color, CardSymbol.numbered(value), CardStyle.FILLED);
    }

    /** Pauses briefly between repeated draw animations. */
    private void sleepDrawDelay() {
        try {
            Thread.sleep(DRAW_STEP_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /** Pauses before a bot announces DUO. */
    private void sleepBotDuoDelay() {
        sleep(BOT_DUO_DELAY_MS);
    }

    /** Sends game end messages and cleans up the finished lobby game. */
    private void finishLobbyGame(LobbyGameSession session, String winnerUsername) {
        sleep(GAME_END_DELAY_MS);

        Network.GameEnd gameEnd = new Network.GameEnd();
        gameEnd.winner = winnerUsername;
        for (LobbyGamePlayer player : session.players) {
            if (player.account != null) {
                player.account.sendPacket(gameEnd);
            }
        }
        disposeLobbyGame(session);
    }

    /** Sleeps for the requested duration while preserving interruption state. */
    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /** Removes all server-side state for a lobby game session. */
    private void disposeLobbyGame(LobbyGameSession session) {
        lobbyGames.remove(session);
        for (LobbyGamePlayer player : session.players) {
            if (player.account != null) {
                lobbyGamesByAccount.remove(player.account);
                player.account.setInGame(false);
            }
        }
        session.players.clear();
        session.drawPile.clear();
        session.playersWithTwoCards.clear();
        session.playersWhoCalledDuo.clear();
    }

    private static final class LobbyGameSession {
        private final List<LobbyGamePlayer> players = new ArrayList<>();
        private final List<Card> drawPile = new ArrayList<>();
        private final Set<String> playersWithTwoCards = new HashSet<>();
        private final Set<String> playersWhoCalledDuo = new HashSet<>();
        private Card topPlayPileCard;
        private int currentTurnIndex;
        private int turnDirection = 1;
        private int pendingDrawCount;
        private boolean currentPlayerCanDraw = true;
        private boolean turnActionsLocked;
    }

    private static final class LobbyGamePlayer {
        private final String username;
        private final boolean bot;
        private final Account account;
        private final List<Card> handCards = new ArrayList<>();

        /** Stores player identity and hand state for a lobby game. */
        private LobbyGamePlayer(String username, boolean bot, Account account) {
            this.username = username;
            this.bot = bot;
            this.account = account;
        }
    }
}
