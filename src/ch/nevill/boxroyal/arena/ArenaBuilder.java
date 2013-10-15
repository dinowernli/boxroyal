package ch.nevill.boxroyal.arena;

import ch.nevill.boxroyal.proto.GameState;

public interface ArenaBuilder {
  public void build(GameState.Builder stateBuilder);
}
