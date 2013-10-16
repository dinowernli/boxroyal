package ch.nevill.boxroyal.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import ch.nevill.boxroyal.proto.Operation;
import ch.nevill.boxroyal.proto.Turn;
import ch.nevill.boxroyal.proto.View;

class Client {

  private final Socket socket;
  private final InputStream receiveStream;
  private final OutputStream transmitStream;

  public Client(Socket s) throws IOException {
    this.socket = s;
    this.receiveStream = socket.getInputStream();
    this.transmitStream = socket.getOutputStream();
  }

  public List<Operation> receiveOperations() throws IOException {
    Turn turn = Turn.parseDelimitedFrom(receiveStream);
    return turn.getOperationList();
  }

  public void transmitState(View view) throws IOException {
    view.writeDelimitedTo(transmitStream);
  }

  public boolean isConnected() {
    return socket.isConnected();
  }

  public String getName() {
    return Integer.toString(hashCode());
  }

}