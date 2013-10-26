package ch.nevill.boxroyal.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;


public class ImmediateMatchScheduler implements MatchScheduler, PlayerEntry {

  private static final int PLAYERS_PER_MATCH = 2;

  private final BlockingQueue<Client> readyClients = new LinkedBlockingQueue<>();
  private List<Client> nextClients = new ArrayList<>(PLAYERS_PER_MATCH);
  private final MatchBuilder matchBuilder;

  public ImmediateMatchScheduler(MatchBuilder matchBuilder) {
    this.matchBuilder = matchBuilder;
  }

  @Override
  public Optional<ListenableFutureTask<MatchSimulator>> getNextMatch(int matchId) {
    int required = PLAYERS_PER_MATCH - nextClients.size();
    while (required > 0 && !readyClients.isEmpty()) {
      readyClients.drainTo(nextClients, required);

      // Cull already disconnected clients to reduce the number of preemptively terminated matches
      for (Iterator<Client> i = nextClients.iterator(); i.hasNext(); ) {
        Client c = i.next();
        if (!c.isConnected()) {
          i.remove();
        }
      }
    }

    if (nextClients.size() < PLAYERS_PER_MATCH) {
      return Optional.absent();
    }

    final ImmutableList<Client> players =
        ImmutableList.copyOf(Iterables.limit(nextClients, PLAYERS_PER_MATCH));
    final MatchSimulator match = matchBuilder.build(matchId, players);
    nextClients = nextClients.subList(PLAYERS_PER_MATCH, nextClients.size());

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
