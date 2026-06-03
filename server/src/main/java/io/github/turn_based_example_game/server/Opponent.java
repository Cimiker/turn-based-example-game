package io.github.turn_based_example_game.server;

import io.github.turn_based_example_game.Card;
import io.github.turn_based_example_game.CardColor;
import io.github.turn_based_example_game.CardSymbol;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Opponent {
    private static final long ACTION_DELAY_MS = 1000L;
    private static final CardColor[] CARD_COLORS = CardColor.values();

    private final Random random;

    public Opponent() {
        this(new Random());
    }

    Opponent(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public Decision chooseAction(Card topPlayPileCard, List<Card> handCards) {
        pauseBeforeAction();

        if (handCards == null || handCards.isEmpty()) {
            return Decision.draw();
        }

        Decision decision = findWildDrawFour(handCards);
        if (decision != null) {
            return decision;
        }

        decision = findMatchingSymbol(handCards, topPlayPileCard.symbol());
        if (decision != null) {
            return decision;
        }

        if (topPlayPileCard.color() != null) {
            decision = findMatchingColor(handCards, topPlayPileCard.color());
            if (decision != null) {
                return decision;
            }
        }

        decision = findWildChangeColor(handCards);
        if (decision != null) {
            return decision;
        }

        return Decision.draw();
    }

    private Decision findWildDrawFour(List<Card> handCards) {
        for (int i = 0; i < handCards.size(); i++) {
            Card card = handCards.get(i);
            if (card.symbol() == CardSymbol.CHANGE_COLOR_PLUS_4) {
                return Decision.play(i, card.withColor(randomColor()));
            }
        }
        return null;
    }

    private Decision findMatchingSymbol(List<Card> handCards, CardSymbol topSymbol) {
        for (int i = 0; i < handCards.size(); i++) {
            Card card = handCards.get(i);
            if (card.symbol() == topSymbol) {
                return Decision.play(i, resolvePlayedCard(card));
            }
        }
        return null;
    }

    private Decision findMatchingColor(List<Card> handCards, CardColor topColor) {
        for (int i = 0; i < handCards.size(); i++) {
            Card card = handCards.get(i);
            if (card.color() == topColor) {
                return Decision.play(i, resolvePlayedCard(card));
            }
        }
        return null;
    }

    private Decision findWildChangeColor(List<Card> handCards) {
        for (int i = 0; i < handCards.size(); i++) {
            Card card = handCards.get(i);
            if (card.symbol() == CardSymbol.CHANGE_COLOR) {
                return Decision.play(i, card.withColor(randomColor()));
            }
        }
        return null;
    }

    private CardColor randomColor() {
        return CARD_COLORS[random.nextInt(CARD_COLORS.length)];
    }

    private Card resolvePlayedCard(Card card) {
        if (card.isWild()) {
            return card.withColor(randomColor());
        }
        return card;
    }

    private void pauseBeforeAction() {
        try {
            Thread.sleep(ACTION_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public static final class Decision {
        public final Action action;
        public final int handIndex;
        public final Card playedCard;

        private Decision(Action action, int handIndex, Card playedCard) {
            this.action = action;
            this.handIndex = handIndex;
            this.playedCard = playedCard;
        }

        public static Decision play(int handIndex, Card playedCard) {
            return new Decision(Action.PLAY, handIndex, playedCard);
        }

        public static Decision draw() {
            return new Decision(Action.DRAW, -1, null);
        }
    }

    public enum Action {
        PLAY,
        DRAW
    }
}
