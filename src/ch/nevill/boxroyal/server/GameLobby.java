package ch.nevill.boxroyal.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class GameLobby implements Runnable {
  
  private BlockingQueue<Client> readyClients = new LinkedBlockingQueue<>();
  private int nextMatchId = 1;
  
  public void run() {
    try {
      while (true) {
        Client player1 = readyClients.take(), player2 = readyClients.take();
        startGame(player1, player2);
      }
    } catch (InterruptedException e) {
      return;
    }
  }
  
  private void startGame(Client player1, Client player2) {
    GameSimulator simulator = new GameSimulator(player1, player2, nextMatchId);
    ++nextMatchId;
    new Thread(simulator).start();
  }
  
  public static void startServer() throws IOException {
    GameLobby lobby = new GameLobby();
    new Thread(lobby).start();
    ServerSocket serverSocket = new ServerSocket(45678);
    while (true) {
      Socket s = serverSocket.accept();
      Client c = new Client(s);
      lobby.readyClients.add(c);
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
