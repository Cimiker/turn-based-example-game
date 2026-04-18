package io.github.turn_based_example_game;

import com.esotericsoftware.kryonet.Client;

import java.io.IOException;

public class NetworkManager {
    private static Client client;
    private static Network.LobbyState currentLobby;

    public static void initialize() throws IOException {
        client = new Client();
        client.start();
        Network.register(client);
        client.connect(5000, "127.0.0.1", 8080, 8081);

    }

    // Clean up and close the connection
    public static void close() {
        if (client != null) {
            client.stop();
            System.out.println("Client stopped.");
        }
    }

    public static void reconnect() {
        try {
            if (client.isConnected()) {
                client.close(); // Close the existing connection
            }

            // Try reconnecting
            client.connect(5000, "127.0.0.1", 8080, 8081);
            System.out.println("Reconnected successfully!");
        } catch (IOException e) {
            System.err.println("Reconnection failed: " + e.getMessage());
        }
    }

    public static void sendTCP(Object object){
        client.sendTCP(object);
    }

    public static void sendUDP(Object object){
        client.sendUDP(object);
    }

    // Get the client instance for other parts of the app (e.g., for adding listeners)
    public static Client getClient() {
        return client;
    }

    public static Network.LobbyState createLobby(Network.LobbySettings settings) {
        Network.LobbyState lobbyState = new Network.LobbyState();
        lobbyState.settings = settings;

        Network.LobbyPlayer owner = new Network.LobbyPlayer();
        owner.username = Account.getUsername() != null ? Account.getUsername() : "Player";
        owner.owner = true;
        owner.ready = false;
        owner.bot = false;
        lobbyState.players.add(owner);

        if (settings.fillWithBots) {
            for (int i = 2; i <= settings.maxPlayers; i++) {
                Network.LobbyPlayer bot = new Network.LobbyPlayer();
                bot.username = "Bot " + (i - 1);
                bot.owner = false;
                bot.ready = true;
                bot.bot = true;
                lobbyState.players.add(bot);
            }
        }

        currentLobby = lobbyState;
        return currentLobby;
    }

    public static Network.LobbyState getCurrentLobby() {
        return currentLobby;
    }

    public static void leaveLobby() {
        currentLobby = null;
    }

    public static void toggleReady(String username) {
        if (currentLobby == null || username == null) {
            return;
        }

        for (Network.LobbyPlayer player : currentLobby.players) {
            if (username.equals(player.username)) {
                player.ready = !player.ready;
                return;
            }
        }
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
}
