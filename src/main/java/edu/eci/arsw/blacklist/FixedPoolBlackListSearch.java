package edu.eci.arsw.blacklist;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Laboratory implementation: students must complete this class.
 */
public final class FixedPoolBlackListSearch implements BlackListSearch {
    private final List<BlackListProvider> providers;
    private final int poolSize;

    public FixedPoolBlackListSearch(List<BlackListProvider> providers, int poolSize) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be greater than zero");
        }
        this.poolSize = poolSize;
    }

    @Override
    public SearchResult search(String ipAddress, int alarmThreshold) {

        Objects.requireNonNull(ipAddress, "ipAddress");
        if (alarmThreshold <= 0) {
            throw new IllegalArgumentException("alarmThreshold must be greater than zero");
        }

        long startedAd = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        List<Future<Integer>> futures = new ArrayList<>(providers.size());
        for (BlackListProvider provider : providers) {
            Callable<Integer> task = () -> provider.isBlacklisted(ipAddress) ? provider.id() : null;
            futures.add(executor.submit(task));
        }

        List<Integer> matches = new ArrayList<>();
        int consulted = 0;
        try {
            for (Future<Integer> future : futures) {
                Integer matchedId = future.get();
                consulted++;
                if (matchedId != null) {
                    matches.add(matchedId);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Search was interrupted while waiting for providers", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Provider consultation failed", ex.getCause());
        }

        throw new UnsupportedOperationException(
                "TODO: implement with ExecutorService and a fixed-size thread pool of " + poolSize);
    }
}
