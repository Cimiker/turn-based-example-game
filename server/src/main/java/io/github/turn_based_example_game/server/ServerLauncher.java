package io.github.turn_based_example_game.server;

import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Connection;
import java.io.IOException;


/** Launches the server application. */
public class ServerLauncher {

    public ServerLauncher() {com.esotericsoftware.kryonet.Server server = new com.esotericsoftware.kryonet.Server();
        server.start();
        Database.load();
        System.out.println(Database.getLeaderboard());
        GameManager gameManager = new GameManager(server);
        Network.register(server);

        try {
            server.bind(8080, 8081);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        server.addListener(new Listener() {
            @SuppressWarnings("unchecked")
            @Override
            public void received (Connection connection, Object object) {
                if (object instanceof Network.RegisterRequest request) {
                    Network.RegisterResponse response = new Network.RegisterResponse();

                    if (Database.registerUser(request.username, request.password)) {
                        response.success = true;
                        response.message = "Registration successful!";
                    } else {
                        response.success = false;
                        response.message = "Username already taken!";
                    }

                    connection.sendTCP(response);
                    return;
                }

                if (object instanceof Network.LoginRequest request) {
                    Network.LoginResponse response = new Network.LoginResponse();
                    Account account = Database.authenticate(connection, request.username, request.password);
                    if (account != null) {
                        response.success = true;
                        response.message = "Login successful!";
                    } else {
                        response.success = false;
                        response.message = "Invalid username or password!";
                    }
                    System.out.println(response);
                    connection.sendTCP(response);
                    return;
                }

                if (object instanceof Network.LeaderboardRequest) {
                    Network.LeaderboardResponse response = new Network.LeaderboardResponse();
                    response.leaderboard = Database.getLeaderboard();
                    connection.sendTCP(response);
                }

                Account account = Database.findAccount(connection);
                if(account == null){
                    return;
                }

                if(account != null){
                    gameManager.handlePackets(connection, object);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                Account account = Database.findAccount(connection);
                if(account != null){
                    gameManager.handlePlayerLeave(account);
                    Database.logOff(connection);
                }
            }

        });
    }

    public static void main(String[] args) {
        new ServerLauncher();
    }
}
