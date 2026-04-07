package io.github.turn_based_example_game.server;

import io.github.turn_based_example_game.server.game.Game;

import java.util.Set;

public abstract class Player {

    public void setGame(Game game){};

    public abstract void sendPacket(Object object);

    public abstract void setInGame(boolean inGame);

    public String getUsername(){
        return "Bot";
    }
}
