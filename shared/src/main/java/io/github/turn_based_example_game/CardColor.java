package io.github.turn_based_example_game;

public enum CardColor {
    RED,
    GREEN,
    BLUE,
    YELLOW;

    public String id() {
        return name().toLowerCase();
    }
}
