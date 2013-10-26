package ch.nevill.boxroyal.server;

import ch.nevill.boxroyal.arena.ArenaBuilder;
import ch.nevill.boxroyal.proto.MatchConfig;
import ch.nevill.boxroyal.proto.MatchState;
import ch.nevill.boxroyal.proto.Player;

import com.google.common.collect.ImmutableList;

public class MatchBuilder {
  private final ArenaBuilder arenaBuilder;

  public MatchBuilder(ArenaBuilder arenaBuilder) {
    this.arenaBuilder = arenaBuilder;
  }

  protected MatchSimulator build(final int matchId, final ImmutableList<Client> players) {
    MatchConfig matchConfig = MatchConfig.newBuilder()
        .setMatchId(matchId)
        .addPlayer(Player.newBuilder().setId(1))
        .addPlayer(Player.newBuilder().setId(2))
        .build();

    MatchState state = arenaBuilder.build(matchConfig);
    state = state.toBuilder().setConfig(matchConfig).setRound(0).build();
    return new MatchSimulator(players, state);
  }
}
