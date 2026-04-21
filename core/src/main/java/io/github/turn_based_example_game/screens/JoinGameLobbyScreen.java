package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.NetworkManager;
import io.github.turn_based_example_game.SoundController;

import java.util.function.Consumer;

public class JoinGameLobbyScreen extends Stage {
    private final Consumer<io.github.turn_based_example_game.Network.LobbyOperationResult> lobbyOperationListener;

    public JoinGameLobbyScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());
        Gdx.input.setInputProcessor(this);

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        Image backgroundImage = new Image(backgroundTexture);

        TextButton joinPublicLobbyButton = new TextButton("Join Public Lobby", skin);
        TextButton joinPrivateLobbyButton = new TextButton("Join Private Lobby", skin);
        TextButton createLobbyButton = new TextButton("Create Lobby", skin);
        TextButton backButton = new TextButton("Back", skin);
        Label statusLabel = new Label("", skin);
        statusLabel.setColor(Color.SCARLET);

        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0);
        addActor(backgroundImage);

        Table table = new Table();
        table.setFillParent(true);

        table.add(statusLabel).padBottom(10).row();
        table.add(joinPublicLobbyButton).width(200).padBottom(10).row();
        table.add(joinPrivateLobbyButton).width(200).padBottom(10).row();
        table.add(createLobbyButton).width(200).padBottom(10).row();
        table.add(backButton).width(150).padBottom(10).row();

        addActor(table);

        lobbyOperationListener = result -> {
            if (result.success && result.lobbyState != null) {
                Gdx.app.postRunnable(() -> game.switchScreen(new GameLobbyScreen(game, soundController)));
                return;
            }

            if (result.message != null && !result.message.isBlank()) {
                statusLabel.setText(result.message);
            }
        };
        NetworkManager.setLobbyOperationListener(lobbyOperationListener);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game, soundController)));
            }
        });

        joinPublicLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                statusLabel.setText("");
                NetworkManager.joinPublicLobby();
            }
        });

        joinPrivateLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new JoinPrivateLobbyScreen(game, soundController)));
            }
        });

        createLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new CreateLobbyScreen(game, soundController)));
            }
        });
    }

    @Override
    public void dispose() {
        NetworkManager.clearLobbyOperationListener(lobbyOperationListener);
        super.dispose();
    }
}
