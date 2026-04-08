package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Account;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.SoundController;

/**
 * Displays the main menu after login. Provides navigation
 * to other parts of the game like settings, leaderboard, game start, etc.
 */
public class MainMenuScreen extends Stage {

    public MainMenuScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());

        // Set this screen to handle user input
        Gdx.input.setInputProcessor(this);

        // Load UI skin and background texture
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        // UI Components
        Image backgroundImage = new Image(backgroundTexture); // Main background

        Label titleLabel = new Label("Main Menu", skin);
        Label usernameLabel = new Label(Account.getUsername(), skin);

        TextButton startButton = new TextButton("Start Game", skin);
        TextButton leaderboardButton = new TextButton("Leaderboard", skin);
        TextButton tutorialButton = new TextButton("Tutorial", skin);
        TextButton settingsButton = new TextButton("Settings", skin);
        TextButton exitButton = new TextButton("Exit", skin);
        TextButton logOutButton = new TextButton("Log out", skin);

        // Configure and add the background image to the stage
        // backgroundImage.setSize(backgroundImage.getWidth(), backgroundImage.getHeight());
        // backgroundImage.setPosition(backgroundImage.getX(), backgroundImage.getY());
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0); // Set background behind everything else
        addActor(backgroundImage);

        // Layout container for all buttons and labels
        Table table = new Table();
        table.setFillParent(true); // Full screen layout

        // Add elements to the table in order
        table.add(titleLabel).colspan(2).padBottom(10).row();
        table.add(usernameLabel).colspan(2).padBottom(20).row();
        table.add(startButton).width(200).padBottom(10).row();
        table.add(leaderboardButton).width(200).padBottom(10).row();
        table.add(tutorialButton).width(200).padBottom(10).row();
        table.add(settingsButton).width(200).padBottom(10).row();
        table.add(exitButton).width(200).padBottom(10).row();
        table.add(logOutButton).width(200).padBottom(10).row();

        // Add table to the stage
        addActor(table);


        // Navigate to Join Game Lobby screen
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new JoinGameLobbyScreen(game, soundController)));
            }
        });

        // Navigate to Tutorial screen
        tutorialButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new TutorialScreen(game, soundController)));
            }
        });

        // Navigate to Settings screen
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new SettingsScreen(game, soundController)));
            }
        });

        // Log out and return to log in screen
        logOutButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                Account.delete(); // Clear saved account info
                //soundController.stopMenuTheme();
                //soundController.disposeMenuTheme();
                Gdx.app.postRunnable(() -> game.switchScreen(new LoginScreen(game, soundController)));
            }
        });

        // Exit application
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Navigate to Leaderboard screen
        leaderboardButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new LeaderboardScreen(game, soundController)));
            }
        });
    }

    @Override
    public void draw(){
        super.draw(); // Draw all actors added to the stage
    }
}
