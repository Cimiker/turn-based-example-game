package io.github.turn_based_example_game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;

import io.github.turn_based_example_game.screens.*;

import java.io.IOException;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    Preferences prefs;
    private Stage currentStage;

    @Override
    public void create() {

        try {
            NetworkManager.initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //Create settings if they don't exist
        prefs = Gdx.app.getPreferences("GameSettings");
        if(!prefs.contains("gameVolume")) {
            prefs.putFloat("gameVolume", 100f);
            prefs.putBoolean("isFullScreen", false);
            prefs.flush();
        }

        if(Account.load()) {
            Account.authenticate(success -> {
                Gdx.app.postRunnable(() -> {
                    if(success){
                        switchScreen(new MainMenuScreen(this));
                    }else{
                        switchScreen(new LoginScreen(this));
                    }
                });
            });
        } else {
            switchScreen(new LoginScreen(this));
        }
    }

    public void switchScreen(Stage newStage) {
        if (currentStage != null) currentStage.dispose();  // Clean up old stage
        currentStage = newStage;
        Gdx.input.setInputProcessor(currentStage);
    }

    @Override
    public void resize(int width, int height) {
        if (currentStage != null) {
            if (currentStage instanceof GameScreen) {
                ((GameScreen)currentStage).resize(width, height);
            } else  {
                currentStage.getViewport().update(width, height, true);
            }
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (currentStage != null) {
            currentStage.act(Gdx.graphics.getDeltaTime());
            currentStage.draw();
        }
    }

    @Override
    public void dispose() {
        if (currentStage != null) currentStage.dispose();
        NetworkManager.close();
    }
}
