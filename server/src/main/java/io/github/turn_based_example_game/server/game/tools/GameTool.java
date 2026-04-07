package io.github.turn_based_example_game.server.game.tools;

import io.github.turn_based_example_game.server.game.Game;

import java.util.Comparator;
import java.util.List;


public class GameTool {


    public static double distance(double[] a, double[] b){
        return Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2));
    }


}
