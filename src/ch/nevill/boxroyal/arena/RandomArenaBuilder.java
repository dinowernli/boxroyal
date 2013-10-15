package ch.nevill.boxroyal.arena;

import java.util.Random;

import ch.nevill.boxroyal.proto.GameState;
import ch.nevill.boxroyal.proto.Player;
import ch.nevill.boxroyal.proto.Point;

public class RandomArenaBuilder implements ArenaBuilder {

  private int width;
  private int height;
  private Random random;

  public RandomArenaBuilder(int width, int height) {
    this.width = width;
    this.height = height;
    this.random = new Random();
  }

  @Override
  public void build(GameState.Builder stateBuilder) {
    for (int i = 0; i < width*height; ++i) {
      stateBuilder.addBoxBuilder().setBlocking(random.nextDouble() > 0.9);
    }
    
    for (Player player : stateBuilder.getPlayerList()) {
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
  }

}
