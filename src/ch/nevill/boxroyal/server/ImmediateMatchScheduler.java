package ch.nevill.boxroyal.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;

public class ImmediateMatchScheduler implements MatchScheduler, PlayerEntry {
  private static final int PLAYERS_PER_MATCH = 2;

  private final Queue<Client> readyClients = new ConcurrentLinkedQueue<>();
  private List<Client> nextClients = new ArrayList<>(PLAYERS_PER_MATCH);
  private final MatchBuilder matchBuilder;

  public ImmediateMatchScheduler(MatchBuilder matchBuilder) {
    this.matchBuilder = matchBuilder;
  }

  @Override
  public Optional<ListenableFutureTask<MatchSimulator>> getNextMatch(int matchId) {
    while (nextClients.size() < PLAYERS_PER_MATCH) {
      Client client = readyClients.poll();
      if (client == null) {
        return Optional.absent();
      }
      nextClients.add(client);

      // Cull already disconnected clients to reduce the number of preemptively terminated matches
      for (Iterator<Client> i = nextClients.iterator(); i.hasNext(); ) {
        Client c = i.next();
        if (!c.isConnected()) {
          i.remove();
        }
      }
    }

    Preconditions.checkState(nextClients.size() == PLAYERS_PER_MATCH);
    final ImmutableList<Client> players = ImmutableList.copyOf(nextClients);
    nextClients.clear();
    final MatchSimulator match = matchBuilder.build(matchId, players);

    ListenableFutureTask<MatchSimulator> matchTask = ListenableFutureTask.create(match, match);
    Futures.addCallback(matchTask, new FutureCallback<MatchSimulator>() {
      @Override
      public void onSuccess(MatchSimulator match) {
        addPlayers(players);
      }
      @Override
      public void onFailure(Throwable arg0) {
        addPlayers(players);
      }
    });

    return Optional.of(matchTask);
  }

  protected void addPlayer(Client player) {
    if (player.isConnected()) {
      readyClients.add(player);
    }
  }

  @Override
  public void addPlayers(Iterable<Client> players) {
    for (Client p : players) {
      addPlayer(p);
    }
  }
}
