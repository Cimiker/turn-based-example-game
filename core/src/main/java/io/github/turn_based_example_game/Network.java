package io.github.turn_based_example_game;

import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {
    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(RegisterRequest.class);
        kryo.register(RegisterResponse.class);
        kryo.register(LoginRequest.class);
        kryo.register(ArrayList.class);
        kryo.register(LoginResponse.class);
        kryo.register(double[].class);
        kryo.register(JoinGameRequest.Mode.class);
        kryo.register(JoinGameRequest.class);
        kryo.register(GameStateUpdate.class);
        kryo.register(GameEnd.class);
        kryo.register(Class.class);
        kryo.register(Class[].class);
        kryo.register(LeaveGameRequest.class);
        kryo.register(PlayerStats.class);
        kryo.register(PlayerStats[].class);
        kryo.register(LeaderboardResponse.class);
        kryo.register(LeaderboardRequest.class);

    }

    public static class RegisterRequest {
        public String username;
        public String password;
    }

    public static class RegisterResponse {
        public boolean success;
        public String message;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public boolean success;
        public String message;
    }

    public static class GameStateUpdate {
        public int playerIndex;

        public GameStateUpdate() {}
    }

    public static class JoinGameRequest {
        public enum Mode {
            HUMAN, COMPUTER, TEAMS
        }
        public Mode mode;
    }

    public static class GameEnd{
        public String winner;
    }

    public static class LeaveGameRequest {

    }

    public static class LeaderboardRequest {

    }

    public static class LeaderboardResponse {
        public PlayerStats[] leaderboard;
    }
}
