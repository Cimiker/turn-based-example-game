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
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.NetworkManager;
import io.github.turn_based_example_game.SoundController;

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
    private static final String[] NUMBERED_CARD_COLORS = {"red", "yellow", "green", "blue"};
    private static final int NUMBERED_CARD_VARIANTS = 10;
    private static final String[] DEFAULT_PLAYER_HAND = {
        "red_7_filled",
        "yellow_2_white",
        "blue_skip_filled",
        "green_switch_order_white",
        "change_color",
        "red_plus_2_filled",
        "blue_9_white"
    };

    private final Array<Texture> disposableTextures = new Array<>();
    private final Main game;
    private final SoundController soundController;
    private final Skin skin;
    private final List<Network.LobbyPlayer> playOrder = new ArrayList<>();
    private final List<String> playerHandCards = new ArrayList<>();
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

    public GameScreen() {
        this(null, null, null, null);
    }

    public GameScreen(Main game, SoundController soundController) {
        this(game, soundController, null, null);
    }

    public GameScreen(Main game, SoundController soundController, Network.LobbyState lobbyState) {
        this(game, soundController, lobbyState, null);
    }

    public GameScreen(Main game, SoundController soundController, Network.LobbyState lobbyState, Network.GameStateUpdate initialGameState) {
        super(new ScreenViewport());
        this.game = game;
        this.soundController = soundController;
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        playOrder.addAll(buildPlayOrder(lobbyState != null ? lobbyState : NetworkManager.getCurrentLobby()));
        initializePlayerHand(initialGameState);
        playPile = new PlayPile(resolveInitialTopPlayPileCardId(initialGameState));
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

    private Table createTopRow() {
        float drawZoneWidth = (CARD_WIDTH * DRAW_PILE_SCALE) + 64f;
        Table row = new Table();
        row.add().width(drawZoneWidth);
        row.add(createOpponentArea(getPlayerAtSeat(2))).expandX().center().top();
        row.add(createDrawPileArea()).width(drawZoneWidth).right().top();
        return row;
    }

    private Table createMiddleRow() {
        Table row = new Table();
        row.defaults().pad(12f);
        row.add(createSideOpponentArea(getPlayerAtSeat(3))).width(220f).expandY().left();
        row.add(createCenterArea()).expand().center();
        row.add(createSideOpponentArea(getPlayerAtSeat(1))).width(220f).expandY().right();
        return row;
    }

    private Table createBottomRow() {
        Table row = new Table();
        row.add(createPlayerArea(getPlayerAtSeat(0))).center().bottom();
        return row;
    }

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

    private Table createCenterArea() {
        Table section = new Table();
        section.defaults().pad(10f);
        playPileCardActor = new StaticCardActor(playPile.getTopCardId(), false, CARD_WIDTH, CARD_HEIGHT);
        section.add(createPile("Play Pile", playPileCardActor, CARD_WIDTH, CARD_HEIGHT))
            .size(CARD_WIDTH + 14f, CARD_HEIGHT + 38f);

        return section;
    }

    private Table createWildColorPicker() {
        Table picker = new Table(skin);
        picker.defaults().width(88f).height(40f).pad(4f);
        picker.setVisible(false);
        picker.setTouchable(Touchable.disabled);

        picker.add(createWildColorButton("Red", "red"));
        picker.add(createWildColorButton("Green", "green"));
        picker.row();
        picker.add(createWildColorButton("Blue", "blue"));
        picker.add(createWildColorButton("Yellow", "yellow"));
        picker.pack();
        return picker;
    }

    private TextButton createWildColorButton(String text, String colorId) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                chooseWildColor(colorId);
            }
        });
        return button;
    }

    private TextButton createDuoButton() {
        TextButton button = new TextButton("DUO", skin);
        button.getLabel().setFontScale(1.35f);
        button.setSize(150f, 72f);
        button.setPosition(24f, 24f);
        button.setVisible(false);
        button.setTouchable(Touchable.disabled);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                announceDuo();
            }
        });
        return button;
    }

    private Image createDuoOverlayImage() {
        Texture texture = trackTexture(new Texture(Gdx.files.internal(CARD_ASSET_ROOT + "duo.png")));
        Image image = new Image(texture);
        image.setSize(170f, 170f);
        image.setVisible(false);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Table createDrawPileArea() {
        float drawWidth = CARD_WIDTH * DRAW_PILE_SCALE;
        float drawHeight = CARD_HEIGHT * DRAW_PILE_SCALE;
        return createPile("Draw Pile", new DrawPileCardActor(drawWidth, drawHeight), drawWidth, drawHeight);
    }

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

    private Table createHiddenHand(int cardCount) {
        Table cards = new Table();
        cards.defaults().padLeft(-30f);
        populateHiddenHand(cards, cardCount);
        return cards;
    }

    private void populateHiddenHand(Table cards, int cardCount) {
        cards.clearChildren();
        float width = CARD_WIDTH * OPPONENT_CARD_SCALE;
        float height = CARD_HEIGHT * OPPONENT_CARD_SCALE;
        for (int i = 0; i < cardCount; i++) {
            Actor card = createCardActor("card_back", true, false, width, height);
            cards.add(card).size(width, height);
        }
        cards.invalidateHierarchy();
    }

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

    private Network.LobbyPlayer getPlayerAtSeat(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= playOrder.size()) {
            return null;
        }
        return playOrder.get(seatIndex);
    }

    private Network.LobbyPlayer createLocalFallbackPlayer() {
        Network.LobbyPlayer player = new Network.LobbyPlayer();
        player.username = Account.getUsername() == null ? "You" : Account.getUsername();
        player.bot = false;
        return player;
    }

    private Network.LobbyPlayer copyPlayer(Network.LobbyPlayer source) {
        Network.LobbyPlayer copy = new Network.LobbyPlayer();
        copy.username = source.username;
        copy.ready = source.ready;
        copy.owner = source.owner;
        copy.bot = source.bot;
        return copy;
    }

    private String getPlayerLabel(Network.LobbyPlayer player) {
        String currentUsername = Account.getUsername();
        if (!player.bot && currentUsername != null && currentUsername.equals(player.username)) {
            return "You";
        }
        return player.bot ? player.username + " [Bot]" : player.username;
    }

    private Label createPlayerTitleLabel(String username, String baseText) {
        Label title = new Label(baseText, skin);
        title.setColor(Color.WHITE);
        if (username != null) {
            playerLabelsByUsername.put(username, title);
            playerLabelTextByUsername.put(username, baseText);
        }
        return title;
    }

    private void initializePlayerHand(Network.GameStateUpdate initialGameState) {
        playerHandCards.clear();
        if (initialGameState != null && initialGameState.playerHandCardIds != null && !initialGameState.playerHandCardIds.isEmpty()) {
            playerHandCards.addAll(initialGameState.playerHandCardIds);
            return;
        }

        for (String cardId : DEFAULT_PLAYER_HAND) {
            playerHandCards.add(cardId);
        }
    }

    private String resolveInitialTopPlayPileCardId(Network.GameStateUpdate initialGameState) {
        if (initialGameState != null && initialGameState.topPlayPileCardId != null && !initialGameState.topPlayPileCardId.isBlank()) {
            return initialGameState.topPlayPileCardId;
        }
        return pickRandomNumberedCardId();
    }

    private String resolveInitialTurnUsername(Network.GameStateUpdate initialGameState) {
        if (initialGameState != null && initialGameState.currentTurnUsername != null && !initialGameState.currentTurnUsername.isBlank()) {
            return initialGameState.currentTurnUsername;
        }
        return playOrder.isEmpty() ? Account.getUsername() : playOrder.get(0).username;
    }

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

    private void updateDisplayedHandCounts(Network.GameStateUpdate update) {
        if (update.playerUsernames == null || update.playerHandCounts == null || update.playerUsernames.size() != update.playerHandCounts.size()) {
            return;
        }

        handCountsByUsername.clear();
        for (int i = 0; i < update.playerUsernames.size(); i++) {
            handCountsByUsername.put(update.playerUsernames.get(i), update.playerHandCounts.get(i));
        }
    }

    private String pickRandomNumberedCardId() {
        String color = NUMBERED_CARD_COLORS[ThreadLocalRandom.current().nextInt(NUMBERED_CARD_COLORS.length)];
        int value = ThreadLocalRandom.current().nextInt(NUMBERED_CARD_VARIANTS);
        return color + "_" + value + "_filled";
    }

    private void applyGameStateUpdate(Network.GameStateUpdate update) {
        if (update == null) {
            return;
        }

        if (update.topPlayPileCardId != null && !update.topPlayPileCardId.isBlank()) {
            playPile.placeCard(update.topPlayPileCardId);
        }
        if (update.currentTurnUsername != null && !update.currentTurnUsername.isBlank()) {
            currentTurnUsername = update.currentTurnUsername;
        }
        currentPlayerCanDraw = update.currentPlayerCanDraw;
        turnActionsLocked = update.turnActionsLocked;
        serverShowDuoButton = update.showDuoButton;
        playerHandCards.clear();
        if (update.playerHandCardIds != null) {
            playerHandCards.addAll(update.playerHandCardIds);
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

    private void handleGameEnd(Network.GameEnd end) {
        if (game == null) {
            return;
        }
        game.switchScreen(new GameEndScreen(game, soundController, end));
    }

    private void refreshSynchronizedUi() {
        if (playPileCardActor != null) {
            playPileCardActor.setCard(playPile.getTopCardId(), false);
        }
        refreshHiddenHandTables();
        refreshPlayerLabels();
        updateDuoButtonState();
        updateWildColorPickerPosition();
    }

    private void refreshHiddenHandTables() {
        for (Map.Entry<String, Table> entry : hiddenHandTablesByUsername.entrySet()) {
            populateHiddenHand(entry.getValue(), getDisplayedHandCount(entry.getKey()));
        }
    }

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

    private int getDisplayedHandCount(String username) {
        if (username != null && username.equals(Account.getUsername())) {
            return playerHandCards.size();
        }
        Integer count = handCountsByUsername.get(username);
        return count == null ? DEFAULT_HAND_SIZE : count;
    }

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

    private boolean shouldDelayDuoAnnouncement() {
        return isLocalPlayersTurn() && playerHandCards.size() == 3 && playerHasPlayableCard();
    }

    private void sendDuoAnnouncement() {
        pendingDuoAfterPlay = false;
        pendingDuoAnnouncement = true;
        updateDuoButtonState();
        NetworkManager.sendTCP(new Network.GameDuoRequest());
    }

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

    private void handleGameDuo(Network.GameDuoEvent duoEvent) {
        if (duoEvent == null) {
            pendingDuoAnnouncement = false;
            updateDuoButtonState();
            return;
        }
        showDuoOverlay();
    }

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

    private boolean playerHasPlayableCard() {
        for (String cardId : playerHandCards) {
            if (playPile.canAcceptCard(cardId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLocalPlayersTurn() {
        String currentUsername = Account.getUsername();
        return currentUsername != null && currentUsername.equals(currentTurnUsername);
    }

    private boolean canLocalPlayerPlay() {
        return isLocalPlayersTurn() && !turnActionsLocked && !pendingTurnSubmission && !pendingWildColorChoice;
    }

    private boolean canLocalPlayerDraw() {
        return canLocalPlayerPlay() && currentPlayerCanDraw;
    }

    private void drawCardsForPlayer() {
        if (!canLocalPlayerDraw()) {
            return;
        }

        pendingTurnSubmission = true;
        Network.GameTurnActionRequest request = new Network.GameTurnActionRequest();
        request.actionType = Network.GameTurnActionRequest.ActionType.DRAW_CARD;
        NetworkManager.sendTCP(request);
    }

    private void playCardFromHand(int handIndex) {
        if (!canLocalPlayerPlay()) {
            return;
        }

        if (handIndex < 0 || handIndex >= playerHandCards.size()) {
            return;
        }

        String playedCardId = playerHandCards.get(handIndex);
        if (!playPile.canAcceptCard(playedCardId)) {
            return;
        }

        if (requiresWildColorChoice(playedCardId)) {
            promptForWildColor(handIndex);
            return;
        }
        submitPlayCard(handIndex, null);
    }

    private boolean requiresWildColorChoice(String cardId) {
        return "change_color".equals(cardId) || "change_color_plus_4".equals(cardId);
    }

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

    private void chooseWildColor(String colorId) {
        if (!pendingWildColorChoice || pendingWildHandIndex < 0 || colorId == null) {
            return;
        }

        pendingWildColorChoice = false;
        hideWildColorPicker();
        submitPlayCard(pendingWildHandIndex, colorId);
        pendingWildHandIndex = -1;
    }

    private void hideWildColorPicker() {
        if (wildColorPicker != null) {
            wildColorPicker.setVisible(false);
            wildColorPicker.setTouchable(Touchable.disabled);
        }
    }

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

    private void submitPlayCard(int handIndex, String chosenColor) {
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

    private Actor createCardActor(String cardId, boolean hidden, boolean hoverEnabled, float width, float height) {
        return hoverEnabled
            ? new HoverCardActor(cardId, hidden, width, height)
            : new StaticCardActor(cardId, hidden, width, height);
    }

    private Texture loadCardTexture(String cardId, boolean hidden) {
        String resolvedCardId = hidden ? "card_back" : cardId;
        String texturePath = CARD_ASSET_ROOT + resolvedCardId + ".png";
        return Gdx.files.internal(texturePath).exists()
            ? trackTexture(new Texture(Gdx.files.internal(texturePath)))
            : createPlaceholderCardTexture(resolvedCardId, hidden);
    }

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

    private TextureRegionDrawable createPileGlowTexture() {
        Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(1f, 1f, 1f, 0.22f));
        pixmap.fill();
        Texture texture = trackTexture(new Texture(pixmap));
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    private Texture createPlaceholderCardTexture(String cardId, boolean hidden) {
        Pixmap pixmap = new Pixmap(184, 276, Pixmap.Format.RGBA8888);

        Color faceColor = hidden ? Color.valueOf("1b1f3b") : getCardColor(cardId);
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
            drawValue(pixmap, extractCardValue(cardId), 68, 94, 8, Color.valueOf("1a1a1a"));
            drawValue(pixmap, extractCornerValue(cardId), 24, 24, 4, Color.WHITE);
            drawValue(pixmap, extractCornerValue(cardId), 136, 222, 4, Color.WHITE);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return trackTexture(texture);
    }

    private void drawValue(Pixmap pixmap, String value, int startX, int startY, int scale, Color color) {
        pixmap.setColor(color);
        int x = startX;
        for (int i = 0; i < value.length(); i++) {
            x += drawGlyph(pixmap, Character.toUpperCase(value.charAt(i)), x, startY, scale) + scale;
        }
    }

    private void fillRoundedRectangle(Pixmap pixmap, int x, int y, int width, int height, int radius) {
        pixmap.fillRectangle(x + radius, y, width - (radius * 2), height);
        pixmap.fillRectangle(x, y + radius, width, height - (radius * 2));
        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + width - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + height - radius - 1, radius);
        pixmap.fillCircle(x + width - radius - 1, y + height - radius - 1, radius);
    }

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

    private String extractCardValue(String cardId) {
        if (cardId.startsWith("wild_draw_four")) {
            return "W+4";
        }
        if (cardId.startsWith("wild")) {
            return "W";
        }
        if (cardId.endsWith("draw_two")) {
            return "D+2";
        }
        if (cardId.endsWith("reverse")) {
            return "R";
        }
        if (cardId.endsWith("skip")) {
            return "S";
        }
        int splitIndex = cardId.indexOf('_');
        return splitIndex >= 0 ? cardId.substring(splitIndex + 1).toUpperCase() : cardId.toUpperCase();
    }

    private String extractCornerValue(String cardId) {
        String value = extractCardValue(cardId);
        return value.length() > 2 ? value.substring(0, 2) : value;
    }

    private static String extractCardColorId(String cardId) {
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
        if ("change_color".equals(cardId) || "change_color_plus_4".equals(cardId)) {
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
        return "change_color".equals(cardId) || "change_color_plus_4".equals(cardId);
    }

    private Color getCardColor(String cardId) {
        if (cardId.startsWith("red")) {
            return Color.valueOf("d64045");
        }
        if (cardId.startsWith("yellow")) {
            return Color.valueOf("f0b429");
        }
        if (cardId.startsWith("green")) {
            return Color.valueOf("2f9e44");
        }
        if (cardId.startsWith("blue")) {
            return Color.valueOf("1971c2");
        }
        if (cardId.startsWith("wild")) {
            return Color.valueOf("2b2d42");
        }
        return Color.GRAY;
    }

    private Texture trackTexture(Texture texture) {
        disposableTextures.add(texture);
        return texture;
    }

    public void resize(int width, int height) {
        getViewport().update(width, height, true);
        updateWildColorPickerPosition();
        updateDuoButtonPosition();
        updateDuoOverlayPosition();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        updateWildColorPickerPosition();
        updateDuoButtonPosition();
        updateDuoOverlayPosition();
    }

    private void updateDuoButtonPosition() {
        if (duoButton == null) {
            return;
        }
        duoButton.setPosition(24f, 24f);
    }

    private void updateDuoOverlayPosition() {
        if (duoOverlayImage == null) {
            return;
        }
        duoOverlayImage.setPosition(
            getViewport().getWorldWidth() - duoOverlayImage.getWidth() - 24f,
            24f
        );
    }

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

    private class StaticCardActor extends Image {
        private final float width;
        private final float height;

        private StaticCardActor(String cardId, boolean hidden, float width, float height) {
            super(loadCardTexture(cardId, hidden));
            this.width = width;
            this.height = height;
            setScaling(Scaling.fit);
            setSize(width, height);
        }

        private void setCard(String cardId, boolean hidden) {
            setDrawable(new TextureRegionDrawable(loadCardTexture(cardId, hidden)));
            setSize(width, height);
        }
    }

    private class HoverCardActor extends StaticCardActor {
        private HoverCardActor(String cardId, boolean hidden, float width, float height) {
            super(cardId, hidden, width, height);
            setOrigin(width / 2f, height / 2f);
            setTouchable(Touchable.enabled);
            addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (pointer != -1) {
                        return;
                    }
                    toFront();
                    clearActions();
                    addAction(Actions.scaleTo(CARD_HOVER_SCALE, CARD_HOVER_SCALE, CARD_HOVER_DURATION));
                }

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
        private PlayerHandCardActor(int handIndex, String cardId, float width, float height) {
            super(cardId, false, width, height);
            addListener(new ClickListener() {
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
        private DrawPileCardActor(float width, float height) {
            super("card_back", true, width, height);
            addListener(new ClickListener() {
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
        private String topCardId;

        private PlayPile(String initialTopCardId) {
            topCardId = initialTopCardId;
        }

        private String getTopCardId() {
            return topCardId;
        }

        private boolean canAcceptCard(String cardId) {
            if (isAlwaysPlayableCard(cardId)) {
                return true;
            }

            String topSymbol = extractCardSymbol(topCardId);
            String playedSymbol = extractCardSymbol(cardId);
            if (topSymbol != null && topSymbol.equals(playedSymbol)) {
                return true;
            }

            String topColor = extractCardColorId(topCardId);
            String playedColor = extractCardColorId(cardId);
            if (topColor != null && topColor.equals(playedColor)) {
                return true;
            }

            Integer topNumber = extractCardNumber(topCardId);
            Integer playedNumber = extractCardNumber(cardId);
            return topNumber != null && topNumber.equals(playedNumber);
        }

        private void placeCard(String cardId) {
            topCardId = cardId;
        }
    }
}
