package ch.nevill.boxroyal.server;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFutureTask;

public interface MatchScheduler {
  public Optional<ListenableFutureTask<MatchSimulator>> getNextMatch(int matchId);
}
