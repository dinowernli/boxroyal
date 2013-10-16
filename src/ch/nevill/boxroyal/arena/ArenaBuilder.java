package ch.nevill.boxroyal.arena;

import ch.nevill.boxroyal.proto.MatchConfig;
import ch.nevill.boxroyal.proto.MatchState;

public interface ArenaBuilder {
  public MatchState build(MatchConfig config);
}
