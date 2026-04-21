package io.github.turn_based_example_game;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.function.Consumer;

public class NetworkManager {
    private static Client client;
    private static Network.LobbyState currentLobby;
    private static Runnable lobbyStateListener;
    private static Consumer<Network.LobbyOperationResult> lobbyOperationListener;
    private static Consumer<Network.GameStateUpdate> gameStartListener;

    public static void initialize() throws IOException {
        client = new Client();
        client.start();
        Network.register(client);
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.LobbyState lobbyState) {
                    currentLobby = lobbyState;
                    notifyLobbyStateChanged();
                    return;
                }

                if (object instanceof Network.LobbyOperationResult result) {
                    if (result.lobbyClosed) {
                        currentLobby = null;
                        notifyLobbyStateChanged();
                    } else if (result.lobbyState != null) {
                        currentLobby = result.lobbyState;
                        notifyLobbyStateChanged();
                    }
                    notifyLobbyOperation(result);
                    return;
                }

                if (object instanceof Network.GameStateUpdate update) {
                    currentLobby = null;
                    notifyGameStart(update);
                }
            }
        });
        client.connect(5000, "127.0.0.1", 8080, 8081);
    }

    public static void close() {
        if (client != null) {
            client.stop();
            System.out.println("Client stopped.");
        }
        currentLobby = null;
    }

    public static void reconnect() {
        try {
            if (client.isConnected()) {
                client.close();
            }

            client.connect(5000, "127.0.0.1", 8080, 8081);
            System.out.println("Reconnected successfully!");
        } catch (IOException e) {
            System.err.println("Reconnection failed: " + e.getMessage());
        }
    }

    public static void sendTCP(Object object) {
        client.sendTCP(object);
    }

    public static void sendUDP(Object object) {
        client.sendUDP(object);
    }

    public static Client getClient() {
        return client;
    }

    public static boolean isConnected() {
        return client != null && client.isConnected();
    }

    public static void createLobby(Network.LobbySettings settings) {
        Network.CreateLobbyRequest request = new Network.CreateLobbyRequest();
        request.settings = settings;
        sendTCP(request);
    }

    public static Network.LobbyState getCurrentLobby() {
        return currentLobby;
    }

    public static void leaveLobby() {
        if (currentLobby != null) {
            sendTCP(new Network.LeaveLobbyRequest());
        }
        currentLobby = null;
        notifyLobbyStateChanged();
    }

    public static void toggleReady() {
        sendTCP(new Network.ToggleLobbyReadyRequest());
    }

    public static void startLobbyGame() {
        sendTCP(new Network.StartLobbyGameRequest());
    }

    public static void joinLobbyByCode(String lobbyId) {
        Network.JoinLobbyByCodeRequest request = new Network.JoinLobbyByCodeRequest();
        request.lobbyId = lobbyId;
        sendTCP(request);
    }

    public static void joinPublicLobby() {
        sendTCP(new Network.JoinPublicLobbyRequest());
    }

    public static boolean isLobbyOwner(String username) {
        if (currentLobby == null || username == null) {
            return false;
        }

        for (Network.LobbyPlayer player : currentLobby.players) {
            if (username.equals(player.username)) {
                return player.owner;
            }
        }
        return false;
    }

    public static boolean areAllPlayersReady() {
        if (currentLobby == null || currentLobby.players.isEmpty()) {
            return false;
        }

        for (Network.LobbyPlayer player : currentLobby.players) {
            if (!player.ready) {
                return false;
            }
        }
        return true;
    }

    public static void setLobbyStateListener(Runnable listener) {
        lobbyStateListener = listener;
    }

    public static void clearLobbyStateListener(Runnable listener) {
        if (lobbyStateListener == listener) {
            lobbyStateListener = null;
        }
    }

    public static void setLobbyOperationListener(Consumer<Network.LobbyOperationResult> listener) {
        lobbyOperationListener = listener;
    }

    public static void clearLobbyOperationListener(Consumer<Network.LobbyOperationResult> listener) {
        if (lobbyOperationListener == listener) {
            lobbyOperationListener = null;
        }
    }

    public static void setGameStartListener(Consumer<Network.GameStateUpdate> listener) {
        gameStartListener = listener;
    }

    public static void clearGameStartListener(Consumer<Network.GameStateUpdate> listener) {
        if (gameStartListener == listener) {
            gameStartListener = null;
        }
    }

    private static void notifyLobbyStateChanged() {
        if (lobbyStateListener != null) {
            Gdx.app.postRunnable(lobbyStateListener);
        }
    }

    private static void notifyLobbyOperation(Network.LobbyOperationResult result) {
        if (lobbyOperationListener != null) {
            Gdx.app.postRunnable(() -> lobbyOperationListener.accept(result));
        }
    }

    private static void notifyGameStart(Network.GameStateUpdate update) {
        if (gameStartListener != null) {
            Gdx.app.postRunnable(() -> gameStartListener.accept(update));
        }
    }
}
