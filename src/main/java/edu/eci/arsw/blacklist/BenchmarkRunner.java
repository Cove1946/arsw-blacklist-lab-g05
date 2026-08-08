package edu.eci.arsw.blacklist;

import java.util.List;
import java.util.Locale;


public final class BenchmarkRunner {

    private static final int PROVIDER_COUNT = 100;
    private static final int ALARM_THRESHOLD = 5;

    private static final String USAGE =
            "Usage: <strategy> <ip> <simulateIo> <warmups> <measuredRuns> [poolSize]";

    private BenchmarkRunner() {
    }

    private enum Strategy {
        SEQUENTIAL, FIXED, VIRTUAL
    }

    private record Config(
            Strategy strategy,
            String ipAddress,
            boolean simulateIo,
            int warmups,
            int measuredRuns,
            Integer poolSize) {
    }

    public static void main(String[] args) {
        Config config;
        try {
            config = parseArgs(args);
        } catch (IllegalArgumentException ex) {
            System.err.println("Invalid arguments: " + ex.getMessage());
            System.err.println();
            System.err.print(USAGE);
            System.exit(1);
            return;
        }

        try {
            run(config);
        } catch (IllegalStateException ex) {
            System.err.println("Benchmark aborted: " + ex.getMessage());
            System.exit(2);
        }
    }

    private static Config parseArgs(String[] args) {
        if (args.length != 5 && args.length != 6) {
            throw new IllegalArgumentException(
                    "expected 5 or 6 arguments, got " + args.length);
        }

        Strategy strategy;
        try {
            strategy = Strategy.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "strategy must be one of SEQUENTIAL, FIXED, VIRTUAL, got '" + args[0] + "'");
        }

        String ipAddress = args[1];
        if (ipAddress.isBlank()) {
            throw new IllegalArgumentException("ipAddress must not be blank");
        }

        boolean simulateIo = parseBoolean(args[2], "simulateIo");
        int warmups = parseNonNegativeInt(args[3], "warmups");
        int measuredRuns = parsePositiveInt(args[4], "measuredRuns");

        Integer poolSize = null;
        if (strategy == Strategy.FIXED) {
            if (args.length != 6) {
                throw new IllegalArgumentException("poolSize is required for strategy FIXED");
            }
            poolSize = parsePositiveInt(args[5], "poolSize");
        } else if (args.length == 6) {
            throw new IllegalArgumentException(
                    "poolSize must only be provided for strategy FIXED");
        }

        return new Config(strategy, ipAddress, simulateIo, warmups, measuredRuns, poolSize);
    }

    private static boolean parseBoolean(String raw, String fieldName) {
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        throw new IllegalArgumentException(fieldName + " must be 'true' or 'false', got '" + raw + "'");
    }

    private static int parseNonNegativeInt(String raw, String fieldName) {
        int value = parseInt(raw, fieldName);
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be >= 0, got " + value);
        }
        return value;
    }

    private static int parsePositiveInt(String raw, String fieldName) {
        int value = parseInt(raw, fieldName);
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be > 0, got " + value);
        }
        return value;
    }

    private static int parseInt(String raw, String fieldName) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be an integer, got '" + raw + "'");
        }
    }

    private static void run(Config config) {
        List<BlackListProvider> providers = ProviderFactory.create(PROVIDER_COUNT, config.simulateIo());
        BlackListSearch search = createSearch(config, providers);


        List<BlackListProvider> referenceProviders = ProviderFactory.create(PROVIDER_COUNT, false);
        SearchResult reference = new SequentialBlackListSearch(referenceProviders)
                .search(config.ipAddress(), ALARM_THRESHOLD);

        printConfiguration(config);

        for (int i = 1; i <= config.warmups(); i++) {
            SearchResult result = search.search(config.ipAddress(), ALARM_THRESHOLD);
            verifyResult(result, reference, "warm-up " + i);
        }

        double[] elapsedMs = new double[config.measuredRuns()];
        int matches = -1;
        int consulted = -1;

        System.out.println();
        System.out.println("=== Measured runs ===");
        for (int i = 1; i <= config.measuredRuns(); i++) {
            SearchResult result = search.search(config.ipAddress(), ALARM_THRESHOLD);
            verifyResult(result, reference, "measured run " + i);

            elapsedMs[i - 1] = result.elapsed().toNanos() / 1_000_000.0;
            matches = result.matchingProviderIds().size();
            consulted = result.consultedProviders();

            System.out.printf(
                    "Run %d: elapsed=%.3f ms matches=%d consulted=%d%n",
                    i, elapsedMs[i - 1], matches, consulted);
        }

        double min = elapsedMs[0];
        double max = elapsedMs[0];
        double sum = 0.0;
        for (double value : elapsedMs) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }
        double avg = sum / elapsedMs.length;

        System.out.println();
        System.out.println("=== Summary ===");
        System.out.printf("Min elapsed: %.3f ms%n", min);
        System.out.printf("Max elapsed: %.3f ms%n", max);
        System.out.printf("Avg elapsed: %.3f ms%n", avg);

        printCsv(config, elapsedMs, matches, consulted);
    }

    private static BlackListSearch createSearch(Config config, List<BlackListProvider> providers) {
        return switch (config.strategy()) {
            case SEQUENTIAL -> new SequentialBlackListSearch(providers);
            case FIXED -> new FixedPoolBlackListSearch(providers, config.poolSize());
            case VIRTUAL -> new VirtualThreadBlackListSearch(providers);
        };
    }

    private static void verifyResult(SearchResult result, SearchResult reference, String label) {
        if (!result.matchingProviderIds().equals(reference.matchingProviderIds())) {
            throw new IllegalStateException(
                    label + ": matching provider ids " + result.matchingProviderIds()
                            + " do not match expected " + reference.matchingProviderIds());
        }
        if (result.consultedProviders() != reference.consultedProviders()) {
            throw new IllegalStateException(
                    label + ": consulted " + result.consultedProviders()
                            + " providers, expected " + reference.consultedProviders());
        }
    }

    private static void printConfiguration(Config config) {
        System.out.println("=== Benchmark configuration ===");
        System.out.printf("Strategy: %s%n", config.strategy());
        System.out.printf("IP address: %s%n", config.ipAddress());
        System.out.printf("Simulate I/O: %s%n", config.simulateIo());
        System.out.printf("Warm-up runs: %d%n", config.warmups());
        System.out.printf("Measured runs: %d%n", config.measuredRuns());
        System.out.printf("Pool size: %s%n", config.poolSize() != null ? config.poolSize() : "-");
        System.out.printf("Provider count: %d%n", PROVIDER_COUNT);
        System.out.printf("Alarm threshold: %d%n", ALARM_THRESHOLD);
    }

    private static void printCsv(Config config, double[] elapsedMs, int matches, int consulted) {
        String scenario = config.simulateIo() ? "IO" : "NO_IO";
        String strategy = config.strategy().name();
        String poolSize = config.poolSize() != null ? String.valueOf(config.poolSize()) : "-";

        System.out.println();
        System.out.println("=== CSV (copy into results/results.csv) ===");
        System.out.println("scenario,strategy,pool_size,run,elapsed_ms,matches,consulted_providers");
        for (int i = 0; i < elapsedMs.length; i++) {
            System.out.printf(
                    Locale.ROOT,
                    "%s,%s,%s,%d,%.3f,%d,%d%n",
                    scenario, strategy, poolSize, i + 1, elapsedMs[i], matches, consulted);
        }
    }
}
