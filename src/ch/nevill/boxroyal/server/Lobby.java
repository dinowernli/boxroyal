package ch.nevill.boxroyal.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import ch.nevill.boxroyal.arena.ArenaBuilder;
import ch.nevill.boxroyal.arena.RandomArenaBuilder;
import ch.nevill.boxroyal.proto.MatchConfig;
import ch.nevill.boxroyal.proto.MatchState;
import ch.nevill.boxroyal.proto.Player;

import com.google.common.collect.ImmutableList;


public class Lobby implements Runnable {

  private final BlockingQueue<Client> readyClients = new LinkedBlockingQueue<>();
  private int nextMatchId = 1;
  private final ArenaBuilder arenaBuilder = new RandomArenaBuilder(20, 20);

  @Override
  public void run() {
    try {
      while (true) {
        Client player1 = null, player2 = null;
        while (player1 == null) {
          player1 = readyClients.take();
          if (!player1.isConnected()) {
            player1 = null;
          }
        }
        while (player2 == null) {
          player2 = readyClients.take();
          if (!player2.isConnected()) {
            player2 = null;
          }
        }
        startGame(player1, player2);
      }
    } catch (InterruptedException e) {
      return;
    }
  }

  private void startGame(final Client player1, final Client player2) {
    MatchConfig matchConfig = MatchConfig.newBuilder()
        .setMatchId(nextMatchId)
        .addPlayer(Player.newBuilder().setId(1))
        .addPlayer(Player.newBuilder().setId(2))
        .build();
    ++nextMatchId;

    MatchState state = arenaBuilder.build(matchConfig);
    state = state.toBuilder().setConfig(matchConfig).setRound(0).build();

    MatchSimulator simulator = new MatchSimulator(
        ImmutableList.of(player1, player2), state, new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            if (player1.isConnected()) {
              readyClients.add(player1);
            }
            if (player2.isConnected()) {
              readyClients.add(player2);
            }
            return null;
          }
        });
    new Thread(simulator).start();
  }

  public static void startServer() throws IOException {
    Lobby lobby = new Lobby();
    new Thread(lobby).start();
    try (ServerSocket serverSocket = new ServerSocket(45678)) {
      while (true) {
        Socket s = serverSocket.accept();
        Client c = new Client(s);
        lobby.readyClients.add(c);
      }
    }
  }

  public static void main(String[] args) {
    try {
      startServer();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
