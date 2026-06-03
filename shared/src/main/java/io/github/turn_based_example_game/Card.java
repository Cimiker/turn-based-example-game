package io.github.turn_based_example_game;

import java.util.Objects;

public record Card(CardColor color, CardSymbol symbol, CardStyle style) {
    public Card {
        Objects.requireNonNull(symbol, "symbol");

        if (symbol.isWild()) {
            if (style != null) {
                throw new IllegalArgumentException("Wild cards do not use a style");
            }
        } else {
            Objects.requireNonNull(color, "color");
            Objects.requireNonNull(style, "style");
        }
    }

    public boolean isWild() {
        return symbol.isWild();
    }

    public boolean isResolvedWild() {
        return isWild() && color != null;
    }

    public Card withColor(CardColor chosenColor) {
        if (!isWild()) {
            throw new IllegalStateException("Only wild cards can be recolored");
        }
        return new Card(Objects.requireNonNull(chosenColor, "chosenColor"), symbol, null);
    }

    public String assetId() {
        if (isWild()) {
            return color == null ? symbol.id() : color.id() + "_" + symbol.id();
        }
        return color.id() + "_" + symbol.id() + "_" + style.id();
    }
}
