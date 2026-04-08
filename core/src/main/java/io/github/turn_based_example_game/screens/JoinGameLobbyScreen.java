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

public class JoinGameLobbyScreen extends Stage {

    public JoinGameLobbyScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());

        // Set this screen to receive input events
        Gdx.input.setInputProcessor(this);

        // Load the UI skin and background texture
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        // Create background image using the texture
        Image backgroundImage = new Image(backgroundTexture);

        // Create buttons for choosing lobby
        TextButton joinPublicLobbyButton = new TextButton("Join Public Lobby", skin);
        TextButton joinPrivateLobbyButton = new TextButton("Join Private Lobby", skin);
        TextButton createLobbyButton = new TextButton("Create Lobby", skin);
        TextButton backButton = new TextButton("Back", skin);

        // Configure and add the background image to the stage
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0); // Set background behind everything else
        addActor(backgroundImage);

        // Create a table to arrange UI components vertically
        Table table = new Table();
        table.setFillParent(true); // Fill the entire screen

        // Add UI components to the table
        table.add(joinPublicLobbyButton).width(200).padBottom(10).row();
        table.add(joinPrivateLobbyButton).width(200).padBottom(10).row();
        table.add(createLobbyButton).width(200).padBottom(10).row();
        table.add(backButton).width(150).padBottom(10).row();

        // Add the table to the stage
        addActor(table);

        // Handle "Back" button click to return to the main menu
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game, soundController)));
            }
        });

        // Start searching for a public lobby that is not full
        joinPublicLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Searching for public lobby...");
                //Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game, soundController)));
            }
        });

        // Go to insert the code for a private lobby
        joinPrivateLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new JoinPrivateLobbyScreen(game, soundController)));
            }
        });

        // Go to create lobby screen
        createLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Going to create lobby...");
                //Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game, soundController)));
            }
        });
    }
}
