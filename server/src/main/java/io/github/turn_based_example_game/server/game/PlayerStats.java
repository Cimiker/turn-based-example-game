package io.github.turn_based_example_game.server.game;

public class PlayerStats {
    private Integer wins;
    private Integer losses;
    private String username;

    public String getUsername() {
        return username;
    }

    public PlayerStats(Integer wins, Integer losses, String username) {
        this.wins = wins;
        this.losses = losses;
        this.username = username;
    }
    public Integer getWins() {
        return wins;
    }
    public void setWins(Integer wins) {
        this.wins = wins;
    }
    public Integer getLosses() {
        return losses;
    }
    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public void addToStat(Integer amount) {
        if (amount > 0) {
            wins += amount;
        }else {
            losses -= amount;
        }
    }



}
