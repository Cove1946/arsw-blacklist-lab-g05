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

/**
 * Laboratory implementation: students must complete this class using Java 21 virtual threads.
 */
public final class VirtualThreadBlackListSearch implements BlackListSearch {
    private final List<BlackListProvider> providers;

    public VirtualThreadBlackListSearch(List<BlackListProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    @Override
    public SearchResult search(String ipAddress, int alarmThreshold) {
        Objects.requireNonNull(ipAddress, "ipAddress");
        if (alarmThreshold <= 0) {
            throw new IllegalArgumentException("alarmThreshold must be greater than zero");
        }

        long startedAt = System.nanoTime();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Integer>> tasks = new ArrayList<>(providers.size());
            for (BlackListProvider provider : providers) {
                tasks.add(() -> provider.isBlacklisted(ipAddress) ? provider.id() : null);
            }

            List<Integer> matches = new ArrayList<>();
            int consulted = 0;
            try {
                for (Future<Integer> future : executor.invokeAll(tasks)) {
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
        }
    }
}
