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

public class RegisterScreen extends Stage {
    // Label to display registration status (e.g., errors)
    private final Label statusLabel;

    public RegisterScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());

        // Set this screen to handle user input
        Gdx.input.setInputProcessor(this);

        // Load UI skin and background image
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        // Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        // Create and configure background image
        // Image backgroundImage = new Image(backgroundTexture);
        // backgroundImage.setFillParent(true);
        // backgroundImage.setSize(backgroundImage.getWidth(), backgroundImage.getHeight());
        // backgroundImage.setPosition(backgroundImage.getX(), backgroundImage.getY());
        // backgroundImage.setZIndex(0); // Place it behind all other actors
        // addActor(backgroundImage);

        // UI Components
        Label titleLabel = new Label("Register", skin);

        // Username input field
        TextField usernameField = new TextField("", skin);
        usernameField.setMessageText("Enter username");

        // Password input field with masking
        TextField passwordField = new TextField("", skin);
        passwordField.setMessageText("Enter password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');

        // Buttons for registering and going back
        TextButton registerButton = new TextButton("Register", skin);
        TextButton backButton = new TextButton("Back to Login", skin);

        // Status label to display success/failure messages
        statusLabel = new Label("", skin);

        // Layout container
        Table table = new Table();
        table.setFillParent(true); // Make table occupy full screen

        // Add UI elements to the table in order
        table.add(titleLabel).colspan(2).padBottom(20).row();
        table.add(usernameField).width(300).padBottom(10).row();
        table.add(passwordField).width(300).padBottom(20).row();
        table.add(registerButton).width(200).padBottom(10).row();
        table.add(backButton).width(200).padBottom(10).row();
        table.add(statusLabel).padTop(10).row();

        // Add the layout table to the stage
        addActor(table);

        // Registration logic triggered on button click
        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Call Account.register with entered credentials
                Account.register(usernameField.getText(), passwordField.getText(), success -> {
                    Gdx.app.postRunnable(() -> {
                        if (success) {
                            game.switchScreen(new LoginScreen(game, soundController));
                        } else {
                            statusLabel.setText("Login failed. Try again.");
                        }
                    });
                });
            }
        });

        // Optionally, you can add a click listener to the backButton here
        // to return to the LoginScreen without registering
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.switchScreen(new LoginScreen(game, soundController));
            }
        });
    }

    @Override
    public void dispose() {
        // Dispose of resources and clean up when screen is closed
        super.dispose();
    }
}
