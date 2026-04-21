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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Account;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.NetworkManager;
import io.github.turn_based_example_game.SoundController;

import java.util.ArrayList;
import java.util.List;

public class GameScreen extends Stage {
    private static final String CARD_ASSET_ROOT = "UnoCards/sprites/";
    private static final float CARD_WIDTH = 92f;
    private static final float CARD_HEIGHT = 138f;
    private static final float OPPONENT_CARD_SCALE = 0.62f;
    private static final float DRAW_PILE_SCALE = 0.72f;
    private static final int DISPLAY_PLAYER_COUNT = 4;
    private static final int DEFAULT_HAND_SIZE = 7;
    private static final float CARD_HOVER_SCALE = 1.08f;
    private static final float CARD_HOVER_DURATION = 0.08f;

    private final Array<Texture> disposableTextures = new Array<>();
    private final Skin skin;
    private final List<Network.LobbyPlayer> playOrder = new ArrayList<>();

    public GameScreen() {
        this(null, null, null);
    }

    public GameScreen(Main game, SoundController soundController) {
        this(game, soundController, null);
    }

    public GameScreen(Main game, SoundController soundController, Network.LobbyState lobbyState) {
        super(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        playOrder.addAll(buildPlayOrder(lobbyState != null ? lobbyState : NetworkManager.getCurrentLobby()));
        buildBoard();
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

        Label title = new Label(getPlayerLabel(player), skin);
        title.setColor(Color.WHITE);
        section.add(title).padBottom(10f).row();

        section.add(createHiddenHand(DEFAULT_HAND_SIZE)).center();
        return section;
    }

    private Table createSideOpponentArea(Network.LobbyPlayer player) {
        Table section = new Table();
        section.defaults().pad(4f);

        if (player == null) {
            return section;
        }

        Label title = new Label(getPlayerLabel(player), skin);
        title.setColor(Color.WHITE);
        title.setWrap(true);
        section.add(title).width(200f).center().padBottom(10f).row();
        section.add(createHiddenHand(DEFAULT_HAND_SIZE)).center();
        return section;
    }

    private Table createCenterArea() {
        Table section = new Table();
        section.defaults().pad(10f);

        Label title = new Label("Play Pile", skin);
        title.setColor(new Color(1f, 0.95f, 0.7f, 1f));
        section.add(title).padBottom(12f).row();
        section.add(createPile("Play Pile", "change_color_plus_4", false, false, CARD_WIDTH, CARD_HEIGHT))
            .size(CARD_WIDTH + 14f, CARD_HEIGHT + 38f);

        return section;
    }

    private Table createDrawPileArea() {
        float drawWidth = CARD_WIDTH * DRAW_PILE_SCALE;
        float drawHeight = CARD_HEIGHT * DRAW_PILE_SCALE;
        return createPile("Draw Pile", "card_back", true, true, drawWidth, drawHeight);
    }

    private Table createPile(String labelText, String cardId, boolean hidden, boolean hoverEnabled, float cardWidth, float cardHeight) {
        Table pile = new Table();
        pile.defaults().pad(4f);

        Image glow = new Image(createPileGlowTexture());
        glow.setColor(1f, 1f, 1f, 0.18f);
        pile.setBackground(glow.getDrawable());

        Label label = new Label(labelText, skin);
        label.setColor(Color.WHITE);

        pile.add(createCardActor(cardId, hidden, hoverEnabled, cardWidth, cardHeight)).size(cardWidth, cardHeight).row();
        pile.add(label).padTop(4f);
        return pile;
    }

    private Table createPlayerArea(Network.LobbyPlayer player) {
        Table section = new Table();
        section.defaults().pad(4f);

        String labelText = player == null ? "Your Hand" : getPlayerLabel(player);
        Label title = new Label(labelText, skin);
        title.setColor(Color.WHITE);
        section.add(title).padBottom(12f).row();

        Table cards = new Table();
        cards.defaults().padLeft(-30f);

        String[] hand = {
            "red_7_filled",
            "yellow_2_white",
            "blue_skip_filled",
            "green_switch_order_white",
            "change_color",
            "red_plus_2_filled",
            "blue_9_white"
        };
        for (String cardId : hand) {
            cards.add(createCardActor(cardId, false, true, CARD_WIDTH, CARD_HEIGHT)).size(CARD_WIDTH, CARD_HEIGHT);
        }

        section.add(cards).bottom();
        return section;
    }

    private Table createHiddenHand(int cardCount) {
        Table cards = new Table();
        cards.defaults().padLeft(-30f);
        float width = CARD_WIDTH * OPPONENT_CARD_SCALE;
        float height = CARD_HEIGHT * OPPONENT_CARD_SCALE;
        for (int i = 0; i < cardCount; i++) {
            Actor card = createCardActor("card_back", true, false, width, height);
            cards.add(card).size(width, height);
        }
        return cards;
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
    }

    @Override
    public void dispose() {
        for (Texture texture : disposableTextures) {
            texture.dispose();
        }
        skin.dispose();
        super.dispose();
    }

    private class StaticCardActor extends Image {
        private StaticCardActor(String cardId, boolean hidden, float width, float height) {
            super(loadCardTexture(cardId, hidden));
            setScaling(Scaling.fit);
            setSize(width, height);
        }
    }

    private final class HoverCardActor extends StaticCardActor {
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
}
