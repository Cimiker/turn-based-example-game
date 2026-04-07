package io.github.turn_based_example_game.server.game;

import java.io.Serializable;
import java.util.List;

public record GameResult(List<String> losers, List<String> winners) implements Serializable{

}
