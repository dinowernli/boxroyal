package ch.nevill.boxroyal.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.nevill.boxroyal.proto.Box;
import ch.nevill.boxroyal.proto.Bullet;
import ch.nevill.boxroyal.proto.MatchState;
import ch.nevill.boxroyal.proto.Operation;
import ch.nevill.boxroyal.proto.OperationError;
import ch.nevill.boxroyal.proto.Point;
import ch.nevill.boxroyal.proto.Round;
import ch.nevill.boxroyal.proto.Soldier;
import ch.nevill.boxroyal.proto.Soldier.Builder;
import ch.nevill.boxroyal.proto.SoldierOrBuilder;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

class StepSimulator {
  private static final Log log = LogFactory.getLog(StepSimulator.class);

  private final MatchState entryState;
  private final ImmutableMap<Integer, Builder> soldierMap;
  private final MatchState.Builder matchState;
  private final Round.Builder round;

  public StepSimulator(ImmutableMap<Integer, Soldier.Builder> soldierMap,
                        MatchState.Builder matchState,
                        Round.Builder round) {
    this.soldierMap = soldierMap;
    this.matchState = matchState;
    this.entryState = matchState.build();
    this.round = round;
  }

  private int getRoundId() {
    return round.getRoundId();
  }

  private int getMatchId() {
    return entryState.getConfig().getMatchId();
  }

  private Box getBoxAt(Point p) {
    return entryState.getBoxList().get(p.getX() + entryState.getSize().getWidth() * p.getY());
  }

  private void applyOperation(int playerId, Operation operation) throws OperationException {
    if (operation.hasMove() == operation.hasShoot()) {
      throw new OperationException(OperationError.INVALID_FIELD);
    }

    if (operation.hasShoot()) {
      if (!operation.getShoot().hasSoldierId()) {
        throw new OperationException(OperationError.INVALID_FIELD);
      }

      Soldier.Builder soldier = soldierMap.get(operation.getShoot().getSoldierId());
      if (soldier == null) {
        throw new OperationException(OperationError.INVALID_ID);
      }
      if (soldier.getPlayerId() != playerId) {
        throw new OperationException(OperationError.WRONG_PLAYER);
      }

      this.matchState.addBulletBuilder()
          .setPosition(soldier.getPosition())
          .setDirection(operation.getShoot().getDirection());
    }

    if (operation.hasMove()) {
      if (!operation.getMove().hasSoldierId()) {
        throw new OperationException(OperationError.INVALID_FIELD);
      }

      Soldier.Builder soldier = soldierMap.get(operation.getMove().getSoldierId());
      if (soldier == null) {
        throw new OperationException(OperationError.INVALID_ID);
      }
      if (soldier.getPlayerId() != playerId) {
        throw new OperationException(OperationError.WRONG_PLAYER);
      }

      Point dest = GeometryUtils.applyDirection(soldier.getPosition(), operation.getMove().getDirection());
      if (!GeometryUtils.pointInArea(dest, matchState.getSize())) {
        throw new OperationException(OperationError.INVALID_MOVEMENT);
      }

      Box destBox = getBoxAt(dest);
      if (destBox.getBlocking()) {
        throw new OperationException(OperationError.INVALID_MOVEMENT);
      }

      soldier.setPosition(dest);
    }
  }

  void runPreStep() {
    matchState.clearBullet();
  }

  void runPlayerOperation(int playerId, Operation operation) {
    OperationError error = OperationError.NONE;
    try {
      if (getRoundId() != operation.getRoundId()) {
        error = OperationError.WRONG_ROUND;
      }
      else {
        applyOperation(playerId, operation);
      }
    } catch (OperationException exc) {
      error = exc.getCode();
    }

    round.addOperationBuilder()
        .setOperation(operation)
        .setError(error)
        .setPlayerId(playerId);
  }

  void runPostStep() {
    List<Bullet> oldBullets = entryState.getBulletList();
    for (final Bullet bullet : oldBullets) {

      final Iterable<Soldier.Builder> soldiersInPath =
          Iterables.filter(matchState.getSoldierBuilderList(), new Predicate<Soldier.Builder>() {
            @Override
            public boolean apply(Soldier.Builder soldier) {
              return GeometryUtils.pointInPath(bullet.getPosition(), bullet.getDirection(), soldier.getPosition());
            }
          });

      if (Iterables.isEmpty(soldiersInPath)) {
        continue;
      }

      final Ordering<SoldierOrBuilder> proximityOrdering = Ordering.natural().onResultOf(
          new Function<SoldierOrBuilder, Integer>() {
            @Override
            public Integer apply(SoldierOrBuilder soldier) {
              return Math.abs(soldier.getPosition().getX() - bullet.getPosition().getX())
                  + Math.abs(soldier.getPosition().getY() - bullet.getPosition().getY());
            }
          });

      final List<Soldier.Builder> hitOrderedSoldiers =
          proximityOrdering.sortedCopy(soldiersInPath);
      List<Soldier.Builder> hits = new ArrayList<>();
      for (Soldier.Builder target : hitOrderedSoldiers) {
        if (!target.getPosition().equals(hitOrderedSoldiers.get(0).getPosition())) {
          break;
        }
        hits.add(target);
      }

      for (Soldier.Builder hit : hits) {
        if (hit.getPlayerId() == bullet.getOwnerId()) {
          log.info(String.format("Match %d:%d: Soldier %d blocked bullet from %d.",
              getMatchId(), getRoundId(), hit.getSoldierId(), bullet.getOwnerId()));
        }
        else {
          // TODO: "kill" target
          log.info(String.format("Match %d:%d: Soldier %d killed by %d.",
              getMatchId(), getRoundId(), hit.getSoldierId(), bullet.getOwnerId()));
        }
      }
    }
  }
}