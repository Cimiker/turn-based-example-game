package io.github.turn_based_example_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.turn_based_example_game.screens.GameScreen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class Game {

    private List<Account> players; // List of units in the current game instance
    private final SpriteBatch spriteBatch; // Used for rendering units
    private final Listener listener; // Listener for ongoing game updates
    private static Listener joinGameListener; // Listener for joining the game
    private static final AtomicBoolean joinProcessComplete = new AtomicBoolean(false); // To prevent duplicate join handling
    private GameScreen gameScreen; // Reference to the game UI screen

    public Game(List<Account> players) {
        this.players = players;
        spriteBatch = new SpriteBatch();

        listener = new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                // Handle game state updates from the server
                if (object instanceof Network.GameStateUpdate) {
                    Network.GameStateUpdate update = (Network.GameStateUpdate) object;
                    Gdx.app.postRunnable(() -> {

                    });
                }
                // Handle end-of-game message
                if(object instanceof Network.GameEnd){
                    Network.GameEnd end = (Network.GameEnd) object;
                    Gdx.app.postRunnable(() -> {

                    });
                }
            }
        };
    }

    // Sets the UI screen associated with this game instance
    public void setScreen(GameScreen screen) {
        this.gameScreen = screen;
    }

    // Renders all units using the sprite batch
    public void render(){
        spriteBatch.begin();


        spriteBatch.end();
    }

    // Cleans up resources and detaches listeners
    public void dispose(){
        if(listener != null){
            NetworkManager.getClient().removeListener(listener);
        }
        spriteBatch.dispose();
    }

    public SpriteBatch getSpriteBatch() {
        return this.spriteBatch;
    }
}
