package ch.nevill.boxroyal.server;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.nevill.boxroyal.proto.Box;
import ch.nevill.boxroyal.proto.Bullet;
import ch.nevill.boxroyal.proto.Direction;
import ch.nevill.boxroyal.proto.GameLog;
import ch.nevill.boxroyal.proto.GameState;
import ch.nevill.boxroyal.proto.Operation;
import ch.nevill.boxroyal.proto.OperationError;
import ch.nevill.boxroyal.proto.Point;
import ch.nevill.boxroyal.proto.Point.Builder;
import ch.nevill.boxroyal.proto.Round;
import ch.nevill.boxroyal.proto.Size;
import ch.nevill.boxroyal.proto.Soldier;
import ch.nevill.boxroyal.proto.SoldierOrBuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

public class Match implements Runnable {
  
  private static final Log log = LogFactory.getLog(Match.class);
  
  private static final int MAX_ROUNDS = 200;

  private class OperationException extends Exception {
    private static final long serialVersionUID = -2405849614106891603L;
    private OperationError code;

    public OperationException(OperationError code) {
      this.code = code;
    }
    
    public OperationError getCode() {
      return code;
    }
  }
  
  private static Point applyDirection(Point point, Direction direction) {
    Builder builder = point.toBuilder();
    switch (direction.getNumber()) {
      case Direction.NORTH_VALUE:
        builder.setY(point.getY() + 1);
        break;
      case Direction.EAST_VALUE:
        builder.setX(point.getX() + 1);
        break;
      case Direction.SOUTH_VALUE:
        builder.setY(point.getY() - 1);
        break;
      case Direction.WEST_VALUE:
        builder.setX(point.getX() - 1);
        break;
      default:
        throw new IllegalArgumentException();
    }
    return builder.build();
  }
  
  private static boolean pointInArea(Point point, Size area) {
    return point.getX() >= 0
        && point.getY() >= 0
        && point.getX() < area.getWidth()
        && point.getY() < area.getHeight();
  }
  
  private static boolean pointInPath(Point start, Direction direction, Point target) {
    if (start.getX() != target.getX() && start.getY() != target.getY()) {
      return false;
    }
    if (start.getX() == target.getX() && start.getY() == target.getY()) {
      return true;
    }
    
    switch (direction.getNumber()) {
      case Direction.NORTH_VALUE:
        return target.getY() > start.getY();
      case Direction.EAST_VALUE:
        return target.getX() > start.getX();
      case Direction.SOUTH_VALUE:
        return target.getY() < start.getY();
      case Direction.WEST_VALUE:
        return target.getX() < start.getX();
      default:
        throw new IllegalArgumentException();
    }
  }
  
  private Optional<Soldier.Builder> getSoldierById(int soldierId) {
    return Optional.<Soldier.Builder>absent();
  }
  
  private GameState.Builder simulationState;
  private GameLog.Builder gameLog;
  private int roundId = 0;
  private final Client player1;
  private final Client player2;
  private final int matchId;
  
  public Match(Client player1, Client player2, int matchId) {
    this.player1 = player1;
    this.player2 = player2;
    this.matchId = matchId;
  }

  class SimulationStep {

    private final GameState entryState;

    public SimulationStep() {
      this.entryState = simulationState.build();
    }

    private Box getBoxAt(Point p) {
      return entryState.getBoxList().get(p.getX() * entryState.getSize().getWidth() * p.getY());
    }

    private void applyOperation(int playerId, Operation operation) throws OperationException {
      if (operation.hasMove() == operation.hasShoot()) {
        throw new OperationException(OperationError.INVALID_FIELD);
      }

      if (operation.hasShoot()) {
        if (!operation.getShoot().hasSoldierId()) {
          throw new OperationException(OperationError.INVALID_FIELD);
        }
        
        Optional<Soldier.Builder> soldier = getSoldierById(operation.getShoot().getSoldierId());
        if (!soldier.isPresent()) {
          throw new OperationException(OperationError.INVALID_ID);
        }
        if (soldier.get().getPlayerId() != playerId) {
          throw new OperationException(OperationError.WRONG_PLAYER);
        }
        
        simulationState.addBulletBuilder()
            .setPosition(soldier.get().getPosition())
            .setDirection(operation.getShoot().getDirection());
      }

      if (operation.hasMove()) {
        if (!operation.getMove().hasSoldierId()) {
          throw new OperationException(OperationError.INVALID_FIELD);
        }
        
        Optional<Soldier.Builder> soldier = getSoldierById(operation.getMove().getSoldierId());
        if (!soldier.isPresent()) {
          throw new OperationException(OperationError.INVALID_ID);
        }
        if (soldier.get().getPlayerId() != playerId) {
          throw new OperationException(OperationError.WRONG_PLAYER);
        }
        
        Point dest = applyDirection(
            soldier.get().getPosition(),
            operation.getMove().getDirection());
        if (!pointInArea(dest, simulationState.getSize())) {
          throw new OperationException(OperationError.INVALID_MOVEMENT);
        }

        Box destBox = getBoxAt(dest);
        if (destBox.getBlocking()) {
          throw new OperationException(OperationError.INVALID_MOVEMENT);
        }
        
        soldier.get().setPosition(dest);
      }
    }
    
