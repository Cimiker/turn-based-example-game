package io.github.turn_based_example_game.server;

import com.esotericsoftware.kryonet.Connection;

import io.github.turn_based_example_game.server.Network.JoinGameRequest;
import io.github.turn_based_example_game.server.game.Game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GameManager {
    private Set<Game> games;
    private List<Account> waitList;
    private List<Account> teamsWaitList;

    public GameManager(com.esotericsoftware.kryonet.Server server) {
        games = new HashSet<>();
        waitList = new ArrayList<>();
        teamsWaitList = new ArrayList<>();
    }

    public void gameEnded(Game game) {
        System.out.println("Game ended: " + game + "winner: " + game.getWinner());
        game.dispose();
        games.remove(game);
    }

    public void handlePlayerLeave(Account account) {
        waitList.remove(account);
        account.setInGame(false);
        for(Game game : games){
            if(game.getPlayers().contains(account)){
                game.handlePlayerLeave(account);
                break;
            }
        }
    }

    public void handlePackets(Connection connection, Object object){
        Account account = Database.findAccount(connection);
        if(object instanceof Network.JoinGameRequest){

            Network.JoinGameRequest request = (JoinGameRequest) object;

            if(account.getInGame()) {
                return;
            }
            if(waitList.contains(account)) {
                waitList.remove(account);
            }
            if(teamsWaitList.contains(account)) {
                teamsWaitList.remove(account);
            }

            if(request.mode == Network.JoinGameRequest.Mode.COMPUTER){
                account.setInGame(true);

                //Vello player = new Vello(account.getUnits());
                //List<Player> players = new ArrayList<>(List.of(account, player));
                //Game newGame = new Game(players, this);
                //player.setGame(newGame);
                //games.add(newGame);
                //newGame.start();
            }else if (request.mode == Network.JoinGameRequest.Mode.HUMAN){
                waitList.add(account);
                if(waitList.size() == 2){
                    for(Account acc : waitList){
                        acc.setInGame(true);
                    }
                    List<Player> players = new ArrayList<>(waitList);
                    Game newGame = new Game(players, this);
                    games.add(newGame);
                    newGame.start();
                    waitList.clear();
                }
            }else if (request.mode == Network.JoinGameRequest.Mode.TEAMS) {
                System.out.println("TEAMS");
                teamsWaitList.add(account);
                System.out.println(teamsWaitList.size());
                if(teamsWaitList.size() == 4){
                    System.out.println("GAME ON");
                    for(Account acc : waitList){
                        acc.setInGame(true);
                    }
                    List<Player> players = new ArrayList<>(teamsWaitList);
                    Game newGame = new Game(players, this);
                    games.add(newGame);
                    newGame.start();
                    teamsWaitList.clear();
                }
            }





        };

        if(object instanceof Network.LeaveGameRequest){
            handlePlayerLeave(account);
        }
    }




}
