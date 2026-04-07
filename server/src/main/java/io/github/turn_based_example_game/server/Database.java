package io.github.turn_based_example_game.server;

import com.esotericsoftware.kryonet.Connection;
import io.github.turn_based_example_game.server.game.GameResult;
import io.github.turn_based_example_game.server.game.PlayerStats;

import java.io.*;
import java.util.*;

public class Database {
    private static final String ACCOUNT_FILE_PATH = "users.db";
    private static final String GAMELOG_FILE_PATH = "gamelog.db";
    private static List<Account> accounts = new LinkedList<>();
    private static List<GameResult> results = new ArrayList<>();
    private static Map<String, PlayerStats> playerStats = new HashMap<>();
    private static final Map<Connection, Account> connections = new HashMap<>(); // Maps connections to usernames


    static {
        load();
    }

    public static void addResult(GameResult result) {
        results.add(result);
        save();
        for (String player : result.losers()) {
            if (player.equals("Bot")) {
                continue;
            }
            if (!playerStats.containsKey(player)) {
                playerStats.put(player, new PlayerStats(0,0, player));
            }
            playerStats.get(player).addToStat(-1);
        }
        for (String player : result.winners()) {
            if (player.equals("Bot")) {
                continue;
            }
            if (!playerStats.containsKey(player)) {
                playerStats.put(player, new PlayerStats(0,0, player));
            }
            playerStats.get(player).addToStat(1);
        }
    }

    public static PlayerStats[] getLeaderboard(){
        return playerStats.values().stream().sorted(Comparator.comparingInt(PlayerStats::getWins).reversed()).limit(10).toArray(PlayerStats[]::new);
    }

    public static Map<Connection, Account> getConnections(){
        return connections;
    }
    public static List<Account> getAccounts() {
        return accounts;
    }

    public static void logOff(Connection connection) {
        connections.remove(connection);
    }

    public static void load() {
        File accountFile = new File(ACCOUNT_FILE_PATH);
        if (accountFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(ACCOUNT_FILE_PATH);
                ObjectInputStream ois = new ObjectInputStream(fis);

                accounts = (List<Account>) ois.readObject();
                ois.close();
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


        File gameLogFile = new File(GAMELOG_FILE_PATH);
        if (gameLogFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(GAMELOG_FILE_PATH);
                ObjectInputStream ois = new ObjectInputStream(fis);

                results = (List<GameResult>) ois.readObject();
                ois.close();
                for (GameResult result : results) {
                    for (String player : result.losers()) {
                        if (player.equals("Bot")) {
                            continue;
                        }
                        if (!playerStats.containsKey(player)) {
                            playerStats.put(player, new PlayerStats(0,0, player));
                        }
                        playerStats.get(player).addToStat(-1);
                    }

                    for (String player : result.winners()) {
                        if (player.equals("Bot")) {
                            continue;
                        }
                        if (!playerStats.containsKey(player)) {
                            playerStats.put(player, new PlayerStats(0,0, player));
                        }
                        playerStats.get(player).addToStat(1);
                    }

                }
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();

            }
        }
    }

    public static synchronized boolean registerUser(String username, String password) {
        if(Account.findByUsername(accounts, username) != null){
            return false;
        }

        accounts.add(new Account(username, password));
        save();
        return true;
    }

    public static Account authenticate(Connection connection, String username, String password) {
        Account account = Account.findByUsername(accounts, username);
        if (account == null || !account.getPassword().equals(password) || account.getConnection() != null){
            return null;
        }
        connections.put(connection, account);
        return account;
    }

    public static Account findAccount(Connection connection){
        return connections.getOrDefault(connection, null);
    }

    public static void save() {
        try {
            FileOutputStream fos = new FileOutputStream(ACCOUNT_FILE_PATH);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(accounts);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream fos = new FileOutputStream(GAMELOG_FILE_PATH);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(results);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
