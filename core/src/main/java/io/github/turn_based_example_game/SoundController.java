package io.github.turn_based_example_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SoundController {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public float gameVolume;

    Preferences prefs;

    public SoundController() {
        prefs = Gdx.app.getPreferences("Preferences");
        gameVolume = prefs.getFloat("gameVolume");
    }

    public void playCardSound() {
        System.out.println("Playing card sound at " + gameVolume);
    }
}
