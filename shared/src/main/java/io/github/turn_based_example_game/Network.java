package io.github.turn_based_example_game;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;

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
        kryo.register(LobbySettings.class);
        kryo.register(LobbyPlayer.class);
        kryo.register(LobbyState.class);
        kryo.register(CreateLobbyRequest.class);
        kryo.register(ToggleLobbyReadyRequest.class);
        kryo.register(LeaveLobbyRequest.class);
        kryo.register(StartLobbyGameRequest.class);
        kryo.register(JoinLobbyByCodeRequest.class);
        kryo.register(JoinPublicLobbyRequest.class);
        kryo.register(LobbyOperationResult.class);
        kryo.register(GameTurnActionRequest.class);
        kryo.register(GameTurnActionRequest.ActionType.class);
        kryo.register(GameDuoRequest.class);
        kryo.register(GameDuoEvent.class);
        kryo.register(Card.class, new CardSerializer());
        kryo.register(CardColor.class);
        kryo.register(CardStyle.class);
        kryo.register(CardSymbol.class);
    }

    private static final class CardSerializer extends Serializer<Card> {
        @Override
        public void write(Kryo kryo, Output output, Card card) {
            kryo.writeObjectOrNull(output, card.color(), CardColor.class);
            kryo.writeObject(output, card.symbol());
            kryo.writeObjectOrNull(output, card.style(), CardStyle.class);
        }

        @Override
        public Card read(Kryo kryo, Input input, Class<Card> type) {
            CardColor color = kryo.readObjectOrNull(input, CardColor.class);
            CardSymbol symbol = kryo.readObject(input, CardSymbol.class);
            CardStyle style = kryo.readObjectOrNull(input, CardStyle.class);
            return new Card(color, symbol, style);
        }
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
        public Card topPlayPileCard;
        public String currentTurnUsername;
        public boolean currentPlayerCanDraw = true;
        public boolean turnActionsLocked;
        public boolean showDuoButton;
        public ArrayList<String> playerUsernames = new ArrayList<>();
        public ArrayList<Integer> playerHandCounts = new ArrayList<>();
        public ArrayList<Card> playerHandCards = new ArrayList<>();

        public GameStateUpdate() {
        }
    }

    public static class GameTurnActionRequest {
        public ActionType actionType;
        public int handIndex = -1;
        public CardColor chosenColor;

        public enum ActionType {
            PLAY_CARD,
            DRAW_CARD
        }
    }

    public static class GameDuoRequest {

    }

    public static class GameDuoEvent {
        public String username;
    }

    public static class JoinGameRequest {
        public enum Mode {
            HUMAN, COMPUTER, TEAMS
        }

        public Mode mode;
    }

    public static class GameEnd {
        public String winner;
    }

    public static class LeaveGameRequest {

    }

    public static class LobbySettings {
        public int maxPlayers;
        public String lobbyMode;
        public boolean fillWithBots;
    }

    public static class LobbyPlayer {
        public String username;
        public boolean ready;
        public boolean owner;
        public boolean bot;
    }

    public static class LobbyState {
        public String lobbyId;
        public LobbySettings settings;
        public ArrayList<LobbyPlayer> players = new ArrayList<>();
    }

    public static class CreateLobbyRequest {
        public LobbySettings settings;
    }

    public static class ToggleLobbyReadyRequest {

    }

    public static class LeaveLobbyRequest {

    }

    public static class StartLobbyGameRequest {

    }

    public static class JoinLobbyByCodeRequest {
        public String lobbyId;
    }

    public static class JoinPublicLobbyRequest {

    }

    public static class LobbyOperationResult {
        public boolean success;
        public boolean lobbyClosed;
        public String message;
        public LobbyState lobbyState;
    }
}
