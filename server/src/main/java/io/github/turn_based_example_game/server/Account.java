package io.github.turn_based_example_game.server;

import com.esotericsoftware.kryonet.Connection;

import java.io.Serializable;
import java.util.*;

public class Account extends Player implements Serializable {
    private final String username;
    private final String password;
    private transient Boolean inGame = false;

    public Connection getConnection() {
        for(Map.Entry<Connection, Account> entry : Database.getConnections().entrySet()){
            if(entry.getValue() == this){
                return entry.getKey();
            }
        }
        return null;
    }

    public static Account getAccount(String username) {
        return findByUsername(Database.getAccounts(), username);
    }

    private final Map<Integer, Integer> cards = new HashMap<>();

    public Account(String username, String password){
        this.username = username;
        this.password = password;
    }

    public static Account findByUsername(List<Account> accounts, String username){
        for(Account account : accounts){
            if(account.getUsername().equals(username)){
                return account;
            }
        }
        return null;
    }

    @Override
    public String getUsername(){
        return this.username;
    }

    public String getPassword(){
        return this.password;
    }

    public Boolean getInGame(){
        return this.inGame != null && this.inGame;
    }

    public void setInGame(boolean state){
        this.inGame = state;
    }

    public void setCardLevel(int cardId, int level){
        this.cards.put(cardId, level);
    }

    public Map<Integer, Integer> getCards(){
        return this.cards;
    }

    @Override
    public void sendPacket(Object object) {
        this.getConnection().sendTCP(object);
    }


}
