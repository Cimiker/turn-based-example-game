package io.github.turn_based_example_game.server.game;

import io.github.turn_based_example_game.server.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Game {
    private final GameManager gameManager;
    private List<Player> players;
    private final ScheduledExecutorService updateSCheduler;
    private final ScheduledExecutorService simulationScheduler;
    private Integer winner;
    private boolean running;
    private static final double TIMESTEP = 0.016;

    public Game(List<Player> players, GameManager gameManager) {
        this.players = new ArrayList<>(players);
        this.running = true;
        this.gameManager = gameManager;
        this.updateSCheduler = Executors.newSingleThreadScheduledExecutor();
        this.simulationScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public Player getWinner(){
        return players.get(winner);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void simulate() {
        if(this.winner == null){

        }
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

    public void update() {
        for (Player player : players) {
            //Network.GameStateUpdate update = new Network.GameStateUpdate(units.toArray(new Unit[0]));
            //player.sendPacket(update);
        }
    }

    public void start() {
        System.out.println("START");


        update();
        updateSCheduler.scheduleAtFixedRate(this::update, 0, 2000, TimeUnit.MILLISECONDS);
        simulationScheduler.scheduleAtFixedRate(this::simulate, 0, (long) (TIMESTEP * 1000), TimeUnit.MILLISECONDS);
    }

    public void handleMove(Player player) {

    }

    public void dispose() {
        updateSCheduler.shutdown();
        simulationScheduler.shutdown();
    }
}
