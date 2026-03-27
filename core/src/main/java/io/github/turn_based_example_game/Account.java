package io.github.turn_based_example_game;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

public class Account implements Serializable {

    private static final String FILE_PATH = "user.db"; // File path for saving/loading account data

    private static Account instance; // Singleton instance
    private String username;
    private String password;

    private transient boolean isAuthenticated = false; // Transient so it's not serialized
    private transient double money; // In-game currency

    private Account() {}

    public static String getUsername(){
        return instance.username;
    }

    public static void setUsername(String username){
        instance.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    // Loads saved account from file, returns true if file existed
    public static boolean load(){
        File file = new File(FILE_PATH);
        if (!file.exists()){
            instance = new Account();
            return false;
        };

        try {
            FileInputStream fis = new FileInputStream(FILE_PATH);
            ObjectInputStream ois = new ObjectInputStream(fis);
            instance = (Account)ois.readObject();
            ois.close();
        }
        catch (IOException | ClassNotFoundException e) {
            instance = new Account();
            e.printStackTrace();
        }
        return true;
    }

    // Authenticates account with server, updates local state on response
    public static void authenticate(Consumer<Boolean> callback){
        new Thread(() -> {
            if (NetworkManager.getClient() == null) {
                System.err.println("NetworkManager is not initialized.");
                callback.accept(false);
                return;
            }

            Network.LoginRequest request = new Network.LoginRequest();
            request.username = instance.username;
            request.password = instance.password;

            Listener networkListener = new Listener() {
                @Override
                public void received(Connection connection, Object object) {
                    if (object instanceof Network.LoginResponse) {
                        Network.LoginResponse response = (Network.LoginResponse) object;
                        instance.isAuthenticated = response.success;
                        NetworkManager.getClient().removeListener(this);
                        save();
                        callback.accept(response.success);
                    }
                }
            };

            NetworkManager.getClient().addListener(networkListener);
            NetworkManager.sendTCP(request);
        }).start();
    }

    // Registers a new user on the server
    public static void register(String username, String password, Consumer<Boolean> callback){
        new Thread(() -> {
            Network.RegisterRequest registerRequest = new Network.RegisterRequest();
            registerRequest.username = username;
            registerRequest.password = password;

            Listener networkListener = new Listener() {
                @Override
                public void received(Connection connection, Object object) {
                    if (object instanceof Network.RegisterResponse) {
                        Network.RegisterResponse response = (Network.RegisterResponse) object;
                        NetworkManager.getClient().removeListener(this);
                        callback.accept(response.success);
                    }
                }
            };

            NetworkManager.getClient().addListener(networkListener);
            NetworkManager.sendTCP(registerRequest);

        }).start();
    }

    public static void setCredentials(String username, String password){
        instance.username = username;
        instance.password = password;
    }

    public static boolean isAuthenticated() {
        return instance.isAuthenticated;
    }

    public static void setAuthenticated(boolean authenticated){
        instance.isAuthenticated = authenticated;
    }

    // Saves account data to file
    public static void save(){
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(instance);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Deletes saved account data and resets state
    public static void delete(){
        File file = new File(FILE_PATH);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("User data deleted successfully.");
            } else {
                System.err.println("Failed to delete user data.");
            }
        }
        NetworkManager.reconnect();
        instance = new Account();
    }
}

