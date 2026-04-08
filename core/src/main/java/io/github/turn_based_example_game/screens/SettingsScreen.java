package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.SoundController;

/**
 * A screen for adjusting settings such as sound volumes and display mode.
 * This extends LibGDX's Stage and builds the UI elements on construction.
 */
public class SettingsScreen extends Stage {

    public SettingsScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());

        // Set this screen to receive input events
        Gdx.input.setInputProcessor(this);

        // Load the UI skin and background texture
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        // Create background image using the texture
        Image backgroundImage = new Image(backgroundTexture);

        // Create volume labels and sliders for menu and game music
        Label menuSoundLable = new Label("Menu Sound", skin);
        Slider menuSoundSlider = new Slider(0, 100, 2, false, skin);
        //menuSoundSlider.setValue(soundController.menuThemeVolume); // Set initial value

        Label gameSoundLable = new Label("Game Sound", skin);
        Slider gameSoundSlider = new Slider(0, 100, 2, false, skin);
        //gameSoundSlider.setValue(soundController.gameThemeVolume); // Set initial value

        // Create buttons for fullscreen toggle and going back to main menu
        TextButton fullscreenButton = new TextButton("Toggle Fullscreen", skin);
        TextButton backButton = new TextButton("Back", skin);

        // Configure and add the background image to the stage
        // backgroundImage.setSize(backgroundImage.getWidth(), backgroundImage.getHeight());
        // backgroundImage.setPosition(backgroundImage.getX(), backgroundImage.getY());
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0); // Set background behind everything else
        addActor(backgroundImage);

        // Create a table to arrange UI components vertically
        Table table = new Table();
        table.setFillParent(true); // Fill the entire screen

        // Add UI components to the table
        table.add(menuSoundLable).row();
        table.add(menuSoundSlider).row();
        table.add(gameSoundLable).row();
        table.add(gameSoundSlider).row();
        table.add(fullscreenButton).padTop(10).row();
        table.add(backButton).padTop(10).row();

        // Add the table to the stage
        addActor(table);

        // Handle "Back" button click to return to the main menu
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game, soundController)));
            }
        });

        // Toggle fullscreen mode on button click
        fullscreenButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (Gdx.graphics.isFullscreen()) {
                    Gdx.graphics.setWindowedMode(1280, 720); // Exit fullscreen
                } else {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode()); // Enter fullscreen
                }
            }
        });

        // Update the menu music volume when the slider value changes
        menuSoundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int soundValue = (int) menuSoundSlider.getValue();
                //soundController.changeMenuThemeVolume(soundValue);
            }
        });

        // Update the game music volume when the slider value changes
        gameSoundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int soundValue = (int) gameSoundSlider.getValue();
                //soundController.changeGameThemeVolume(soundValue);
            }
        });
    }
}
