package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Account;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.NetworkManager;
import io.github.turn_based_example_game.SoundController;

public class GameLobbyScreen extends Stage {
    private final Main game;
    private final SoundController soundController;
    private final Skin skin;
    private final Label statusLabel;
    private final Table playersTable;
    private final TextButton readyButton;
    private final TextButton startGameButton;

    public GameLobbyScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());
        this.game = game;
        this.soundController = soundController;
        this.skin = new Skin(Gdx.files.internal("uiskin.json"));

        Gdx.input.setInputProcessor(this);

        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));
        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0);
        addActor(backgroundImage);

        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                NetworkManager.leaveLobby();
                Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game, soundController)));
            }
        });

        readyButton = new TextButton("Ready up", skin);
        readyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                NetworkManager.toggleReady(Account.getUsername());
                refreshLobby();
            }
        });

        startGameButton = new TextButton("Start Game", skin);
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (startGameButton.isDisabled()) {
                    return;
                }
                Gdx.app.postRunnable(() -> game.switchScreen(new GameScreen(game, soundController)));
            }
        });

        statusLabel = new Label("", skin);
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);

        playersTable = new Table();
        playersTable.defaults().pad(8f).left();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        addActor(root);

        Table topBar = new Table();
        topBar.add(backButton).width(140f).left();
        topBar.add().expandX();
        root.add(topBar).growX().top().colspan(3).row();

        Table leftPanel = new Table(skin);
        leftPanel.pad(20f);
        leftPanel.defaults().pad(8f);
        leftPanel.add(new Label("Actions", skin)).padBottom(18f).row();
        leftPanel.add(readyButton).width(180f).height(56f).row();

        Table centerPanel = new Table(skin);
        centerPanel.pad(20f);
        centerPanel.defaults().pad(6f);
        Label playersTitle = new Label("Players in Lobby", skin);
        playersTitle.setAlignment(Align.center);
        centerPanel.add(playersTitle).growX().padBottom(12f).row();
        centerPanel.add(playersTable).grow().top().left().row();
        centerPanel.add(statusLabel).growX().padTop(12f);

        Table rightPanel = new Table(skin);
        rightPanel.pad(20f);
        rightPanel.defaults().pad(8f).left();
        rightPanel.add(new Label("Settings", skin)).padBottom(18f).row();

        root.add().height(16f).colspan(3).row();
        root.add(leftPanel).width(220f).growY().left().top();
        root.add(centerPanel).expand().fill();
        root.add(createSettingsPanel(rightPanel)).width(260f).growY().right().top().row();

        root.add().height(20f).colspan(3).row();
        root.add(startGameButton).colspan(3).width(240f).height(60f).center().bottom();

        refreshLobby();
    }

    private Table createSettingsPanel(Table rightPanel) {
        Network.LobbyState lobbyState = NetworkManager.getCurrentLobby();

        if (lobbyState == null || lobbyState.settings == null) {
            rightPanel.add(new Label("No lobby data available.", skin)).left().row();
            return rightPanel;
        }

        rightPanel.add(new Label("Max Players: " + lobbyState.settings.maxPlayers, skin)).row();
        rightPanel.add(new Label("Lobby Mode: " + lobbyState.settings.lobbyMode, skin)).row();
        rightPanel.add(new Label("Fill with Bots: " + (lobbyState.settings.fillWithBots ? "Yes" : "No"), skin)).row();
        return rightPanel;
    }

    private void refreshLobby() {
        Network.LobbyState lobbyState = NetworkManager.getCurrentLobby();

        playersTable.clearChildren();
        if (lobbyState == null) {
            statusLabel.setText("Lobby not found.");
            readyButton.setDisabled(true);
            startGameButton.setDisabled(true);
            return;
        }

        for (Network.LobbyPlayer player : lobbyState.players) {
            String readiness = player.ready ? "READY" : "NOT READY";
            String ownerTag = player.owner ? " (Owner)" : "";
            String botTag = player.bot ? " [Bot]" : "";

            Label playerLabel = new Label(player.username + ownerTag + botTag, skin);
            Label readyLabel = new Label(readiness, skin);
            readyLabel.setColor(player.ready ? Color.GREEN : Color.SCARLET);

            playersTable.add(playerLabel).growX().left();
            playersTable.add(readyLabel).right().row();
        }

        String currentUsername = Account.getUsername();
        boolean currentPlayerReady = false;
        for (Network.LobbyPlayer player : lobbyState.players) {
            if (currentUsername != null && currentUsername.equals(player.username)) {
                currentPlayerReady = player.ready;
                break;
            }
        }

        readyButton.setText(currentPlayerReady ? "Unready" : "Ready up");

        boolean isOwner = NetworkManager.isLobbyOwner(currentUsername);
        boolean allPlayersReady = NetworkManager.areAllPlayersReady();
        startGameButton.setDisabled(!isOwner || !allPlayersReady);

        if (!isOwner) {
            statusLabel.setText("Waiting for the lobby owner to start the game.");
        } else if (allPlayersReady) {
            statusLabel.setText("All players are ready. You can start the game.");
        } else {
            statusLabel.setText("Start Game unlocks when every player in the lobby is ready.");
        }
    }
}
