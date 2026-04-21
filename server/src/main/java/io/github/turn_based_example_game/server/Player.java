package io.github.turn_based_example_game.server;

public abstract class Player {

    public abstract void sendPacket(Object object);

    public abstract void setInGame(boolean inGame);

    public String getUsername(){
        return "Bot";
    }
}
