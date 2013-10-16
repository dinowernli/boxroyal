package ch.nevill.boxroyal.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.nevill.boxroyal.proto.GameLog;
import ch.nevill.boxroyal.proto.GameState;
import ch.nevill.boxroyal.proto.Operation;
import ch.nevill.boxroyal.proto.Player;
import ch.nevill.boxroyal.proto.Round;
import ch.nevill.boxroyal.proto.Soldier;
import ch.nevill.boxroyal.proto.View;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class Match implements Runnable {
  private static final Log log = LogFactory.getLog(Match.class);
  private static final int MAX_ROUNDS = 200;

  final GameState.Builder simulationState;
  GameLog.Builder gameLog;
  int roundId = 0;
  private final ImmutableList<MatchClient> players;
  final int matchId;
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

  public Match(int matchId, List<Client> players, GameState startState, Callable<Void> finishCallable) {
    if (players.size() != startState.getPlayerCount()) {
      throw new IllegalArgumentException();
    }
    ImmutableList.Builder<MatchClient> playersBuilder = ImmutableList.builder();
    for (int i = 0; i < players.size(); i++) {
      playersBuilder.add(new MatchClient(players.get(i), startState.getPlayer(i)));
    }
    this.players = playersBuilder.build();
    this.matchId = matchId;
    this.simulationState = startState.toBuilder();
    this.soldierIdMap = new HashMap<>();
    for (Soldier.Builder s : this.simulationState.getSoldierBuilderList()) {
      this.soldierIdMap.put(s.getSoldierId(), s);
    }
    this.finishCallable = finishCallable;
  }

  private void finished() {
    // TODO: Write gameLog somewhere

    try {
      finishCallable.call();
    } catch (Exception e) {
      log.warn(String.format("Match %d: finishCallable failed", matchId), e);
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
          matchId, player.client.getName()), e);
        finished();
        return;
      }
    }

    for (; roundId < MAX_ROUNDS; ++roundId) {
      Round.Builder round = gameLog.addRoundBuilder().setRoundId(roundId);
      SimulationStep step = new SimulationStep(
          ImmutableMap.copyOf(soldierIdMap), simulationState, round, matchId);
      step.runPreStep();

      for (MatchClient player : players) {
        try {
          for (Operation operation : player.client.receiveOperations()) {
            step.runPlayerOperation(player.player.getId(), operation);
          }
        } catch (IOException e) {
          log.warn(String.format("Match %d:%d: Error receiving turn from player %d",
              matchId, roundId, player.client.getName()), e);
        }
      }

      step.runPostStep();
      GameState roundEnd = simulationState.build();

      for (MatchClient player : players) {
        try {
          player.client.transmitState(View.newBuilder().setState(roundEnd).build());
        } catch (IOException e) {
          log.warn(String.format("Match %d:%d: Error transmitting result to player %d",
              matchId, roundId, player.client.getName()), e);
        }
      }
    }
    finished();
  }
}
