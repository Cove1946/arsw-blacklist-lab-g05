package edu.eci.arsw.blacklist;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

        long startedAt = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
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

            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
            return new SearchResult(ipAddress, matches, consulted, elapsed);
        } finally {
            executor.shutdown();
        }
    }
}