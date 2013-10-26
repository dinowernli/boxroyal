package ch.nevill.boxroyal.server;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ch.nevill.boxroyal.arena.ArenaBuilder;
import ch.nevill.boxroyal.arena.RandomArenaBuilder;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFutureTask;

public class GlobalMatchRunner implements Runnable {

  private static final int MAX_CONCURRENT_MATCHES = 10;
  private static final int WORK_CHECK_INTERVAL = 100;
  private final MatchScheduler rootScheduler;
  private final ThreadPoolExecutor matchExecutor;
  private int nextMatchId = 1;

  public GlobalMatchRunner(MatchScheduler rootScheduler) {
    this.rootScheduler = rootScheduler;
    this.matchExecutor =
        new ThreadPoolExecutor(MAX_CONCURRENT_MATCHES, MAX_CONCURRENT_MATCHES,
            WORK_CHECK_INTERVAL * 3 / 2, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
  }

  @Override
  public void run() {
    while (true) {
      if (matchExecutor.getQueue().size() == 0) {
        Optional<ListenableFutureTask<MatchSimulator>> nextMatch = rootScheduler.getNextMatch(nextMatchId);
        if (nextMatch.isPresent()) {
          ++nextMatchId;
          matchExecutor.submit(nextMatch.get());
          continue;
        }
      }

      try {
        Thread.sleep(WORK_CHECK_INTERVAL);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public static void main(String[] args) {
    ArenaBuilder arenaBuilder = new RandomArenaBuilder(20, 20);
    MatchBuilder matchBuilder = new MatchBuilder(arenaBuilder);
    ImmediateMatchScheduler lobby = new ImmediateMatchScheduler(matchBuilder);

    MatchScheduler scheduler = lobby;
    GlobalMatchRunner matchRunner = new GlobalMatchRunner(scheduler);

    PlayerEntry playerEntry = lobby;
    ClientConnectionListenerService service = new ClientConnectionListenerService(playerEntry);
    service.startAsync().awaitRunning();

    matchRunner.run();
  }

}
