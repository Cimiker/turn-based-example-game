package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.SoundController;

public class GameScreen extends Stage {
    private static final float CARD_WIDTH = 92f;
    private static final float CARD_HEIGHT = 138f;
    private static final float OPPONENT_CARD_SCALE = 0.72f;

    private final Array<Texture> disposableTextures = new Array<>();
    private final Skin skin;

    public GameScreen() {
        this(null, null);
    }

    public GameScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));
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
        addActor(root);

        root.add(createOpponentArea()).growX().top().padBottom(32f).row();
        root.add(createCenterArea()).expand().center().row();
        root.add(createPlayerArea()).growX().bottom().padTop(32f);
    }

    private Table createOpponentArea() {
        Table section = new Table();
        section.defaults().pad(4f);

        Label title = new Label("Opponent", skin);
        title.setColor(Color.WHITE);
        section.add(title).padBottom(10f).row();

        Table cards = new Table();
        cards.defaults().padLeft(-34f);
        for (int i = 0; i < 7; i++) {
            Actor card = createCardActor("back", true, CARD_WIDTH * OPPONENT_CARD_SCALE, CARD_HEIGHT * OPPONENT_CARD_SCALE);
            cards.add(card).size(CARD_WIDTH * OPPONENT_CARD_SCALE, CARD_HEIGHT * OPPONENT_CARD_SCALE);
        }

        section.add(cards);
        return section;
    }

    private Table createCenterArea() {
        Table section = new Table();
        section.defaults().pad(10f);

        Label title = new Label("Center Stack", skin);
        title.setColor(new Color(1f, 0.95f, 0.7f, 1f));
        section.add(title).colspan(2).padBottom(12f).row();

        section.add(createPile("Draw Pile", "back")).size(CARD_WIDTH + 14f, CARD_HEIGHT + 38f);
        section.add(createPile("Discard", "wild_draw_four")).size(CARD_WIDTH + 14f, CARD_HEIGHT + 38f);

        return section;
    }

    private Table createPile(String labelText, String cardId) {
        Table pile = new Table();
        pile.defaults().pad(4f);

        Image glow = new Image(createPileGlowTexture());
        glow.setColor(1f, 1f, 1f, 0.18f);
        pile.setBackground(glow.getDrawable());

        Label label = new Label(labelText, skin);
        label.setColor(Color.WHITE);

        pile.add(createCardActor(cardId, false, CARD_WIDTH, CARD_HEIGHT)).size(CARD_WIDTH, CARD_HEIGHT).row();
        pile.add(label).padTop(4f);
        return pile;
    }

    private Table createPlayerArea() {
        Table section = new Table();
        section.defaults().pad(4f);

        Label title = new Label("Your Hand", skin);
        title.setColor(Color.WHITE);
        section.add(title).padBottom(12f).row();

        Table cards = new Table();
        cards.defaults().padLeft(-30f);

        String[] hand = {"red_7", "yellow_2", "blue_skip", "green_reverse", "wild", "red_draw_two", "blue_9"};
        for (String cardId : hand) {
            cards.add(createCardActor(cardId, false, CARD_WIDTH, CARD_HEIGHT)).size(CARD_WIDTH, CARD_HEIGHT);
        }

        section.add(cards).bottom();
        return section;
    }

    private Actor createCardActor(String cardId, boolean hidden, float width, float height) {
        String texturePath = hidden ? "cards/back.png" : "cards/" + cardId + ".png";
        Texture texture = Gdx.files.internal(texturePath).exists()
            ? trackTexture(new Texture(Gdx.files.internal(texturePath)))
            : createPlaceholderCardTexture(cardId, hidden);

        Image image = new Image(texture);
        image.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        image.setSize(width, height);
        return image;
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
}
