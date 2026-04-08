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

public class CreateLobbyScreen extends Stage {

    public CreateLobbyScreen(Main game, SoundController soundController) {
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

        // Create UI elements
        Label screenLabel = new Label("Lobby options", skin);
        TextButton twoPlayers = new TextButton("2", skin);
        TextButton threePlayers = new TextButton("3", skin);
        TextButton fourPlayers = new TextButton("4", skin);
        Label maxPlayersLabel = new Label("Maximum Players", skin);
        ButtonGroup maxPlayersButtonGroup = new ButtonGroup(twoPlayers, threePlayers, fourPlayers);
        TextButton privateButton = new TextButton("Private", skin);
        TextButton publicButton = new TextButton("Public", skin);
        Label lobbyModeLabel = new Label("Lobby Mode", skin);
        ButtonGroup lobbyModeButtonGroup = new ButtonGroup(privateButton, publicButton);
        TextButton yesBotsButton = new TextButton("Yes", skin);
        TextButton noBotsButton = new TextButton("No", skin);
        Label fillWithBotsLabel = new Label("Fill empty spots with Bots", skin);
        ButtonGroup botsButtonGroup = new ButtonGroup(yesBotsButton, noBotsButton);
        TextButton createLobbyButton = new TextButton("Create Lobby", skin);
        TextButton backButton = new TextButton("Back", skin);

        maxPlayersButtonGroup.setChecked("4");
        lobbyModeButtonGroup.setChecked("Private");
        botsButtonGroup.setChecked("Yes");

        // Create a table to arrange UI components vertically
        Table table = new Table();
        table.setFillParent(true); // Fill the entire screen

        // Create table for max player buttons
        Table maxPlayersTable = new Table();

        // Add UI components to the table
        table.add(screenLabel).padBottom(10).colspan(2).center().row();
        table.add(maxPlayersLabel).width(200).padBottom(10).row();
        maxPlayersTable.add(twoPlayers).width(50).padBottom(10).expandX();
        maxPlayersTable.add(threePlayers).width(50).padBottom(10).padLeft(100).padRight(100).expandX();
        maxPlayersTable.add(fourPlayers).width(50).padBottom(10).expandX();
        table.add(maxPlayersTable).colspan(2).row();
        table.add(lobbyModeLabel).width(200).padBottom(10).row();
        table.add(privateButton).width(200).padBottom(10);
        table.add(publicButton).width(200).padBottom(10).row();
        table.add(fillWithBotsLabel).width(200).padBottom(10).row();
        table.add(yesBotsButton).width(200).padBottom(10);
        table.add(noBotsButton).width(200).padBottom(10).row();
        table.add(createLobbyButton).width(200).padBottom(10).colspan(2).row();
        table.add(backButton).width(150).padBottom(10).colspan(2).row();

        // Add the table to the stage
        addActor(table);

        // Handle Create Lobby button click
        createLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println(maxPlayersButtonGroup.getChecked());
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
