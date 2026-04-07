package io.github.turn_based_example_game;

import com.esotericsoftware.kryonet.Client;

import java.io.IOException;

public class NetworkManager {
    private static Client client;

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
}
