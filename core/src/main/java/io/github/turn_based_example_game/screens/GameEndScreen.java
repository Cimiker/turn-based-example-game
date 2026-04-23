package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.NetworkManager;

public class GameEndScreen extends Stage {
    private final Skin skin;
    private final Texture backgroundTexture;

    public GameEndScreen() {
        this(null, null);
    }

    public GameEndScreen(Main game, Network.GameEnd end) {
        super(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        backgroundTexture = Gdx.files.internal("menuBackground.png").exists()
            ? new Texture(Gdx.files.internal("menuBackground.png"))
            : null;

        if (backgroundTexture != null) {
            Image backgroundImage = new Image(backgroundTexture);
            backgroundImage.setFillParent(true);
            addActor(backgroundImage);
        }

        String winner = end == null || end.winner == null || end.winner.isBlank() ? "Winner unknown" : end.winner;

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        addActor(root);

        Label title = new Label("Game Over", skin);
        title.setColor(new Color(1f, 0.95f, 0.7f, 1f));
        title.setFontScale(1.4f);

        Label winnerLabel = new Label("Winner: " + winner, skin);
        winnerLabel.setAlignment(Align.center);
        winnerLabel.setColor(Color.WHITE);

        TextButton backButton = new TextButton("Back to main menu", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                NetworkManager.leaveGame();
                if (game != null) {
                    game.switchScreen(new MainMenuScreen(game));
                }
            }
        });

        root.add(title).padBottom(18f).row();
        root.add(winnerLabel).padBottom(18f).row();
        root.add(backButton).width(240f).height(56f);

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        skin.dispose();
        super.dispose();
    }
}
