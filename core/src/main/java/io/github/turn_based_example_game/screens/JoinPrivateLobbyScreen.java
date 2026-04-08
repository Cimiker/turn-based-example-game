package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Account;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.SoundController;

public class JoinPrivateLobbyScreen extends Stage {

    private final Label statusLabel;
    private String lobbyCode;

    public JoinPrivateLobbyScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());

        // Set this screen to receive input events
        Gdx.input.setInputProcessor(this);

        // Load the UI skin and background texture
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        // Create background image using the texture
        Image backgroundImage = new Image(backgroundTexture);

        // Configure background
        //backgroundImage.setPosition(backgroundImage.getX(), backgroundImage.getY());
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0);
        addActor(backgroundImage);

        // Create elements for choosing lobby
        TextField lobbyCodeField = new TextField("", skin);
        TextButton joinLobbyButton = new TextButton("Join Lobby", skin);
        TextButton backButton = new TextButton("Back", skin);

        lobbyCodeField.setMessageText("Lobby code");

        // Status label for feedback
        statusLabel = new Label("", skin);

        // Create a table to arrange UI components vertically
        Table table = new Table();
        table.setFillParent(true); // Fill the entire screen

        // Add UI components to the table
        table.add(lobbyCodeField).width(300).padBottom(10).row();
        table.add(joinLobbyButton).width(200).padBottom(10).row();
        table.add(backButton).width(150).padBottom(10).row();
        table.add(statusLabel).padTop(10).row();

        // Add the table to the stage
        addActor(table);

        joinLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                lobbyCode = lobbyCodeField.getText();

                System.out.println(lobbyCode);
            }
        });

        // Handle "Back" button click to return to the main menu
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new JoinGameLobbyScreen(game, soundController)));
            }
        });
    }
}
