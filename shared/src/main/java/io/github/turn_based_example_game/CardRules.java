package io.github.turn_based_example_game;

public final class CardRules {
    private CardRules() {
    }

    public static boolean canPlay(Card topCard, Card playedCard) {
        if (topCard == null || playedCard == null) {
            return false;
        }
        if (playedCard.isWild()) {
            return true;
        }
        if (topCard.symbol() == playedCard.symbol()) {
            return true;
        }
        return topCard.color() != null && topCard.color() == playedCard.color();
    }

    public static Card resolvePlayedCard(Card handCard, CardColor chosenColor) {
        if (handCard == null) {
            return null;
        }
        if (!handCard.isWild()) {
            return handCard;
        }
        return chosenColor == null ? null : handCard.withColor(chosenColor);
    }

    public static int drawPenalty(Card card) {
        if (card == null) {
            return 0;
        }
        return switch (card.symbol()) {
            case PLUS_2 -> 2;
            case CHANGE_COLOR_PLUS_4 -> 4;
            default -> 0;
        };
    }

    public static int turnAdvanceCount(Card card) {
        if (card == null) {
            return 1;
        }
        return card.symbol() == CardSymbol.SKIP ? 2 : 1;
    }

    public static boolean reversesDirection(Card card) {
        return card != null && card.symbol() == CardSymbol.SWITCH_ORDER;
    }
}
