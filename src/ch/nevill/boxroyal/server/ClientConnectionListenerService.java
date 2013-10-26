package ch.nevill.boxroyal.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.util.concurrent.AbstractExecutionThreadService;

public class ClientConnectionListenerService extends AbstractExecutionThreadService {

  private static final Log log = LogFactory.getLog(ClientConnectionListenerService.class);

  private static final int SERVICE_PORT = 45678;

  private ServerSocket serverSocket = null;
  private final PlayerEntry playerEntry;

  public ClientConnectionListenerService(PlayerEntry playerEntry) {
    this.playerEntry = playerEntry;
  }

  @Override
  protected void startUp() throws Exception {
    super.startUp();
    serverSocket = new ServerSocket(SERVICE_PORT);
    log.info(String.format("Listening for clients on %s:%d",
        serverSocket.getInetAddress(), serverSocket.getLocalPort()));
  }

  @Override
  protected void run() throws Exception {
    while (true) {
      Socket s = serverSocket.accept();
      Client c = new Client(s);
      playerEntry.addPlayers(Collections.singleton(c));
    }
  }

  @Override
  protected void shutDown() throws Exception {
    super.shutDown();
    serverSocket.close();
    serverSocket = null;
  }
}
