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
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.NetworkManager;

import java.util.function.Consumer;

public class JoinPrivateLobbyScreen extends Stage {
    private final Label statusLabel;
    private boolean navigatingToLobby;
    private final Runnable lobbyStateListener;
    private final Consumer<io.github.turn_based_example_game.Network.LobbyOperationResult> lobbyOperationListener;

    public JoinPrivateLobbyScreen(Main game) {
        super(new ScreenViewport());
        Gdx.input.setInputProcessor(this);

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0);
        addActor(backgroundImage);

        TextField lobbyCodeField = new TextField("", skin);
        TextButton joinLobbyButton = new TextButton("Join Lobby", skin);
        TextButton backButton = new TextButton("Back", skin);

        lobbyCodeField.setMessageText("Lobby code");
        lobbyCodeField.setMaxLength(5);
        lobbyCodeField.setTextFieldFilter((textField, c) -> c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
        lobbyCodeField.setTextFieldListener((textField, c) -> {
            if (Character.isLetter(c)) {
                textField.setText(textField.getText().toUpperCase());
                textField.setCursorPosition(textField.getText().length());
            }
        });

        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.SCARLET);

        Table table = new Table();
        table.setFillParent(true);

        table.add(statusLabel).padBottom(10).row();
        table.add(lobbyCodeField).width(300).padBottom(10).row();
        table.add(joinLobbyButton).width(200).padBottom(10).row();
        table.add(backButton).width(150).padBottom(10).row();

        addActor(table);

        lobbyStateListener = () -> {
            if (navigatingToLobby) {
                return;
            }

            if (NetworkManager.getCurrentLobby() != null) {
                navigatingToLobby = true;
                Gdx.app.postRunnable(() -> game.switchScreen(new GameLobbyScreen(game)));
            }
        };

        lobbyOperationListener = result -> {
            if (result.success && result.lobbyState != null) {
                if (!navigatingToLobby) {
                    navigatingToLobby = true;
                    Gdx.app.postRunnable(() -> game.switchScreen(new GameLobbyScreen(game)));
                }
                return;
            }

            navigatingToLobby = false;
            if (result.message != null && !result.message.isBlank()) {
                statusLabel.setText(result.message);
            } else {
                statusLabel.setText("A lobby couldn't be found");
            }
        };
        NetworkManager.setLobbyStateListener(lobbyStateListener);
        NetworkManager.setLobbyOperationListener(lobbyOperationListener);

        joinLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                statusLabel.setText("");
                navigatingToLobby = false;
                if (!NetworkManager.isConnected()) {
                    statusLabel.setText("Server is not available");
                    return;
                }
                String lobbyCode = lobbyCodeField.getText().trim().toUpperCase();
                if (lobbyCode.length() != 5) {
                    statusLabel.setText("A lobby couldn't be found");
                    return;
                }
                NetworkManager.joinLobbyByCode(lobbyCode);
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new JoinGameLobbyScreen(game)));
            }
        });
    }

    @Override
    public void dispose() {
        NetworkManager.clearLobbyStateListener(lobbyStateListener);
        NetworkManager.clearLobbyOperationListener(lobbyOperationListener);
        super.dispose();
    }
}
