package io.github.turn_based_example_game.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class LobbyManager {
    private static final int LOBBY_ID_LENGTH = 5;
    private static final Random RANDOM = new Random();
    private static final String[] BOT_NAMES = {
        "Martin",
        "Laura",
        "Patrick",
        "Sean",
        "Jack",
        "Ashley",
        "Maria",
        "Lewis",
        "Simon",
        "Sam",
        "Adam",
        "Bruce",
        "James"
    };

    private final GameManager gameManager;
    private final Map<Account, Lobby> lobbiesByAccount = new HashMap<>();
    private final Map<String, Lobby> lobbiesById = new HashMap<>();

    public LobbyManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public synchronized boolean handlePacket(Account account, Object object) {
        if (object instanceof Network.CreateLobbyRequest request) {
            Network.LobbyOperationResult result = createLobby(account, request.settings);
            account.sendPacket(result);
            return true;
        }

        if (object instanceof Network.ToggleLobbyReadyRequest) {
            Network.LobbyOperationResult result = toggleReady(account);
            if (!result.success) {
                account.sendPacket(result);
            }
            return true;
        }

        if (object instanceof Network.LeaveLobbyRequest) {
            leaveLobby(account);
            return true;
        }

        if (object instanceof Network.StartLobbyGameRequest) {
            Network.LobbyOperationResult result = startLobbyGame(account);
            if (!result.success) {
                account.sendPacket(result);
            }
            return true;
        }

        if (object instanceof Network.JoinLobbyByCodeRequest request) {
            Network.LobbyOperationResult result = joinLobbyByCode(account, request.lobbyId);
            account.sendPacket(result);
            return true;
        }

        if (object instanceof Network.JoinPublicLobbyRequest) {
            Network.LobbyOperationResult result = joinRandomPublicLobby(account);
            account.sendPacket(result);
            return true;
        }

        return false;
    }

    public synchronized void handlePlayerDisconnect(Account account) {
        leaveLobby(account);
    }

    private Network.LobbyOperationResult createLobby(Account owner, Network.LobbySettings settings) {
        Network.LobbyOperationResult result = new Network.LobbyOperationResult();
        if (owner.getInGame()) {
            result.success = false;
            result.message = "You cannot create a lobby while already in a game.";
            return result;
        }

        if (settings == null) {
            result.success = false;
            result.message = "Lobby settings are missing.";
            return result;
        }

        leaveLobby(owner);

        Lobby lobby = new Lobby(generateUniqueLobbyId(), copySettings(settings));
        lobby.players.put(owner, createHumanPlayer(owner, true));
        lobbiesByAccount.put(owner, lobby);
        lobbiesById.put(lobby.lobbyId, lobby);

        if (settings.fillWithBots) {
            for (int i = 2; i <= settings.maxPlayers; i++) {
                lobby.bots.add(createBotPlayer(lobby));
            }
        }

        result.success = true;
        result.message = "Lobby created. Code: " + lobby.lobbyId;
        result.lobbyState = toLobbyState(lobby);
        broadcastLobby(lobby);
        return result;
    }

    private Network.LobbyOperationResult toggleReady(Account account) {
        Network.LobbyOperationResult result = new Network.LobbyOperationResult();
        Lobby lobby = lobbiesByAccount.get(account);
        if (lobby == null) {
            result.success = false;
            result.message = "You are not in a lobby.";
            return result;
        }

        Network.LobbyPlayer player = lobby.players.get(account);
        if (player == null) {
            result.success = false;
            result.message = "Lobby player not found.";
            return result;
        }

        player.ready = !player.ready;
        result.success = true;
        result.message = player.ready ? "You are ready." : "You are no longer ready.";
        result.lobbyState = toLobbyState(lobby);
        broadcastLobby(lobby);
        return result;
    }

    private Network.LobbyOperationResult joinLobbyByCode(Account account, String lobbyId) {
        if (lobbyId == null || lobbyId.isBlank()) {
            return failedResult("A lobby couldn't be found");
        }

        Lobby lobby = lobbiesById.get(lobbyId.trim().toUpperCase());
        if (lobby == null) {
            return failedResult("A lobby couldn't be found");
        }

        return joinLobby(account, lobby);
    }

    private Network.LobbyOperationResult joinRandomPublicLobby(Account account) {
        for (Lobby lobby : lobbiesById.values()) {
            if ("Public".equalsIgnoreCase(lobby.settings.lobbyMode) && hasJoinableSlot(lobby)) {
                return joinLobby(account, lobby);
            }
        }

        return failedResult("No public lobby is available right now.");
    }

    private Network.LobbyOperationResult startLobbyGame(Account account) {
        Network.LobbyOperationResult result = new Network.LobbyOperationResult();
        Lobby lobby = lobbiesByAccount.get(account);
        if (lobby == null) {
            result.success = false;
            result.message = "You are not in a lobby.";
            return result;
        }

        Network.LobbyPlayer requester = lobby.players.get(account);
        if (requester == null || !requester.owner) {
            result.success = false;
            result.message = "Only the lobby owner can start the game.";
            result.lobbyState = toLobbyState(lobby);
            return result;
        }

        if (!allPlayersReady(lobby)) {
            result.success = false;
            result.message = "All players must be ready before the game can start.";
            result.lobbyState = toLobbyState(lobby);
            return result;
        }

        ArrayList<Account> humanAccounts = new ArrayList<>(lobby.players.keySet());
        ArrayList<Network.LobbyPlayer> playersInOrder = new ArrayList<>();
        for (Network.LobbyPlayer player : lobby.players.values()) {
            playersInOrder.add(copyPlayer(player));
        }
        for (Network.LobbyPlayer bot : lobby.bots) {
            playersInOrder.add(copyPlayer(bot));
        }
        gameManager.startLobbyGame(humanAccounts, playersInOrder);
        for (Account playerAccount : humanAccounts) {
            lobbiesByAccount.remove(playerAccount);
        }
        lobbiesById.remove(lobby.lobbyId);
        lobby.players.clear();
        lobby.bots.clear();

        result.success = true;
        result.message = "Game starting.";
        return result;
    }

    private void leaveLobby(Account account) {
        Lobby lobby = lobbiesByAccount.remove(account);
        if (lobby == null) {
            return;
        }

        Network.LobbyPlayer leavingPlayer = lobby.players.get(account);
        boolean ownerLeaving = leavingPlayer != null && leavingPlayer.owner;

        lobby.players.remove(account);
        if (ownerLeaving) {
            closeLobby(lobby, "The lobby owner left the lobby.");
            return;
        }

        if (lobby.settings.fillWithBots && lobby.players.size() + lobby.bots.size() < lobby.settings.maxPlayers) {
            lobby.bots.add(createReplacementBot(lobby));
        }

        if (lobby.players.isEmpty()) {
            lobbiesById.remove(lobby.lobbyId);
            return;
        }

        broadcastLobby(lobby);
    }

    private Network.LobbyOperationResult joinLobby(Account account, Lobby lobby) {
        if (account.getInGame()) {
            return failedResult("You cannot join a lobby while already in a game.");
        }

        Lobby existingLobby = lobbiesByAccount.get(account);
        if (existingLobby == lobby) {
            Network.LobbyOperationResult result = new Network.LobbyOperationResult();
            result.success = true;
            result.message = "Joined lobby " + lobby.lobbyId + ".";
            result.lobbyState = toLobbyState(lobby);
            return result;
        }

        leaveLobby(account);

        if (!hasJoinableSlot(lobby)) {
            return failedResult("A lobby couldn't be found");
        }

        if (!lobby.bots.isEmpty()) {
            lobby.bots.remove(lobby.bots.size() - 1);
        }

        lobby.players.put(account, createHumanPlayer(account, false));
        lobbiesByAccount.put(account, lobby);

        Network.LobbyOperationResult result = new Network.LobbyOperationResult();
        result.success = true;
        result.message = "Joined lobby " + lobby.lobbyId + ".";
        result.lobbyState = toLobbyState(lobby);
        broadcastLobby(lobby);
        return result;
    }

    private boolean hasJoinableSlot(Lobby lobby) {
        return lobby.players.size() + lobby.bots.size() < lobby.settings.maxPlayers || !lobby.bots.isEmpty();
    }

    private boolean allPlayersReady(Lobby lobby) {
        for (Network.LobbyPlayer player : lobby.players.values()) {
            if (!player.ready) {
                return false;
            }
        }
        for (Network.LobbyPlayer bot : lobby.bots) {
            if (!bot.ready) {
                return false;
            }
        }
        return true;
    }

    private void broadcastLobby(Lobby lobby) {
        Network.LobbyState state = toLobbyState(lobby);
        for (Account account : lobby.players.keySet()) {
            account.sendPacket(state);
        }
    }

    private Network.LobbyState toLobbyState(Lobby lobby) {
        Network.LobbyState state = new Network.LobbyState();
        state.lobbyId = lobby.lobbyId;
        state.settings = copySettings(lobby.settings);
        for (Network.LobbyPlayer player : lobby.players.values()) {
            state.players.add(copyPlayer(player));
        }
        for (Network.LobbyPlayer bot : lobby.bots) {
            state.players.add(copyPlayer(bot));
        }
        return state;
    }

    private Network.LobbySettings copySettings(Network.LobbySettings source) {
        Network.LobbySettings settings = new Network.LobbySettings();
        int requestedMaxPlayers = source.maxPlayers;
        settings.maxPlayers = Math.max(2, Math.min(4, requestedMaxPlayers == 0 ? 4 : requestedMaxPlayers));
        settings.lobbyMode = source.lobbyMode == null || source.lobbyMode.isBlank() ? "Private" : source.lobbyMode;
        settings.fillWithBots = source.fillWithBots;
        return settings;
    }

    private Network.LobbyPlayer createHumanPlayer(Account account, boolean owner) {
        Network.LobbyPlayer player = new Network.LobbyPlayer();
        player.username = account.getUsername();
        player.ready = false;
        player.owner = owner;
        player.bot = false;
        return player;
    }

    private Network.LobbyPlayer createReplacementBot(Lobby lobby) {
        return createBotPlayer(lobby);
    }

    private Network.LobbyPlayer createBotPlayer(Lobby lobby) {
        ArrayList<String> availableNames = new ArrayList<>();
        for (String botName : BOT_NAMES) {
            if (!containsPlayerName(lobby, botName)) {
                availableNames.add(botName);
            }
        }

        Network.LobbyPlayer bot = new Network.LobbyPlayer();
        if (!availableNames.isEmpty()) {
            bot.username = availableNames.get(RANDOM.nextInt(availableNames.size()));
        } else {
            bot.username = BOT_NAMES[RANDOM.nextInt(BOT_NAMES.length)] + " " + (RANDOM.nextInt(900) + 100);
        }
        bot.ready = true;
        bot.owner = false;
        bot.bot = true;
        return bot;
    }

    private String generateUniqueLobbyId() {
        String candidate;
        do {
            StringBuilder builder = new StringBuilder(LOBBY_ID_LENGTH);
            for (int i = 0; i < LOBBY_ID_LENGTH; i++) {
                builder.append((char) ('A' + RANDOM.nextInt(26)));
            }
            candidate = builder.toString();
        } while (lobbiesById.containsKey(candidate));
        return candidate;
    }

    private Network.LobbyPlayer copyPlayer(Network.LobbyPlayer source) {
        Network.LobbyPlayer player = new Network.LobbyPlayer();
        player.username = source.username;
        player.ready = source.ready;
        player.owner = source.owner;
        player.bot = source.bot;
        return player;
    }

    private Network.LobbyOperationResult failedResult(String message) {
        Network.LobbyOperationResult result = new Network.LobbyOperationResult();
        result.success = false;
        result.message = message;
        return result;
    }

    private void closeLobby(Lobby lobby, String message) {
        lobbiesById.remove(lobby.lobbyId);
        for (Account playerAccount : new ArrayList<>(lobby.players.keySet())) {
            lobbiesByAccount.remove(playerAccount);
            Network.LobbyOperationResult result = new Network.LobbyOperationResult();
            result.success = false;
            result.lobbyClosed = true;
            result.message = message;
            playerAccount.sendPacket(result);
        }
        lobby.players.clear();
        lobby.bots.clear();
    }

    private boolean containsPlayerName(Lobby lobby, String username) {
        for (Network.LobbyPlayer player : lobby.players.values()) {
            if (username.equals(player.username)) {
                return true;
            }
        }
        for (Network.LobbyPlayer bot : lobby.bots) {
            if (username.equals(bot.username)) {
                return true;
            }
        }
        return false;
    }

    private static class Lobby {
        private final String lobbyId;
        private final Network.LobbySettings settings;
        private final LinkedHashMap<Account, Network.LobbyPlayer> players = new LinkedHashMap<>();
        private final ArrayList<Network.LobbyPlayer> bots = new ArrayList<>();

        private Lobby(String lobbyId, Network.LobbySettings settings) {
            this.lobbyId = lobbyId;
            this.settings = settings;
        }
    }
}