    private void runPreStep() {
      simulationState.clearBullet();
    }
    
    private void runPlayerOperation(int playerId, Operation operation) {
      OperationError error = OperationError.NONE;
      Round.Builder round = gameLog.addRoundBuilder();
      round.setRoundId(roundId);
      try {
        applyOperation(playerId, operation);
      } catch (OperationException exc) {
        error = exc.getCode();
      }
      round.addOperationBuilder()
          .setOperation(operation)
          .setError(error)
          .setPlayerId(playerId);
    }
    
    private void runPostStep() {
      List<Bullet> oldBullets = entryState.getBulletList();
      for (final Bullet bullet : oldBullets) {
        
        Iterable<Soldier.Builder> soldiersInPath
            = Iterables.filter(simulationState.getSoldierBuilderList(),
                               new Predicate<Soldier.Builder>() {
          @Override
          public boolean apply(Soldier.Builder soldier) {
            return soldier.getPlayerId() != bullet.getOwnerId()
                && pointInPath(bullet.getPosition(), bullet.getDirection(), soldier.getPosition());
          }
        });
        
        Ordering<SoldierOrBuilder> proximityOrdering
            = Ordering.natural().onResultOf(new Function<SoldierOrBuilder, Integer>() {
          @Override
          public Integer apply(SoldierOrBuilder soldier) {
            return Math.abs(soldier.getPosition().getX() - bullet.getPosition().getX())
                + Math.abs(soldier.getPosition().getY() - bullet.getPosition().getY());
          }
        });
        
        Soldier.Builder target;
        try {
          target = proximityOrdering.min(soldiersInPath);
        } catch (NoSuchElementException e) {
          // bullet missed
          continue;
        }
        
        // TODO: "kill" target
        log.info(String.format(
            "Match %d:%d: Soldier %s died.", matchId, roundId, target.getSoldierId()));
      }
    }

  }

  public void run() {
    // TODO: setup initial state
    
    gameLog.setStartState(simulationState.build());
    try {
      player1.transmitState(gameLog.getStartState());
    } catch (IOException e) {
      log.error(String.format("Match %d: Error transmitting initial state to player 1", matchId), e);
    }
    try {
      player2.transmitState(gameLog.getStartState());
    } catch (IOException e) {
      log.error(String.format("Match %d: Error transmitting initial state to player 2", matchId), e);
    }
    
    while (roundId < MAX_ROUNDS) {
      SimulationStep step = new SimulationStep();
      step.runPreStep();
      
      try {
        for (Operation operation : player1.receiveOperations()) {
          step.runPlayerOperation(1, operation);
        }
      } catch (IOException e) {
        log.warn(String.format("Match %d:%d: Error receiving data from player 1", matchId, roundId), e);
      }
      try {
        for (Operation operation : player2.receiveOperations()) {
          step.runPlayerOperation(2, operation);
        }
      } catch (IOException e) {
        log.warn(String.format("Match %d:%d: Error receiving data from player 2", matchId, roundId), e);
      }
      
      step.runPostStep();
      
      GameState roundEnd = simulationState.build();
      try {
        player1.transmitState(roundEnd);
      } catch (IOException e) {
        log.warn(String.format("Match %d:%d: Error transmitting data to player 1", matchId, roundId), e);
      }
      try {
        player2.transmitState(roundEnd);
      } catch (IOException e) {
        log.warn(String.format("Match %d:%d: Error transmitting data to player 2", matchId, roundId), e);
      }
    }
  }
  
}
