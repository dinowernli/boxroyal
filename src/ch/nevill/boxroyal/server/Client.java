package ch.nevill.boxroyal.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import ch.nevill.boxroyal.proto.GameState;
import ch.nevill.boxroyal.proto.Operation;
import ch.nevill.boxroyal.proto.Turn;

class Client {

  private Socket socket;
  private InputStream receiveStream;
  private OutputStream transmitStream;

  public Client(Socket s) throws IOException {
    this.socket = s;
    this.receiveStream = socket.getInputStream();
    this.transmitStream = socket.getOutputStream();
  }
  
  public List<Operation> receiveOperations() throws IOException {
    Turn turn = Turn.parseDelimitedFrom(receiveStream);
    return turn.getOperationList();
  }
  
  public void transmitState(GameState state) throws IOException {
    state.writeDelimitedTo(transmitStream);
  }
  
}