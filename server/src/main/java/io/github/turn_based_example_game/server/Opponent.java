package io.github.turn_based_example_game.server;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Opponent {
    private static final long ACTION_DELAY_MS = 1000L;
    private static final String[] CARD_COLORS = {"red", "green", "blue", "yellow"};
    private static final String CHANGE_COLOR = "change_color";
    private static final String CHANGE_COLOR_PLUS_4 = "change_color_plus_4";

    private final Random random;

    public Opponent() {
        this(new Random());
    }

    Opponent(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public Decision chooseAction(String topPlayPileCardId, List<String> handCardIds) {
        pauseBeforeAction();

        if (handCardIds == null || handCardIds.isEmpty()) {
            return Decision.draw();
        }

        Decision decision = findWildDrawFour(handCardIds);
        if (decision != null) {
            return decision;
        }

        String topSymbol = extractCardSymbol(topPlayPileCardId);
        if (topSymbol != null) {
            decision = findMatchingSymbol(handCardIds, topSymbol);
            if (decision != null) {
                return decision;
            }
        }

        String topColor = extractCardColor(topPlayPileCardId);
        if (topColor != null) {
            decision = findMatchingColor(handCardIds, topColor);
            if (decision != null) {
                return decision;
            }
        }

        decision = findWildChangeColor(handCardIds);
        if (decision != null) {
            return decision;
        }

        return Decision.draw();
    }

    private Decision findWildDrawFour(List<String> handCardIds) {
        for (int i = 0; i < handCardIds.size(); i++) {
            String cardId = handCardIds.get(i);
            if (CHANGE_COLOR_PLUS_4.equals(cardId)) {
                return Decision.play(i, cardId, withRandomColor(cardId));
            }
        }
        return null;
    }

    private Decision findMatchingSymbol(List<String> handCardIds, String topSymbol) {
        for (int i = 0; i < handCardIds.size(); i++) {
            String cardId = handCardIds.get(i);
            if (topSymbol.equals(extractCardSymbol(cardId))) {
                return Decision.play(i, cardId, resolvePlayedCardId(cardId));
            }
        }
        return null;
    }

    private Decision findMatchingColor(List<String> handCardIds, String topColor) {
        for (int i = 0; i < handCardIds.size(); i++) {
            String cardId = handCardIds.get(i);
            if (topColor.equals(extractCardColor(cardId))) {
                return Decision.play(i, cardId, resolvePlayedCardId(cardId));
            }
        }
        return null;
    }

    private Decision findWildChangeColor(List<String> handCardIds) {
        for (int i = 0; i < handCardIds.size(); i++) {
            String cardId = handCardIds.get(i);
            if (CHANGE_COLOR.equals(cardId)) {
                return Decision.play(i, cardId, withRandomColor(cardId));
            }
        }
        return null;
    }

    private String withRandomColor(String wildCardId) {
        return CARD_COLORS[random.nextInt(CARD_COLORS.length)] + "_" + wildCardId;
    }

    private String resolvePlayedCardId(String cardId) {
        if (CHANGE_COLOR.equals(cardId) || CHANGE_COLOR_PLUS_4.equals(cardId)) {
            return withRandomColor(cardId);
        }
        return cardId;
    }

    private void pauseBeforeAction() {
        try {
            Thread.sleep(ACTION_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static String extractCardColor(String cardId) {
        if (cardId == null) {
            return null;
        }

        for (String color : CARD_COLORS) {
            if (cardId.startsWith(color + "_")) {
                return color;
            }
        }
        return null;
    }

    private static String extractCardSymbol(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            return null;
        }
        if (CHANGE_COLOR.equals(cardId) || CHANGE_COLOR_PLUS_4.equals(cardId)) {
            return cardId;
        }

        String[] parts = cardId.split("_");
        if (parts.length < 2) {
            return cardId;
        }

        int startIndex = extractCardColor(cardId) == null ? 0 : 1;
        int endIndex = parts.length;
        if (endIndex > startIndex && ("filled".equals(parts[endIndex - 1]) || "white".equals(parts[endIndex - 1]))) {
            endIndex--;
        }
        if (startIndex >= endIndex) {
            return cardId;
        }

        StringBuilder symbol = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                symbol.append('_');
            }
            symbol.append(parts[i]);
        }
        return symbol.toString();
    }

    public static final class Decision {
        public final Action action;
        public final int handIndex;
        public final String handCardId;
        public final String playPileCardId;

        private Decision(Action action, int handIndex, String handCardId, String playPileCardId) {
            this.action = action;
            this.handIndex = handIndex;
            this.handCardId = handCardId;
            this.playPileCardId = playPileCardId;
        }

        public static Decision play(int handIndex, String handCardId, String playPileCardId) {
            return new Decision(Action.PLAY, handIndex, handCardId, playPileCardId);
        }

        public static Decision draw() {
            return new Decision(Action.DRAW, -1, null, null);
        }
    }

    public enum Action {
        PLAY,
        DRAW
    }
}
