package ch.nevill.boxroyal.server;

import java.util.List;

import ch.nevill.boxroyal.proto.Box;
import ch.nevill.boxroyal.proto.Bullet;
import ch.nevill.boxroyal.proto.Direction;
import ch.nevill.boxroyal.proto.GameState;
import ch.nevill.boxroyal.proto.Operation;
import ch.nevill.boxroyal.proto.Point;
import ch.nevill.boxroyal.proto.Point.Builder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class GameSimulator {
  
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

  class SimulationState {

    private GameState state;
    private ImmutableMap<Integer, Integer> soldierIndexMap;
    private ImmutableMap<Integer, Bullet> bulletMap;

    public SimulationState(GameState state) {
      this.state = state;
    }

    private Box getBoxAt(Point p) {
      return state.getBoxList().get(p.getX() * state.getSize().getWidth() * p.getY());
    }

    private Optional<GameState> applyOperation(final GameState state, Operation operation) {
      if (operation.hasMove() == operation.hasShoot()) {
        return Optional.<GameState>absent();
      }

      GameState.Builder newState = state.toBuilder();
      if (operation.hasShoot()) {
        int soldierIndex = soldierIndexMap.get(operation.getShoot().getSoldierId());
        newState.addBulletBuilder()
            .setPosition(state.getSoldier(soldierIndex).getPosition())
            .setDirection(operation.getShoot().getDirection());
      }

      if (operation.hasMove()) {
        int soldierIndex = soldierIndexMap.get(operation.getMove().getSoldierId());
        
        Point dest = applyDirection(
            state.getSoldier(soldierIndex).getPosition(),
            operation.getMove().getDirection());
        Box destBox = getBoxAt(dest);
        if (destBox.hasBlocking()) {
          return Optional.<GameState>absent();
        }
        
        newState.getSoldierBuilder(soldierIndex).setPosition(dest);
      }

      return Optional.of(newState.build());
    }

    private GameState operationsStep(final List<Operation> operations) {

      GameState newState = state;
      for (Operation operation : operations) {
        Optional<GameState> result = applyOperation(newState, operation);
        if (result.isPresent()) {
          newState = result.get();
        }
      }

      return newState;
    }

  }

  private GameState simulateStep(final GameState state) {
    SimulationState simulation = new SimulationState(state);
    throw new UnsupportedOperationException();
  }



}
