package ch.nevill.boxroyal.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.nevill.boxroyal.proto.GameLog;
import ch.nevill.boxroyal.proto.MatchState;
import ch.nevill.boxroyal.proto.Operation;
import ch.nevill.boxroyal.proto.Player;
import ch.nevill.boxroyal.proto.Round;
import ch.nevill.boxroyal.proto.Soldier;
import ch.nevill.boxroyal.proto.View;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MatchSimulator implements Runnable {
  private static final Log log = LogFactory.getLog(MatchSimulator.class);
  private static final int MAX_ROUNDS = 200;

  final MatchState.Builder simulationState;
  GameLog.Builder gameLog;
  private final ImmutableList<MatchClient> players;
  private final Map<Integer, Soldier.Builder> soldierIdMap;
  private final Callable<Void> finishCallable;

  private static class MatchClient {
    public final Client client;
    public final Player player;
    public MatchClient(Client client, Player player) {
      this.client = client;
      this.player = player;
    }
  }

  public MatchSimulator(List<Client> players, MatchState startState, Callable<Void> finishCallable) {
    if (players.size() != startState.getConfig().getPlayerCount()) {
      throw new IllegalArgumentException();
    }
    ImmutableList.Builder<MatchClient> playersBuilder = ImmutableList.builder();
    for (int i = 0; i < players.size(); i++) {
      playersBuilder.add(new MatchClient(players.get(i), startState.getConfig().getPlayer(i)));
    }
    this.players = playersBuilder.build();
    this.simulationState = startState.toBuilder();
    this.soldierIdMap = new HashMap<>();
    for (Soldier.Builder s : this.simulationState.getSoldierBuilderList()) {
      this.soldierIdMap.put(s.getSoldierId(), s);
    }
    this.finishCallable = finishCallable;
  }

  private int getRoundId() {
    return simulationState.getRound();
  }

  private int getMatchId() {
    return simulationState.getConfig().getMatchId();
  }

  private void finished() {
    // TODO: Write gameLog somewhere

    try {
      finishCallable.call();
    } catch (Exception e) {
      log.warn(String.format("Match %d: finishCallable failed", getMatchId()), e);
    }
  }

  @Override
  public void run() {
    gameLog.setStartState(simulationState.build());
    for (MatchClient player : players) {
      try {
        player.client.transmitState(View.newBuilder().setState(gameLog.getStartState()).build());
      } catch (IOException e) {
        log.error(String.format("Match %d: Error transmitting initial state to player %s",
            getRoundId(), player.client.getName()), e);
        finished();
        return;
      }
    }

    for (; getRoundId() < MAX_ROUNDS; simulationState.setRound(getRoundId() + 1)) {
      Round.Builder round = gameLog.addRoundBuilder().setRoundId(getRoundId());
      StepSimulator step = new StepSimulator(
          ImmutableMap.copyOf(soldierIdMap), simulationState, round);
      step.runPreStep();

      for (MatchClient player : players) {
        try {
          for (Operation operation : player.client.receiveOperations()) {
            step.runPlayerOperation(player.player.getId(), operation);
          }
        } catch (IOException e) {
          log.warn(String.format("Match %d:%d: Error receiving turn from player %d",
              getMatchId(), getRoundId(), player.client.getName()), e);
        }
      }

      step.runPostStep();
      MatchState roundEnd = simulationState.build();

      for (MatchClient player : players) {
        try {
          player.client.transmitState(View.newBuilder().setState(roundEnd).build());
        } catch (IOException e) {
          log.warn(String.format("Match %d:%d: Error transmitting result to player %d",
              getMatchId(), getRoundId(), player.client.getName()), e);
        }
      }
    }

    finished();
  }
}
