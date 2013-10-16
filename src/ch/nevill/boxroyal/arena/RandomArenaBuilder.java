package ch.nevill.boxroyal.arena;

import java.util.Random;

import ch.nevill.boxroyal.proto.MatchConfig;
import ch.nevill.boxroyal.proto.MatchState;
import ch.nevill.boxroyal.proto.Player;
import ch.nevill.boxroyal.proto.Point;

public class RandomArenaBuilder implements ArenaBuilder {

  private final int width;
  private final int height;
  private final Random random;

  public RandomArenaBuilder(int width, int height) {
    this.width = width;
    this.height = height;
    this.random = new Random();
  }

  @Override
  public MatchState build(MatchConfig config) {
    MatchState.Builder stateBuilder = MatchState.newBuilder();
    stateBuilder.setConfig(config);

    for (int i = 0; i < width*height; ++i) {
      stateBuilder.addBoxBuilder().setBlocking(random.nextDouble() > 0.9);
    }

    for (Player player : config.getPlayerList()) {
      int soldiers = 0;
      while (soldiers < 3) {
        Point p = Point.newBuilder()
            .setX(random.nextInt(width))
            .setY(random.nextInt(height))
            .build();
        if (!stateBuilder.getBox(p.getX() + p.getY() * width).getBlocking()) {
          stateBuilder.addSoldierBuilder().setPlayerId(player.getId()).setPosition(p);
          ++soldiers;
        }
      }
    }

    return stateBuilder.build();
  }

}
