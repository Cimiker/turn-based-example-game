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

/**
 * Login screen that allows the user to input credentials
 * and authenticate or navigate to the registration screen.
 */
public class LoginScreen extends Stage {
    private final Label statusLabel;  // Displays login status
    private String username;
    private String password;

    public LoginScreen(Main game) {
        super(new ScreenViewport());

        // Set this screen to handle input
        Gdx.input.setInputProcessor(this);

        // Load UI skin and background texture
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        // UI Components
        Image backgroundImage = new Image(backgroundTexture);  // Background image
        Label titleLabel = new Label("Login", skin);           // Screen title
        TextField usernameField = new TextField("", skin);     // Username input
        usernameField.setMessageText("Username");

        // Configure background
        backgroundImage.setPosition(backgroundImage.getX(), backgroundImage.getY());
        backgroundImage.setZIndex(0);
        addActor(backgroundImage);

        // Password input setup
        TextField passwordField = new TextField("", skin);
        passwordField.setMessageText("Password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');

        // Buttons
        TextButton loginButton = new TextButton("Login", skin);
        TextButton registerButton = new TextButton("Register", skin);

        // Status label for login feedback
        statusLabel = new Label("", skin);

        // Layout container
        Table table = new Table();
        table.setFillParent(true);  // Fill the screen

        // Add components to layout
        table.add(titleLabel).colspan(2).padBottom(20).row();
        table.add(usernameField).width(300).padBottom(10).row();
        table.add(passwordField).width(300).padBottom(20).row();
        table.add(loginButton).width(200).padBottom(10).row();
        table.add(registerButton).width(200).padBottom(10).row();
        table.add(statusLabel).padTop(10).row();

        addActor(table);

        // Handle login logic
        loginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                username = usernameField.getText();
                password = passwordField.getText();
                Account.setCredentials(username, password);  // Set entered credentials

                // Attempt authentication
                Account.authenticate(success -> {
                    Gdx.app.postRunnable(() -> {
                        if (success) {
                            // If successful, go to main menu
                            game.switchScreen(new MainMenuScreen(game));
                        } else {
                            // Show failure message
                            statusLabel.setText("Login failed. Try again.");
                        }
                    });
                });
            }
        });

        // Navigate to registration screen
        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.switchScreen(new RegisterScreen(game));
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose(); // Clean up resources
    }
}
