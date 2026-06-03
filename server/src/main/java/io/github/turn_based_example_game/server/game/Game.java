package io.github.turn_based_example_game.server.game;

import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.server.*;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private final GameManager gameManager;
    private final List<Player> players;
    private Integer winner;
    private boolean running;

    public Game(List<Player> players, GameManager gameManager) {
        this.players = new ArrayList<>(players);
        this.running = true;
        this.gameManager = gameManager;
    }

    public Player getWinner(){
        return players.get(winner);
    }

    public List<Player> getPlayers() {
        return players;
    }

    private void end(){
        if(this.running){
            for(Player p : players){
                Network.GameEnd end = new Network.GameEnd();
                end.winner = (players.indexOf(p) + this.winner) % 2 == 0 ? p.getUsername() : players.get(this.winner.intValue()).getUsername();
                p.setInGame(false);
                p.sendPacket(end);
            }
            Database.addResult(new GameResult(
                players.stream().filter(p -> (players.indexOf(p) + this.winner) % 2 != 0).map(p -> p.getUsername()).toList(),
                players.stream().filter(p -> (players.indexOf(p) + this.winner) % 2 == 0).map(p -> p.getUsername()).toList()));
            dispose();
            this.gameManager.gameEnded(this);


        }
        this.running = false;
    }

    public void handlePlayerLeave(Player player) {
        this.winner = (players.indexOf(player) + 1) % 2;
        end();
    }

    public void start() {
    }

    public void dispose() {
    }
}
