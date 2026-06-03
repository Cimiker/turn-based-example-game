package io.github.turn_based_example_game;

public enum CardStyle {
    FILLED,
    WHITE;

    public String id() {
        return name().toLowerCase();
    }
}
