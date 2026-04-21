package io.github.turn_based_example_game.server;

import com.esotericsoftware.kryonet.Connection;
import io.github.turn_based_example_game.server.Network.JoinGameRequest;
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
    private static final String[] NUMBERED_CARD_COLORS = {"red", "yellow", "green", "blue"};
    private static final String[] CARD_STYLE_VARIANTS = {"filled", "white"};
    private static final String[] ACTION_CARD_VALUES = {"skip", "switch_order", "plus_2"};
    private static final String CHANGE_COLOR = "change_color";
    private static final String CHANGE_COLOR_PLUS_4 = "change_color_plus_4";
    private static final int WILD_CARD_COUNT = 4;
    private static final int COLORED_ACTION_CARD_COUNT = 2;
    private static final int NUMBERED_CARD_VARIANTS = 10;
    private static final int DEFAULT_HAND_SIZE = 7;
    private static final long DRAW_STEP_DELAY_MS = 250L;
    private static final long GAME_END_DELAY_MS = 500L;

    private final Random random = new Random();
    private final Opponent opponent = new Opponent();
    private final Set<Game> games;
    private final List<Account> waitList;
    private final List<Account> teamsWaitList;
    private final Map<Account, LobbyGameSession> lobbyGamesByAccount;
    private final Set<LobbyGameSession> lobbyGames;

    public GameManager() {
        games = new HashSet<>();
        waitList = new ArrayList<>();
        teamsWaitList = new ArrayList<>();
        lobbyGamesByAccount = new HashMap<>();
        lobbyGames = new HashSet<>();
    }

    public void gameEnded(Game game) {
        System.out.println("Game ended: " + game + "winner: " + game.getWinner());
        game.dispose();
        games.remove(game);
    }

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

    public synchronized void startLobbyGame(List<Account> humanAccounts, List<Network.LobbyPlayer> playersInOrder) {
        if (humanAccounts == null || humanAccounts.isEmpty() || playersInOrder == null || playersInOrder.isEmpty()) {
            return;
        }

        LobbyGameSession session = new LobbyGameSession();
        session.drawPile.addAll(createShuffledDrawPile());
        session.topPlayPileCardId = drawOpeningPlayPileCard(session.drawPile);

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
        lobbyGames.add(session);
        broadcastLobbyGameState(session);
        resolveCurrentTurn(session);
    }

    public synchronized void handlePackets(Connection connection, Object object) {
        Account account = Database.findAccount(connection);
        if (account == null) {
            return;
        }

        if (object instanceof Network.GameTurnActionRequest request) {
            handleTurnAction(account, request);
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

    private void handleHumanPlay(LobbyGameSession session, LobbyGamePlayer currentPlayer, Network.GameTurnActionRequest request) {
        if (request.handIndex < 0 || request.handIndex >= currentPlayer.handCardIds.size()) {
            return;
        }

        String handCardId = currentPlayer.handCardIds.get(request.handIndex);
        if (!canPlayCard(session.topPlayPileCardId, handCardId)) {
            return;
        }

        String resolvedPlayPileCardId = resolvePlayedPileCard(handCardId, request.chosenColor);
        if (resolvedPlayPileCardId == null) {
            return;
        }

        currentPlayer.handCardIds.remove(request.handIndex);
        session.topPlayPileCardId = resolvedPlayPileCardId;
        if (currentPlayer.handCardIds.isEmpty()) {
            broadcastLobbyGameState(session);
            finishLobbyGame(session, currentPlayer.username);
            return;
        }
        applyTurnDirectionChange(session, resolvedPlayPileCardId);
        session.pendingDrawCount = extractDrawPenalty(resolvedPlayPileCardId);
        advanceToNextTurn(session, extractTurnAdvanceCount(resolvedPlayPileCardId));
        broadcastLobbyGameState(session);
        resolveCurrentTurn(session);
    }

    private void handleHumanDraw(LobbyGameSession session, LobbyGamePlayer currentPlayer) {
        if (!session.currentPlayerCanDraw) {
            return;
        }

        if (playerHasPlayableCard(session.topPlayPileCardId, currentPlayer.handCardIds)) {
            drawCardToPlayer(session, currentPlayer);
            advanceToNextTurn(session);
            broadcastLobbyGameState(session);
            resolveCurrentTurn(session);
            return;
        }

        session.currentPlayerCanDraw = false;
        session.turnActionsLocked = true;
        while (true) {
            String drawnCardId = drawCardToPlayer(session, currentPlayer);
            boolean playable = drawnCardId != null && canPlayCard(session.topPlayPileCardId, drawnCardId);
            session.turnActionsLocked = !playable;
            broadcastLobbyGameState(session);
            if (playable) {
                session.turnActionsLocked = false;
                break;
            }
            sleepDrawDelay();
        }
    }

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
    }

    private void resolveBotTurns(LobbyGameSession session) {
        int safetyCounter = 0;
        while (!session.players.isEmpty() && session.players.get(session.currentTurnIndex).bot && safetyCounter++ < 100) {
            LobbyGamePlayer bot = session.players.get(session.currentTurnIndex);
            Opponent.Decision decision = opponent.chooseAction(session.topPlayPileCardId, bot.handCardIds);
            if (decision.action == Opponent.Action.PLAY) {
                if (decision.handIndex < 0 || decision.handIndex >= bot.handCardIds.size()) {
                    break;
                }
                bot.handCardIds.remove(decision.handIndex);
                session.topPlayPileCardId = decision.playPileCardId;
                if (bot.handCardIds.isEmpty()) {
                    broadcastLobbyGameState(session);
                    finishLobbyGame(session, bot.username);
                    return;
                }
                applyTurnDirectionChange(session, decision.playPileCardId);
                session.pendingDrawCount = extractDrawPenalty(decision.playPileCardId);
                advanceToNextTurn(session, extractTurnAdvanceCount(decision.playPileCardId));
                broadcastLobbyGameState(session);
                resolveCurrentTurn(session);
                return;
            }

            session.currentPlayerCanDraw = false;
            session.turnActionsLocked = true;
            while (true) {
                String drawnCardId = drawCardToPlayer(session, bot);
                boolean playable = drawnCardId != null && canPlayCard(session.topPlayPileCardId, drawnCardId);
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

    private void broadcastLobbyGameState(LobbyGameSession session) {
        String currentTurnUsername = session.players.get(session.currentTurnIndex).username;
        for (int i = 0; i < session.players.size(); i++) {
            LobbyGamePlayer recipient = session.players.get(i);
            if (recipient.account == null) {
                continue;
            }

            Network.GameStateUpdate update = new Network.GameStateUpdate();
            update.playerIndex = i;
            update.topPlayPileCardId = session.topPlayPileCardId;
            update.currentTurnUsername = currentTurnUsername;
            update.currentPlayerCanDraw = session.currentPlayerCanDraw;
            update.turnActionsLocked = session.turnActionsLocked;
            update.playerHandCardIds.addAll(recipient.handCardIds);
            for (LobbyGamePlayer player : session.players) {
                update.playerUsernames.add(player.username);
                update.playerHandCounts.add(player.handCardIds.size());
            }
            recipient.account.sendPacket(update);
        }
    }

    private void advanceToNextTurn(LobbyGameSession session) {
        advanceToNextTurn(session, 1);
    }

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

    private void dealStartingHand(LobbyGameSession session, LobbyGamePlayer player) {
        for (int i = 0; i < DEFAULT_HAND_SIZE; i++) {
            drawCardToPlayer(session, player);
        }
    }

    private String drawCardToPlayer(LobbyGameSession session, LobbyGamePlayer player) {
        String drawnCardId = drawCardFromPile(session);
        if (drawnCardId != null) {
            player.handCardIds.add(drawnCardId);
        }
        return drawnCardId;
    }

    private String drawCardFromPile(LobbyGameSession session) {
        if (session.drawPile.isEmpty()) {
            session.drawPile.addAll(createShuffledDrawPile());
        }
        if (session.drawPile.isEmpty()) {
            return null;
        }
        return session.drawPile.remove(session.drawPile.size() - 1);
    }

    private String drawOpeningPlayPileCard(List<String> drawPile) {
        for (int i = drawPile.size() - 1; i >= 0; i--) {
            String cardId = drawPile.get(i);
            if (extractCardNumber(cardId) != null) {
                drawPile.remove(i);
                return cardId;
            }
        }
        return pickRandomNumberedCardId();
    }

    private List<String> createShuffledDrawPile() {
        List<String> drawPile = new ArrayList<>();
        String cardStyle = CARD_STYLE_VARIANTS[random.nextInt(CARD_STYLE_VARIANTS.length)];
        for (String color : NUMBERED_CARD_COLORS) {
            for (int value = 0; value < NUMBERED_CARD_VARIANTS; value++) {
                drawPile.add(color + "_" + value + "_" + cardStyle);
            }
            for (String actionValue : ACTION_CARD_VALUES) {
                for (int count = 0; count < COLORED_ACTION_CARD_COUNT; count++) {
                    drawPile.add(color + "_" + actionValue + "_" + cardStyle);
                }
            }
        }
        for (int count = 0; count < WILD_CARD_COUNT; count++) {
            drawPile.add(CHANGE_COLOR);
            drawPile.add(CHANGE_COLOR_PLUS_4);
        }
        Collections.shuffle(drawPile, random);
        return drawPile;
    }

    private boolean playerHasPlayableCard(String topPlayPileCardId, List<String> handCardIds) {
        for (String handCardId : handCardIds) {
            if (canPlayCard(topPlayPileCardId, handCardId)) {
                return true;
            }
        }
        return false;
    }

    private boolean canPlayCard(String topPlayPileCardId, String handCardId) {
        if (isAlwaysPlayableCard(handCardId)) {
            return true;
        }

        String topSymbol = extractCardSymbol(topPlayPileCardId);
        String playedSymbol = extractCardSymbol(handCardId);
        if (topSymbol != null && topSymbol.equals(playedSymbol)) {
            return true;
        }

        String topColor = extractCardColorId(topPlayPileCardId);
        String playedColor = extractCardColorId(handCardId);
        if (topColor != null && topColor.equals(playedColor)) {
            return true;
        }

        Integer topNumber = extractCardNumber(topPlayPileCardId);
        Integer playedNumber = extractCardNumber(handCardId);
        return topNumber != null && topNumber.equals(playedNumber);
    }

    private String resolvePlayedPileCard(String handCardId, String chosenColor) {
        if (CHANGE_COLOR.equals(handCardId) || CHANGE_COLOR_PLUS_4.equals(handCardId)) {
            if (!isValidColor(chosenColor)) {
                return null;
            }
            return chosenColor + "_" + handCardId;
        }
        return handCardId;
    }

    private boolean isValidColor(String colorId) {
        if (colorId == null) {
            return false;
        }
        for (String color : NUMBERED_CARD_COLORS) {
            if (color.equals(colorId)) {
                return true;
            }
        }
        return false;
    }

    private static String extractCardColorId(String cardId) {
        if (cardId == null) {
            return null;
        }
        for (String color : NUMBERED_CARD_COLORS) {
            if (cardId.startsWith(color + "_")) {
                return color;
            }
        }
        return null;
    }

    private static Integer extractCardNumber(String cardId) {
        if (cardId == null) {
            return null;
        }
        String[] parts = cardId.split("_");
        if (parts.length < 2) {
            return null;
        }

        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String extractCardSymbol(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            return null;
        }
        if (CHANGE_COLOR.equals(cardId) || CHANGE_COLOR_PLUS_4.equals(cardId)) {
            return cardId;
        }

        String[] parts = cardId.split("_");
        if (parts.length < 2) {
            return cardId;
        }

        int startIndex = extractCardColorId(cardId) == null ? 0 : 1;
        int endIndex = parts.length;
        if (endIndex > startIndex && ("filled".equals(parts[endIndex - 1]) || "white".equals(parts[endIndex - 1]))) {
            endIndex--;
        }
        if (startIndex >= endIndex) {
            return cardId;
        }

        StringBuilder symbol = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                symbol.append('_');
            }
            symbol.append(parts[i]);
        }
        return symbol.toString();
    }

    private static boolean isAlwaysPlayableCard(String cardId) {
        return CHANGE_COLOR.equals(cardId) || CHANGE_COLOR_PLUS_4.equals(cardId);
    }

    private int extractDrawPenalty(String cardId) {
        if (cardId == null) {
            return 0;
        }
        if (cardId.endsWith(CHANGE_COLOR_PLUS_4)) {
            return 4;
        }
        if (cardId.contains("_plus_2_")) {
            return 2;
        }
        return 0;
    }

    private int extractTurnAdvanceCount(String cardId) {
        if (cardId != null && cardId.contains("_skip_")) {
            return 2;
        }
        return 1;
    }

    private void applyTurnDirectionChange(LobbyGameSession session, String cardId) {
        if (cardId != null && cardId.contains("_switch_order_")) {
            session.turnDirection *= -1;
        }
    }

    private String pickRandomNumberedCardId() {
        String color = NUMBERED_CARD_COLORS[random.nextInt(NUMBERED_CARD_COLORS.length)];
        int value = random.nextInt(NUMBERED_CARD_VARIANTS);
        return color + "_" + value + "_filled";
    }

    private void sleepDrawDelay() {
        try {
            Thread.sleep(DRAW_STEP_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

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

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

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
    }

    private static final class LobbyGameSession {
        private final List<LobbyGamePlayer> players = new ArrayList<>();
        private final List<String> drawPile = new ArrayList<>();
        private String topPlayPileCardId;
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
        private final List<String> handCardIds = new ArrayList<>();

        private LobbyGamePlayer(String username, boolean bot, Account account) {
            this.username = username;
            this.bot = bot;
            this.account = account;
        }
    }
}
