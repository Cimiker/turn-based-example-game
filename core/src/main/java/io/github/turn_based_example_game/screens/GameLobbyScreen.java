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

import java.util.function.Consumer;

public class GameLobbyScreen extends Stage {
    private final Main game;
    private final Skin skin;
    private final Label statusLabel;
    private final Table playersTable;
    private final TextButton readyButton;
    private final TextButton startGameButton;
    private final Label lobbyIdValue;
    private final Label maxPlayersValue;
    private final Label lobbyModeValue;
    private final Label fillWithBotsValue;
    private final Runnable lobbyStateListener;
    private final Consumer<Network.LobbyOperationResult> lobbyOperationListener;
    private final Consumer<Network.GameStateUpdate> gameStartListener;
    private Network.LobbyState latestLobbyState;

    public GameLobbyScreen(Main game) {
        super(new ScreenViewport());
        this.game = game;
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
                Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game)));
            }
        });

        readyButton = new TextButton("Ready up", skin);
        readyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                NetworkManager.toggleReady();
            }
        });

        startGameButton = new TextButton("Start Game", skin);
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!startGameButton.isDisabled()) {
                    NetworkManager.startLobbyGame();
                }
            }
        });

        statusLabel = new Label("Waiting for lobby state...", skin);
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);

        playersTable = new Table();
        playersTable.defaults().pad(8f).left();

        lobbyIdValue = new Label("-", skin);
        maxPlayersValue = new Label("-", skin);
        lobbyModeValue = new Label("-", skin);
        fillWithBotsValue = new Label("-", skin);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        addActor(root);

        Table topBar = new Table();
        topBar.add(backButton).width(140f).left();
        topBar.add().expandX();
        topBar.add(new Label("Lobby Code:", skin)).padRight(8f);
        topBar.add(lobbyIdValue).right();
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
        rightPanel.add(new Label("Max Players:", skin));
        rightPanel.add(maxPlayersValue).row();
        rightPanel.add(new Label("Lobby Mode:", skin));
        rightPanel.add(lobbyModeValue).row();
        rightPanel.add(new Label("Fill with Bots:", skin));
        rightPanel.add(fillWithBotsValue).row();

        root.add().height(16f).colspan(3).row();
        root.add(leftPanel).width(220f).growY().left().top();
        root.add(centerPanel).expand().fill();
        root.add(rightPanel).width(260f).growY().right().top().row();

        root.add().height(20f).colspan(3).row();
        root.add(startGameButton).colspan(3).width(240f).height(60f).center().bottom();

        lobbyStateListener = this::refreshLobby;
        lobbyOperationListener = result -> {
            if (result.lobbyClosed) {
                Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game)));
                return;
            }
            if (result.message != null && !result.message.isBlank()) {
                statusLabel.setText(result.message);
            }
        };
        gameStartListener = update -> {
            Network.LobbyState lobbySnapshot = latestLobbyState == null ? null : copyLobbyState(latestLobbyState);
            Gdx.app.postRunnable(() -> game.switchScreen(new GameScreen(game, lobbySnapshot, update)));
        };
        NetworkManager.setLobbyStateListener(lobbyStateListener);
        NetworkManager.setLobbyOperationListener(lobbyOperationListener);
        NetworkManager.setGameStartListener(gameStartListener);

        refreshLobby();
    }

    private void refreshLobby() {
        Network.LobbyState lobbyState = NetworkManager.getCurrentLobby();
        playersTable.clearChildren();

        if (lobbyState == null) {
            latestLobbyState = null;
            lobbyIdValue.setText("-");
            maxPlayersValue.setText("-");
            lobbyModeValue.setText("-");
            fillWithBotsValue.setText("-");
            readyButton.setDisabled(true);
            startGameButton.setDisabled(true);
            if ("Waiting for lobby state...".contentEquals(statusLabel.getText())) {
                statusLabel.setText("Waiting for lobby state...");
            }
            return;
        }

        latestLobbyState = copyLobbyState(lobbyState);
        lobbyIdValue.setText(lobbyState.lobbyId == null ? "-" : lobbyState.lobbyId);
        maxPlayersValue.setText(String.valueOf(lobbyState.settings.maxPlayers));
        lobbyModeValue.setText(lobbyState.settings.lobbyMode);
        fillWithBotsValue.setText(lobbyState.settings.fillWithBots ? "Yes" : "No");

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

        readyButton.setDisabled(false);
        readyButton.setText(currentPlayerReady ? "Unready" : "Ready up");

        boolean isOwner = NetworkManager.isLobbyOwner(currentUsername);
        boolean allPlayersReady = NetworkManager.areAllPlayersReady();
        startGameButton.setVisible(isOwner);
        startGameButton.setDisabled(!isOwner || !allPlayersReady);

        if (!isOwner) {
            statusLabel.setText("Waiting for the lobby owner to start the game.");
        } else if (allPlayersReady) {
            statusLabel.setText("All players are ready. You can start the game.");
        } else {
            statusLabel.setText("Start Game unlocks when every player in the lobby is ready.");
        }
    }

    private Network.LobbyState copyLobbyState(Network.LobbyState source) {
        Network.LobbyState copy = new Network.LobbyState();
        copy.lobbyId = source.lobbyId;
        copy.settings = copyLobbySettings(source.settings);
        for (Network.LobbyPlayer player : source.players) {
            copy.players.add(copyLobbyPlayer(player));
        }
        return copy;
    }

    private Network.LobbySettings copyLobbySettings(Network.LobbySettings source) {
        Network.LobbySettings copy = new Network.LobbySettings();
        if (source == null) {
            return copy;
        }
        copy.maxPlayers = source.maxPlayers;
        copy.lobbyMode = source.lobbyMode;
        copy.fillWithBots = source.fillWithBots;
        return copy;
    }

    private Network.LobbyPlayer copyLobbyPlayer(Network.LobbyPlayer source) {
        Network.LobbyPlayer copy = new Network.LobbyPlayer();
        copy.username = source.username;
        copy.ready = source.ready;
        copy.owner = source.owner;
        copy.bot = source.bot;
        return copy;
    }

    @Override
    public void dispose() {
        NetworkManager.clearLobbyStateListener(lobbyStateListener);
        NetworkManager.clearLobbyOperationListener(lobbyOperationListener);
        NetworkManager.clearGameStartListener(gameStartListener);
        skin.dispose();
        super.dispose();
    }
}
