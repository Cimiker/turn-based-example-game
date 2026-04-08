package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.NetworkManager;
import io.github.turn_based_example_game.PlayerStats;
import io.github.turn_based_example_game.SoundController;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

/**
 * Displays the leaderboard of player stats retrieved from the server.
 */
public class LeaderboardScreen extends Stage {
    private Table leaderboardTable;       // Table to display leaderboard entries
    private Skin skin;                    // UI skin
    private Main main;                    // Reference to game instance
    private SoundController soundController; // Reference to sound controller

    public LeaderboardScreen(Main main, SoundController soundController) {
        super(new ScreenViewport());

        this.main = main;
        this.soundController = soundController;

        // Set this stage as input processor
        Gdx.input.setInputProcessor(this);
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));
        //Texture menuBoxTexture = new Texture(Gdx.files.internal("menuBoxBrown.png"));

        // UI Components
        Image backgroundImage = new Image(backgroundTexture); // Main background
        //Image menuBoxImage = new Image(menuBoxTexture);

        // Layout background and menu box
        float margin = 20f;

        /*
        // size the box to fill the virtual world minus margin on each side
        menuBoxImage.setSize(
            getViewport().getWorldWidth() * 0.3f,
            getViewport().getWorldHeight() - 2 * margin
        );
        // position it margin units in from the bottom-left
        menuBoxImage.setPosition(getViewport().getWorldWidth() / 2 - menuBoxImage.getWidth() / 2, margin);
        menuBoxImage.setColor(1f, 1f, 1f, 0.95f);
        menuBoxImage.setZIndex(1);

        // Position and layer background image
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0); // Behind all UI
        addActor(backgroundImage);
        addActor(menuBoxImage);
        */

        // Main container layout
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        addActor(mainTable);

        // Back button setup
        TextButton backButton = new TextButton("Back", skin);
        mainTable.add(backButton).top().left().pad(10);  // Top-left aligned with padding
        mainTable.row();  // Move to next row in table layout

        // Table to hold leaderboard entries
        leaderboardTable = new Table();
        mainTable.add(leaderboardTable).expand().fill();  // Fill the remaining space

        // Display loading message while waiting for data
        leaderboardTable.add(new Label("Loading...", skin));

        // Back button navigation logic
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                main.switchScreen(new MainMenuScreen(main, soundController));
            }
        });

        // Request leaderboard data from server
        sendLeaderboardRequest();
    }

    /**
     * Sends a request to the server for leaderboard data and sets up a listener to receive it.
     */
    private void sendLeaderboardRequest() {
        Network.LeaderboardRequest request = new Network.LeaderboardRequest();

        Listener listener = new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.LeaderboardResponse) {
                    Network.LeaderboardResponse response = (Network.LeaderboardResponse) object;
                    updateLeaderboard(response.leaderboard);  // Populate leaderboard table
                    NetworkManager.getClient().removeListener(this);  // Remove listener after use
                }
            }
        };

        NetworkManager.getClient().addListener(listener);  // Attach listener
        NetworkManager.sendTCP(request);                   // Send request to server
    }

    /**
     * Updates the leaderboard table with retrieved player stats.
     *
     * @param leaderboard Array of player statistics
     */
    private void updateLeaderboard(PlayerStats[] leaderboard) {
        leaderboardTable.clear();  // Clear previous contents

        // Add table headers
        leaderboardTable.add(new Label("Rank", skin)).padRight(10);
        leaderboardTable.add(new Label("Username", skin)).padRight(10);
        leaderboardTable.add(new Label("Wins", skin)).padRight(10);
        leaderboardTable.add(new Label("Losses", skin)).row();

        // Fill table with leaderboard data
        for (int i = 0; i < leaderboard.length; i++) {
            PlayerStats entry = leaderboard[i];
            leaderboardTable.add(new Label(String.valueOf(i + 1), skin)).padRight(10);
            leaderboardTable.add(new Label(entry.getUsername(), skin)).padRight(10);
            leaderboardTable.add(new Label(entry.getWins().toString(), skin)).padRight(10);
            leaderboardTable.add(new Label(entry.getLosses().toString(), skin)).row();
        }
    }
}
