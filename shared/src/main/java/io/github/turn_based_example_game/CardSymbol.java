package io.github.turn_based_example_game;

public enum CardSymbol {
    NUM_0("0"),
    NUM_1("1"),
    NUM_2("2"),
    NUM_3("3"),
    NUM_4("4"),
    NUM_5("5"),
    NUM_6("6"),
    NUM_7("7"),
    NUM_8("8"),
    NUM_9("9"),
    SKIP("skip"),
    SWITCH_ORDER("switch_order"),
    PLUS_2("plus_2"),
    CHANGE_COLOR("change_color"),
    CHANGE_COLOR_PLUS_4("change_color_plus_4");

    private final String id;

    CardSymbol(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isWild() {
        return this == CHANGE_COLOR || this == CHANGE_COLOR_PLUS_4;
    }

    public boolean isNumbered() {
        return ordinal() <= NUM_9.ordinal();
    }

    public static CardSymbol numbered(int number) {
        return switch (number) {
            case 0 -> NUM_0;
            case 1 -> NUM_1;
            case 2 -> NUM_2;
            case 3 -> NUM_3;
            case 4 -> NUM_4;
            case 5 -> NUM_5;
            case 6 -> NUM_6;
            case 7 -> NUM_7;
            case 8 -> NUM_8;
            case 9 -> NUM_9;
            default -> throw new IllegalArgumentException("Unsupported card number: " + number);
        };
    }
}
