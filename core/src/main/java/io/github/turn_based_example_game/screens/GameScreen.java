package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.math.Vector2;
import io.github.turn_based_example_game.Account;
import io.github.turn_based_example_game.Card;
import io.github.turn_based_example_game.CardColor;
import io.github.turn_based_example_game.CardRules;
import io.github.turn_based_example_game.CardStyle;
import io.github.turn_based_example_game.CardSymbol;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.NetworkManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class GameScreen extends Stage {
    private static final String CARD_ASSET_ROOT = "UnoCards/sprites/";
    private static final float CARD_WIDTH = 92f;
    private static final float CARD_HEIGHT = 138f;
    private static final float OPPONENT_CARD_SCALE = 0.62f;
    private static final float DRAW_PILE_SCALE = 0.72f;
    private static final float DUO_OVERLAY_DURATION_SECONDS = 5f;
    private static final int DISPLAY_PLAYER_COUNT = 4;
    private static final int DEFAULT_HAND_SIZE = 7;
    private static final float CARD_HOVER_SCALE = 1.08f;
    private static final float CARD_HOVER_DURATION = 0.08f;
    private static final Card[] DEFAULT_PLAYER_HAND = {
        new Card(CardColor.RED, CardSymbol.NUM_7, CardStyle.FILLED),
        new Card(CardColor.YELLOW, CardSymbol.NUM_2, CardStyle.WHITE),
        new Card(CardColor.BLUE, CardSymbol.SKIP, CardStyle.FILLED),
        new Card(CardColor.GREEN, CardSymbol.SWITCH_ORDER, CardStyle.WHITE),
        new Card(null, CardSymbol.CHANGE_COLOR, null),
        new Card(CardColor.RED, CardSymbol.PLUS_2, CardStyle.FILLED),
        new Card(CardColor.BLUE, CardSymbol.NUM_9, CardStyle.WHITE)
    };

    private final Array<Texture> disposableTextures = new Array<>();
    private final Main game;
    private final Skin skin;
    private final List<Network.LobbyPlayer> playOrder = new ArrayList<>();
    private final List<Card> playerHandCards = new ArrayList<>();
    private final PlayPile playPile;
    private final Map<String, Integer> handCountsByUsername = new HashMap<>();
    private final Map<String, Table> hiddenHandTablesByUsername = new HashMap<>();
    private final Map<String, Label> playerLabelsByUsername = new HashMap<>();
    private final Map<String, String> playerLabelTextByUsername = new HashMap<>();
    private final Consumer<Network.GameStateUpdate> gameStateListener;
    private final Consumer<Network.GameEnd> gameEndListener;
    private final Consumer<Network.GameDuoEvent> gameDuoListener;
    private final Vector2 temporaryStagePosition = new Vector2();

    private Table playerHandTable;
    private Table wildColorPicker;
    private TextButton duoButton;
    private Image duoOverlayImage;
    private StaticCardActor playPileCardActor;
    private boolean serverShowDuoButton;
    private boolean currentPlayerCanDraw = true;
    private boolean turnActionsLocked;
    private boolean pendingTurnSubmission;
    private boolean pendingWildColorChoice;
    private boolean pendingDuoAnnouncement;
    private boolean pendingDuoAfterPlay;
    private boolean duoOverlayActive;
    private String currentTurnUsername;
    private int pendingWildHandIndex = -1;

    // === Construction ===

    /** Creates a standalone game screen with fallback state. */
    public GameScreen() {
        this(null, null, null);
    }

    /** Creates a game screen attached to the main game instance. */
    public GameScreen(Main game) {
        this(game, null, null);
    }

    /** Creates a game screen from a lobby snapshot. */
    public GameScreen(Main game, Network.LobbyState lobbyState) {
        this(game, lobbyState, null);
    }

    /** Creates a synchronized game screen from lobby and game state. */
    public GameScreen(Main game, Network.LobbyState lobbyState, Network.GameStateUpdate initialGameState) {
        super(new ScreenViewport());
        this.game = game;
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        playOrder.addAll(buildPlayOrder(lobbyState != null ? lobbyState : NetworkManager.getCurrentLobby()));
        initializePlayerHand(initialGameState);
        playPile = new PlayPile(resolveInitialTopPlayPileCard(initialGameState));
        initializeDisplayedHandCounts(initialGameState);
        currentTurnUsername = resolveInitialTurnUsername(initialGameState);
        if (initialGameState != null) {
            currentPlayerCanDraw = initialGameState.currentPlayerCanDraw;
            turnActionsLocked = initialGameState.turnActionsLocked;
            serverShowDuoButton = initialGameState.showDuoButton;
        }
        buildBoard();
        refreshSynchronizedUi();
        gameStateListener = this::applyGameStateUpdate;
        gameEndListener = this::handleGameEnd;
        gameDuoListener = this::handleGameDuo;
        NetworkManager.setGameStartListener(gameStateListener);
        NetworkManager.setGameEndListener(gameEndListener);
        NetworkManager.setGameDuoListener(gameDuoListener);
        Gdx.input.setInputProcessor(this);
    }

    // === Layout ===

    /** Builds the full game board UI and overlay actors. */
    private void buildBoard() {
        Texture backgroundTexture = Gdx.files.internal("menuBackground.png").exists()
            ? trackTexture(new Texture(Gdx.files.internal("menuBackground.png")))
            : createBoardBackgroundTexture();

        Image background = new Image(backgroundTexture);
        background.setFillParent(true);
        addActor(background);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        root.defaults().pad(12f);
        addActor(root);

        root.add(createTopRow()).growX().top().row();
        root.add(createMiddleRow()).expand().fill().row();
        root.add(createBottomRow()).growX().bottom();

        wildColorPicker = createWildColorPicker();
        addActor(wildColorPicker);

        duoButton = createDuoButton();
        addActor(duoButton);
        duoOverlayImage = createDuoOverlayImage();
        addActor(duoOverlayImage);
        duoOverlayImage.toFront();
    }

    /** Creates the top board row with opponent and draw pile. */
    private Table createTopRow() {
        float drawZoneWidth = (CARD_WIDTH * DRAW_PILE_SCALE) + 64f;
        Table row = new Table();
        row.add().width(drawZoneWidth);
        row.add(createOpponentArea(getPlayerAtSeat(2))).expandX().center().top();
        row.add(createDrawPileArea()).width(drawZoneWidth).right().top();
        return row;
    }

    /** Creates the middle board row with side opponents and play pile. */
    private Table createMiddleRow() {
        Table row = new Table();
        row.defaults().pad(12f);
        row.add(createSideOpponentArea(getPlayerAtSeat(3))).width(220f).expandY().left();
        row.add(createCenterArea()).expand().center();
        row.add(createSideOpponentArea(getPlayerAtSeat(1))).width(220f).expandY().right();
        return row;
    }

    /** Creates the bottom board row for the local player's hand. */
    private Table createBottomRow() {
        Table row = new Table();
        row.add(createPlayerArea(getPlayerAtSeat(0))).center().bottom();
        return row;
    }

    /** Creates the top opponent area for a player. */
    private Table createOpponentArea(Network.LobbyPlayer player) {
        Table section = new Table();
        section.defaults().pad(4f);

        if (player == null) {
            return section;
        }

        Label title = createPlayerTitleLabel(player.username, getPlayerLabel(player));
        section.add(title).padBottom(10f).row();

        Table handTable = createHiddenHand(getDisplayedHandCount(player.username));
        hiddenHandTablesByUsername.put(player.username, handTable);
        section.add(handTable).center();
        return section;
    }

    /** Creates a side opponent area for a player. */
    private Table createSideOpponentArea(Network.LobbyPlayer player) {
        Table section = new Table();
        section.defaults().pad(4f);

        if (player == null) {
            return section;
        }

        Label title = createPlayerTitleLabel(player.username, getPlayerLabel(player));
        title.setWrap(true);
        section.add(title).width(200f).center().padBottom(10f).row();
        Table handTable = createHiddenHand(getDisplayedHandCount(player.username));
        hiddenHandTablesByUsername.put(player.username, handTable);
        section.add(handTable).center();
        return section;
    }

    /** Creates the center area containing the play pile. */
    private Table createCenterArea() {
        Table section = new Table();
        section.defaults().pad(10f);
        playPileCardActor = new StaticCardActor(playPile.getTopCard(), false, CARD_WIDTH, CARD_HEIGHT);
        section.add(createPile("Play Pile", playPileCardActor, CARD_WIDTH, CARD_HEIGHT))
            .size(CARD_WIDTH + 14f, CARD_HEIGHT + 38f);

        return section;
    }

    /** Creates the hidden color picker used by wild cards. */
    private Table createWildColorPicker() {
        Table picker = new Table(skin);
        picker.defaults().width(88f).height(40f).pad(4f);
        picker.setVisible(false);
        picker.setTouchable(Touchable.disabled);

        picker.add(createWildColorButton("Red", CardColor.RED));
        picker.add(createWildColorButton("Green", CardColor.GREEN));
        picker.row();
        picker.add(createWildColorButton("Blue", CardColor.BLUE));
        picker.add(createWildColorButton("Yellow", CardColor.YELLOW));
        picker.pack();
        return picker;
    }

    /** Creates one color choice button for the wild card picker. */
    private TextButton createWildColorButton(String text, CardColor color) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ClickListener() {
            /** Selects this button's wild card color. */
            @Override
            public void clicked(InputEvent event, float x, float y) {
                chooseWildColor(color);
            }
        });
        return button;
    }

    /** Creates the DUO call button. */
    private TextButton createDuoButton() {
        TextButton button = new TextButton("DUO", skin);
        button.getLabel().setFontScale(1.35f);
        button.setSize(150f, 72f);
        button.setPosition(24f, 24f);
        button.setVisible(false);
        button.setTouchable(Touchable.disabled);
        button.addListener(new ClickListener() {
            /** Sends or queues a DUO announcement. */
            @Override
            public void clicked(InputEvent event, float x, float y) {
                announceDuo();
            }
        });
        return button;
    }

    /** Creates the temporary DUO announcement overlay. */
    private Image createDuoOverlayImage() {
        Texture texture = trackTexture(new Texture(Gdx.files.internal(CARD_ASSET_ROOT + "duo.png")));
        Image image = new Image(texture);
        image.setSize(170f, 170f);
        image.setVisible(false);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    /** Creates the draw pile section. */
    private Table createDrawPileArea() {
        float drawWidth = CARD_WIDTH * DRAW_PILE_SCALE;
        float drawHeight = CARD_HEIGHT * DRAW_PILE_SCALE;
        return createPile("Draw Pile", new DrawPileCardActor(drawWidth, drawHeight), drawWidth, drawHeight);
    }

    /** Creates a labelled card pile UI block. */
    private Table createPile(String labelText, Actor cardActor, float cardWidth, float cardHeight) {
        Table pile = new Table();
        pile.defaults().pad(4f);

        Image glow = new Image(createPileGlowTexture());
        glow.setColor(1f, 1f, 1f, 0.18f);
        pile.setBackground(glow.getDrawable());

        Label label = new Label(labelText, skin);
        label.setColor(Color.WHITE);

        pile.add(cardActor).size(cardWidth, cardHeight).row();
        pile.add(label).padTop(4f);
        return pile;
    }

    /** Creates the local player's hand area. */
    private Table createPlayerArea(Network.LobbyPlayer player) {
        Table section = new Table();
        section.defaults().pad(4f);

        String labelText = player == null ? "Your Hand" : getPlayerLabel(player);
        String username = player == null ? Account.getUsername() : player.username;
        Label title = createPlayerTitleLabel(username, labelText);
        section.add(title).padBottom(12f).row();

        playerHandTable = new Table();
        playerHandTable.defaults().padLeft(-30f);
        refreshPlayerHand();

        section.add(playerHandTable).bottom();
        return section;
    }

    /** Creates a hidden hand display with card backs. */
    private Table createHiddenHand(int cardCount) {
        Table cards = new Table();
        cards.defaults().padLeft(-30f);
        populateHiddenHand(cards, cardCount);
        return cards;
    }

    /** Rebuilds a hidden hand table with the requested card count. */
    private void populateHiddenHand(Table cards, int cardCount) {
        cards.clearChildren();
        float width = CARD_WIDTH * OPPONENT_CARD_SCALE;
        float height = CARD_HEIGHT * OPPONENT_CARD_SCALE;
        for (int i = 0; i < cardCount; i++) {
            Actor card = createCardActor(null, true, false, width, height);
            cards.add(card).size(width, height);
        }
        cards.invalidateHierarchy();
    }

    // === Initial state ===

    /** Builds player seating order with the local player at the bottom. */
    private List<Network.LobbyPlayer> buildPlayOrder(Network.LobbyState lobbyState) {
        List<Network.LobbyPlayer> orderedPlayers = new ArrayList<>();
        if (lobbyState != null) {
            int limit = lobbyState.settings == null ? DISPLAY_PLAYER_COUNT : Math.min(DISPLAY_PLAYER_COUNT, Math.max(1, lobbyState.settings.maxPlayers));
            for (Network.LobbyPlayer player : lobbyState.players) {
                if (orderedPlayers.size() >= limit) {
                    break;
                }
                orderedPlayers.add(copyPlayer(player));
            }
        }

        if (orderedPlayers.isEmpty()) {
            orderedPlayers.add(createLocalFallbackPlayer());
            return orderedPlayers;
        }

        String currentUsername = Account.getUsername();
        int localIndex = -1;
        for (int i = 0; i < orderedPlayers.size(); i++) {
            if (currentUsername != null && currentUsername.equals(orderedPlayers.get(i).username)) {
                localIndex = i;
                break;
            }
        }

        if (localIndex <= 0) {
            return orderedPlayers;
        }

        List<Network.LobbyPlayer> rotatedPlayers = new ArrayList<>(orderedPlayers.size());
        for (int i = 0; i < orderedPlayers.size(); i++) {
            rotatedPlayers.add(copyPlayer(orderedPlayers.get((localIndex + i) % orderedPlayers.size())));
        }
        return rotatedPlayers;
    }

    /** Returns the player assigned to a visual seat. */
    private Network.LobbyPlayer getPlayerAtSeat(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= playOrder.size()) {
            return null;
        }
        return playOrder.get(seatIndex);
    }

    /** Creates a fallback local player when no lobby state exists. */
    private Network.LobbyPlayer createLocalFallbackPlayer() {
        Network.LobbyPlayer player = new Network.LobbyPlayer();
        player.username = Account.getUsername() == null ? "You" : Account.getUsername();
        player.bot = false;
        return player;
    }

    /** Copies a lobby player for local screen state. */
    private Network.LobbyPlayer copyPlayer(Network.LobbyPlayer source) {
        Network.LobbyPlayer copy = new Network.LobbyPlayer();
        copy.username = source.username;
        copy.ready = source.ready;
        copy.owner = source.owner;
        copy.bot = source.bot;
        return copy;
    }

    /** Builds the label text shown for a player. */
    private String getPlayerLabel(Network.LobbyPlayer player) {
        String currentUsername = Account.getUsername();
        if (!player.bot && currentUsername != null && currentUsername.equals(player.username)) {
            return "You";
        }
        return player.bot ? player.username + " [Bot]" : player.username;
    }

    /** Creates and tracks a player title label. */
    private Label createPlayerTitleLabel(String username, String baseText) {
        Label title = new Label(baseText, skin);
        title.setColor(Color.WHITE);
        if (username != null) {
            playerLabelsByUsername.put(username, title);
            playerLabelTextByUsername.put(username, baseText);
        }
        return title;
    }

    /** Initializes the local hand from server or fallback data. */
    private void initializePlayerHand(Network.GameStateUpdate initialGameState) {
        playerHandCards.clear();
        if (initialGameState != null && initialGameState.playerHandCards != null && !initialGameState.playerHandCards.isEmpty()) {
            playerHandCards.addAll(initialGameState.playerHandCards);
            return;
        }

        for (Card card : DEFAULT_PLAYER_HAND) {
            playerHandCards.add(card);
        }
    }

    /** Resolves the starting play pile card for the screen. */
    private Card resolveInitialTopPlayPileCard(Network.GameStateUpdate initialGameState) {
        if (initialGameState != null && initialGameState.topPlayPileCard != null) {
            return initialGameState.topPlayPileCard;
        }
        return pickRandomNumberedCard();
    }

    /** Resolves whose turn should be displayed initially. */
    private String resolveInitialTurnUsername(Network.GameStateUpdate initialGameState) {
        if (initialGameState != null && initialGameState.currentTurnUsername != null && !initialGameState.currentTurnUsername.isBlank()) {
            return initialGameState.currentTurnUsername;
        }
        return playOrder.isEmpty() ? Account.getUsername() : playOrder.get(0).username;
    }

    /** Initializes displayed hand counts for all visible players. */
    private void initializeDisplayedHandCounts(Network.GameStateUpdate initialGameState) {
        handCountsByUsername.clear();
        for (Network.LobbyPlayer player : playOrder) {
            handCountsByUsername.put(player.username, DEFAULT_HAND_SIZE);
        }
        String currentUsername = Account.getUsername();
        if (currentUsername != null) {
            handCountsByUsername.put(currentUsername, playerHandCards.size());
        }
        if (initialGameState != null) {
            updateDisplayedHandCounts(initialGameState);
        }
    }

    /** Updates visible hand counts from a server state update. */
    private void updateDisplayedHandCounts(Network.GameStateUpdate update) {
        if (update.playerUsernames == null || update.playerHandCounts == null || update.playerUsernames.size() != update.playerHandCounts.size()) {
            return;
        }

        handCountsByUsername.clear();
        for (int i = 0; i < update.playerUsernames.size(); i++) {
            handCountsByUsername.put(update.playerUsernames.get(i), update.playerHandCounts.get(i));
        }
    }

    /** Creates a fallback random numbered card ID. */
    private Card pickRandomNumberedCard() {
        CardColor[] colors = CardColor.values();
        CardColor color = colors[ThreadLocalRandom.current().nextInt(colors.length)];
        int value = ThreadLocalRandom.current().nextInt(10);
        return new Card(color, CardSymbol.numbered(value), CardStyle.FILLED);
    }

    // === Network ===

    /** Applies a server game state update to the local UI. */
    private void applyGameStateUpdate(Network.GameStateUpdate update) {
        if (update == null) {
            return;
        }

        if (update.topPlayPileCard != null) {
            playPile.placeCard(update.topPlayPileCard);
        }
        if (update.currentTurnUsername != null && !update.currentTurnUsername.isBlank()) {
            currentTurnUsername = update.currentTurnUsername;
        }
        currentPlayerCanDraw = update.currentPlayerCanDraw;
        turnActionsLocked = update.turnActionsLocked;
        serverShowDuoButton = update.showDuoButton;
        playerHandCards.clear();
        if (update.playerHandCards != null) {
            playerHandCards.addAll(update.playerHandCards);
        }
        pendingDuoAfterPlay = false;
        updateDisplayedHandCounts(update);
        pendingTurnSubmission = false;
        pendingWildColorChoice = false;
        pendingWildHandIndex = -1;
        hideWildColorPicker();
        refreshPlayerHand();
        refreshSynchronizedUi();
    }

    /** Switches to the game end screen when the server ends the game. */
    private void handleGameEnd(Network.GameEnd end) {
        if (game == null) {
            return;
        }
        game.switchScreen(new GameEndScreen(game, end));
    }

    // === Synchronized UI ===

    /** Refreshes all UI elements that depend on synchronized game state. */
    private void refreshSynchronizedUi() {
        if (playPileCardActor != null) {
            playPileCardActor.setCard(playPile.getTopCard(), false);
        }
        refreshHiddenHandTables();
        refreshPlayerLabels();
        updateDuoButtonState();
        updateWildColorPickerPosition();
    }

    /** Rebuilds hidden opponent hand displays. */
    private void refreshHiddenHandTables() {
        for (Map.Entry<String, Table> entry : hiddenHandTablesByUsername.entrySet()) {
            populateHiddenHand(entry.getValue(), getDisplayedHandCount(entry.getKey()));
        }
    }

    /** Updates player labels to highlight whose turn it is. */
    private void refreshPlayerLabels() {
        for (Map.Entry<String, Label> entry : playerLabelsByUsername.entrySet()) {
            String username = entry.getKey();
            Label label = entry.getValue();
            String baseText = playerLabelTextByUsername.getOrDefault(username, username);
            boolean currentTurn = username != null && username.equals(currentTurnUsername);
            label.setText(currentTurn ? baseText + " <- Turn" : baseText);
            label.setColor(currentTurn ? new Color(1f, 0.95f, 0.7f, 1f) : Color.WHITE);
        }
    }

    /** Returns the displayed hand count for a username. */
    private int getDisplayedHandCount(String username) {
        if (username != null && username.equals(Account.getUsername())) {
            return playerHandCards.size();
        }
        Integer count = handCountsByUsername.get(username);
        return count == null ? DEFAULT_HAND_SIZE : count;
    }

    // === DUO ===

    /** Shows, hides, or disables the DUO button based on state. */
    private void updateDuoButtonState() {
        if (duoButton == null) {
            return;
        }

        if (duoOverlayActive || pendingDuoAnnouncement || pendingDuoAfterPlay) {
            duoButton.setVisible(false);
            duoButton.setTouchable(Touchable.disabled);
            return;
        }

        boolean shouldShow = serverShowDuoButton;
        duoButton.setVisible(shouldShow);
        duoButton.setTouchable(shouldShow ? Touchable.enabled : Touchable.disabled);
        if (shouldShow) {
            duoButton.toFront();
        }
    }

    /** Starts a DUO announcement if the player is allowed to call it. */
    private void announceDuo() {
        if (pendingDuoAnnouncement || pendingDuoAfterPlay || duoOverlayActive) {
            return;
        }

        if (shouldDelayDuoAnnouncement()) {
            pendingDuoAfterPlay = true;
            updateDuoButtonState();
            return;
        }

        sendDuoAnnouncement();
    }

    /** Checks whether DUO should be delayed until after a card play. */
    private boolean shouldDelayDuoAnnouncement() {
        return isLocalPlayersTurn() && playerHandCards.size() == 3 && playerHasPlayableCard();
    }

    /** Sends a DUO request to the server. */
    private void sendDuoAnnouncement() {
        pendingDuoAfterPlay = false;
        pendingDuoAnnouncement = true;
        updateDuoButtonState();
        NetworkManager.sendTCP(new Network.GameDuoRequest());
    }

    /** Displays the temporary DUO overlay animation. */
    private void showDuoOverlay() {
        if (duoOverlayImage == null || duoOverlayActive) {
            return;
        }

        pendingDuoAnnouncement = false;
        duoOverlayActive = true;
        updateDuoButtonState();
        updateDuoOverlayPosition();
        duoOverlayImage.clearActions();
        duoOverlayImage.setVisible(true);
        duoOverlayImage.toFront();
        duoOverlayImage.addAction(Actions.sequence(
            Actions.delay(DUO_OVERLAY_DURATION_SECONDS),
            Actions.run(() -> {
                duoOverlayImage.setVisible(false);
                duoOverlayActive = false;
                updateDuoButtonState();
            })
        ));
    }

    /** Handles a DUO event received from the server. */
    private void handleGameDuo(Network.GameDuoEvent duoEvent) {
        if (duoEvent == null) {
            pendingDuoAnnouncement = false;
            updateDuoButtonState();
            return;
        }
        showDuoOverlay();
    }

    // === Hand rendering and input ===

    /** Rebuilds the local player's visible hand cards. */
    private void refreshPlayerHand() {
        if (playerHandTable == null) {
            return;
        }

        playerHandTable.clearChildren();
        for (int i = 0; i < playerHandCards.size(); i++) {
            playerHandTable.add(new PlayerHandCardActor(i, playerHandCards.get(i), CARD_WIDTH, CARD_HEIGHT)).size(CARD_WIDTH, CARD_HEIGHT);
        }
        playerHandTable.invalidateHierarchy();
    }

    /** Checks whether the local hand contains a playable card. */
    private boolean playerHasPlayableCard() {
        for (Card card : playerHandCards) {
            if (playPile.canAcceptCard(card)) {
                return true;
            }
        }
        return false;
    }

    /** Checks whether it is currently the local player's turn. */
    private boolean isLocalPlayersTurn() {
        String currentUsername = Account.getUsername();
        return currentUsername != null && currentUsername.equals(currentTurnUsername);
    }

    /** Checks whether the local player can currently submit a play. */
    private boolean canLocalPlayerPlay() {
        return isLocalPlayersTurn() && !turnActionsLocked && !pendingTurnSubmission && !pendingWildColorChoice;
    }

    /** Checks whether the local player can currently draw. */
    private boolean canLocalPlayerDraw() {
        return canLocalPlayerPlay() && currentPlayerCanDraw;
    }

    /** Sends a draw-card turn action to the server. */
    private void drawCardsForPlayer() {
        if (!canLocalPlayerDraw()) {
            return;
        }

        pendingTurnSubmission = true;
        Network.GameTurnActionRequest request = new Network.GameTurnActionRequest();
        request.actionType = Network.GameTurnActionRequest.ActionType.DRAW_CARD;
        NetworkManager.sendTCP(request);
    }

    /** Attempts to play a card from the local hand. */
    private void playCardFromHand(int handIndex) {
        if (!canLocalPlayerPlay()) {
            return;
        }

        if (handIndex < 0 || handIndex >= playerHandCards.size()) {
            return;
        }

        Card playedCard = playerHandCards.get(handIndex);
        if (!playPile.canAcceptCard(playedCard)) {
            return;
        }

        if (requiresWildColorChoice(playedCard)) {
            promptForWildColor(handIndex);
            return;
        }
        submitPlayCard(handIndex, null);
    }

    /** Checks whether a card requires a chosen color before play. */
    private boolean requiresWildColorChoice(Card card) {
        return card != null && card.isWild() && !card.isResolvedWild();
    }

    /** Shows the wild color picker for a pending card play. */
    private void promptForWildColor(int handIndex) {
        pendingWildColorChoice = true;
        pendingWildHandIndex = handIndex;
        if (wildColorPicker != null) {
            wildColorPicker.setVisible(true);
            wildColorPicker.setTouchable(Touchable.enabled);
            wildColorPicker.toFront();
            updateWildColorPickerPosition();
        }
    }

    /** Completes a pending wild card play with a selected color. */
    private void chooseWildColor(CardColor color) {
        if (!pendingWildColorChoice || pendingWildHandIndex < 0 || color == null) {
            return;
        }

        pendingWildColorChoice = false;
        hideWildColorPicker();
        submitPlayCard(pendingWildHandIndex, color);
        pendingWildHandIndex = -1;
    }

    /** Hides and disables the wild color picker. */
    private void hideWildColorPicker() {
        if (wildColorPicker != null) {
            wildColorPicker.setVisible(false);
            wildColorPicker.setTouchable(Touchable.disabled);
        }
    }

    /** Positions the wild color picker near the play pile. */
    private void updateWildColorPickerPosition() {
        if (wildColorPicker == null || playPileCardActor == null || !wildColorPicker.isVisible()) {
            return;
        }

        temporaryStagePosition.set(playPileCardActor.getWidth() / 2f, playPileCardActor.getHeight());
        playPileCardActor.localToStageCoordinates(temporaryStagePosition);
        wildColorPicker.pack();
        wildColorPicker.setPosition(
            temporaryStagePosition.x - (wildColorPicker.getWidth() / 2f),
            temporaryStagePosition.y + 12f
        );
    }

    /** Sends a play-card turn action to the server. */
    private void submitPlayCard(int handIndex, CardColor chosenColor) {
        pendingTurnSubmission = true;
        Network.GameTurnActionRequest request = new Network.GameTurnActionRequest();
        request.actionType = Network.GameTurnActionRequest.ActionType.PLAY_CARD;
        request.handIndex = handIndex;
        request.chosenColor = chosenColor;
        NetworkManager.sendTCP(request);
        if (pendingDuoAfterPlay) {
            sendDuoAnnouncement();
        }
    }

    // === Card assets and helpers ===

    /** Creates a visible or hidden card actor with optional hover behavior. */
    private Actor createCardActor(Card card, boolean hidden, boolean hoverEnabled, float width, float height) {
        return hoverEnabled
            ? new HoverCardActor(card, hidden, width, height)
            : new StaticCardActor(card, hidden, width, height);
    }

    /** Loads a card texture or creates a placeholder if missing. */
    private Texture loadCardTexture(Card card, boolean hidden) {
        String resolvedCardId = hidden ? "card_back" : card.assetId();
        String texturePath = CARD_ASSET_ROOT + resolvedCardId + ".png";
        return Gdx.files.internal(texturePath).exists()
            ? trackTexture(new Texture(Gdx.files.internal(texturePath)))
            : createPlaceholderCardTexture(card, hidden);
    }

    /** Creates a simple generated board background texture. */
    private Texture createBoardBackgroundTexture() {
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.valueOf("0d4d3d"));
        pixmap.fill();
        pixmap.setColor(Color.valueOf("0a3a2f"));
        pixmap.fillRectangle(0, 0, 16, 4);
        pixmap.fillRectangle(0, 12, 16, 4);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return trackTexture(texture);
    }

    /** Creates a subtle background drawable for card piles. */
    private TextureRegionDrawable createPileGlowTexture() {
        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(1f, 1f, 1f, 0.22f));
        pixmap.fill();
        Texture texture = trackTexture(new Texture(pixmap));
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    /** Creates a generated placeholder texture for a missing card asset. */
    private Texture createPlaceholderCardTexture(Card card, boolean hidden) {
        Pixmap pixmap = new Pixmap(184, 276, Pixmap.Format.RGBA8888);

        Color faceColor = hidden ? Color.valueOf("1b1f3b") : getCardColor(card);
        pixmap.setColor(Color.WHITE);
        fillRoundedRectangle(pixmap, 0, 0, 184, 276, 26);

        pixmap.setColor(Color.valueOf("111111"));
        fillRoundedRectangle(pixmap, 6, 6, 172, 264, 22);

        pixmap.setColor(faceColor);
        fillRoundedRectangle(pixmap, 12, 12, 160, 252, 18);

        pixmap.setColor(new Color(1f, 1f, 1f, 0.92f));
        pixmap.fillCircle(92, 138, hidden ? 50 : 58);

        if (hidden) {
            pixmap.setColor(Color.valueOf("f5f5f5"));
            for (int line = -120; line <= 120; line += 24) {
                pixmap.drawLine(20, 138 + line, 164, line + 20);
            }
        } else {
            drawValue(pixmap, extractCardValue(card), 68, 94, 8, Color.valueOf("1a1a1a"));
            drawValue(pixmap, extractCornerValue(card), 24, 24, 4, Color.WHITE);
            drawValue(pixmap, extractCornerValue(card), 136, 222, 4, Color.WHITE);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return trackTexture(texture);
    }

    /** Draws blocky card text onto a pixmap. */
    private void drawValue(Pixmap pixmap, String value, int startX, int startY, int scale, Color color) {
        pixmap.setColor(color);
        int x = startX;
        for (int i = 0; i < value.length(); i++) {
            x += drawGlyph(pixmap, Character.toUpperCase(value.charAt(i)), x, startY, scale) + scale;
        }
    }

    /** Draws a filled rounded rectangle onto a pixmap. */
    private void fillRoundedRectangle(Pixmap pixmap, int x, int y, int width, int height, int radius) {
        pixmap.fillRectangle(x + radius, y, width - (radius * 2), height);
        pixmap.fillRectangle(x, y + radius, width, height - (radius * 2));
        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + width - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + height - radius - 1, radius);
        pixmap.fillCircle(x + width - radius - 1, y + height - radius - 1, radius);
    }

    /** Draws one blocky glyph and returns its width. */
    private int drawGlyph(Pixmap pixmap, char glyph, int x, int y, int scale) {
        String[] pattern = switch (glyph) {
            case '0' -> new String[]{"111", "101", "101", "101", "111"};
            case '1' -> new String[]{"010", "110", "010", "010", "111"};
            case '2' -> new String[]{"111", "001", "111", "100", "111"};
            case '3' -> new String[]{"111", "001", "111", "001", "111"};
            case '4' -> new String[]{"101", "101", "111", "001", "001"};
            case '5' -> new String[]{"111", "100", "111", "001", "111"};
            case '6' -> new String[]{"111", "100", "111", "101", "111"};
            case '7' -> new String[]{"111", "001", "001", "010", "010"};
            case '8' -> new String[]{"111", "101", "111", "101", "111"};
            case '9' -> new String[]{"111", "101", "111", "001", "111"};
            case 'D' -> new String[]{"110", "101", "101", "101", "110"};
            case 'R' -> new String[]{"110", "101", "110", "101", "101"};
            case 'S' -> new String[]{"111", "100", "111", "001", "111"};
            case 'W' -> new String[]{"101", "101", "101", "111", "101"};
            case '+' -> new String[]{"010", "010", "111", "010", "010"};
            default -> new String[]{"111", "101", "101", "101", "111"};
        };

        for (int row = 0; row < pattern.length; row++) {
            String line = pattern[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '1') {
                    pixmap.fillRectangle(x + (col * scale), y + ((pattern.length - row - 1) * scale), scale, scale);
                }
            }
        }
        return pattern[0].length() * scale;
    }

    /** Extracts display text for the center of a card. */
    private String extractCardValue(Card card) {
        if (card == null) {
            return "?";
        }
        return switch (card.symbol()) {
            case NUM_0 -> "0";
            case NUM_1 -> "1";
            case NUM_2 -> "2";
            case NUM_3 -> "3";
            case NUM_4 -> "4";
            case NUM_5 -> "5";
            case NUM_6 -> "6";
            case NUM_7 -> "7";
            case NUM_8 -> "8";
            case NUM_9 -> "9";
            case SKIP -> "S";
            case SWITCH_ORDER -> "R";
            case PLUS_2 -> "D+2";
            case CHANGE_COLOR -> "W";
            case CHANGE_COLOR_PLUS_4 -> "W+4";
        };
    }

    /** Extracts shortened display text for card corners. */
    private String extractCornerValue(Card card) {
        String value = extractCardValue(card);
        return value.length() > 2 ? value.substring(0, 2) : value;
    }

    /** Resolves the display color for a card ID. */
    private Color getCardColor(Card card) {
        if (card == null || card.color() == null) {
            return Color.valueOf("2b2d42");
        }
        return switch (card.color()) {
            case RED -> Color.valueOf("d64045");
            case YELLOW -> Color.valueOf("f0b429");
            case GREEN -> Color.valueOf("2f9e44");
            case BLUE -> Color.valueOf("1971c2");
        };
    }

    /** Tracks a texture so it can be disposed with the screen. */
    private Texture trackTexture(Texture texture) {
        disposableTextures.add(texture);
        return texture;
    }

    // === Lifecycle ===

    /** Updates layout-sensitive actors after a viewport resize. */
    public void resize(int width, int height) {
        getViewport().update(width, height, true);
        updateWildColorPickerPosition();
        updateDuoButtonPosition();
        updateDuoOverlayPosition();
    }

    /** Updates actor actions and overlay positions each frame. */
    @Override
    public void act(float delta) {
        super.act(delta);
        updateWildColorPickerPosition();
        updateDuoButtonPosition();
        updateDuoOverlayPosition();
    }

    /** Positions the DUO button in the lower-left corner. */
    private void updateDuoButtonPosition() {
        if (duoButton == null) {
            return;
        }
        duoButton.setPosition(24f, 24f);
    }

    /** Positions the DUO overlay in the lower-right corner. */
    private void updateDuoOverlayPosition() {
        if (duoOverlayImage == null) {
            return;
        }
        duoOverlayImage.setPosition(
            getViewport().getWorldWidth() - duoOverlayImage.getWidth() - 24f,
            24f
        );
    }

    /** Clears listeners and disposes UI resources. */
    @Override
    public void dispose() {
        NetworkManager.clearGameStartListener(gameStateListener);
        NetworkManager.clearGameEndListener(gameEndListener);
        NetworkManager.clearGameDuoListener(gameDuoListener);
        for (Texture texture : disposableTextures) {
            texture.dispose();
        }
        skin.dispose();
        super.dispose();
    }

    // === Nested actors and models ===

    private class StaticCardActor extends Image {
        private final float width;
        private final float height;

        /** Creates a non-interactive card image actor. */
        private StaticCardActor(Card card, boolean hidden, float width, float height) {
            super(loadCardTexture(card, hidden));
            this.width = width;
            this.height = height;
            setScaling(Scaling.fit);
            setSize(width, height);
        }

        /** Replaces this actor's displayed card texture. */
        private void setCard(Card card, boolean hidden) {
            setDrawable(new TextureRegionDrawable(loadCardTexture(card, hidden)));
            setSize(width, height);
        }
    }

    private class HoverCardActor extends StaticCardActor {
        /** Creates a card actor that scales up when hovered. */
        private HoverCardActor(Card card, boolean hidden, float width, float height) {
            super(card, hidden, width, height);
            setOrigin(width / 2f, height / 2f);
            setTouchable(Touchable.enabled);
            addListener(new InputListener() {
                /** Enlarges the card when the pointer enters. */
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (pointer != -1) {
                        return;
                    }
                    toFront();
                    clearActions();
                    addAction(Actions.scaleTo(CARD_HOVER_SCALE, CARD_HOVER_SCALE, CARD_HOVER_DURATION));
                }

                /** Restores the card scale when the pointer exits. */
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (pointer != -1) {
                        return;
                    }
                    clearActions();
                    addAction(Actions.scaleTo(1f, 1f, CARD_HOVER_DURATION));
                }
            });
        }
    }

    private final class PlayerHandCardActor extends HoverCardActor {
        /** Creates a clickable card actor for the local player's hand. */
        private PlayerHandCardActor(int handIndex, Card card, float width, float height) {
            super(card, false, width, height);
            addListener(new ClickListener() {
                /** Plays the card when it is double-clicked. */
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (getTapCount() == 2) {
                        playCardFromHand(handIndex);
                    }
                }
            });
        }
    }

    private final class DrawPileCardActor extends HoverCardActor {
        /** Creates a clickable draw pile card actor. */
        private DrawPileCardActor(float width, float height) {
            super(null, true, width, height);
            addListener(new ClickListener() {
                /** Draws cards when the pile is double-clicked. */
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (getTapCount() == 2) {
                        drawCardsForPlayer();
                    }
                }
            });
        }
    }

    private static final class PlayPile {
        private Card topCard;

        /** Stores the current top card of the play pile. */
        private PlayPile(Card initialTopCard) {
            topCard = initialTopCard;
        }

        /** Returns the current top play pile card ID. */
        private Card getTopCard() {
            return topCard;
        }

        /** Checks whether a card can be played on this pile. */
        private boolean canAcceptCard(Card card) {
            return CardRules.canPlay(topCard, card);
        }

        /** Updates the top card of the play pile. */
        private void placeCard(Card card) {
            topCard = card;
        }
    }
}
