package edu.eci.arsw.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Executable specification of the mandatory result contract: every strategy must perform a
 * complete scan and expose the same evidence, regardless of its execution model.
 */
class BlackListSearchContractTest {
    private static final String IP_ADDRESS = "202.24.34.55";
    private static final int ALARM_THRESHOLD = 5;
    private static final int PROVIDER_COUNT = 100;

    static Stream<Arguments> strategies() {
        List<BlackListProvider> providers = ProviderFactory.create(PROVIDER_COUNT, false);
        return Stream.of(
                Arguments.of("SEQUENTIAL", new SequentialBlackListSearch(providers)),
                Arguments.of("FIXED-2", new FixedPoolBlackListSearch(providers, 2)),
                Arguments.of("FIXED-4", new FixedPoolBlackListSearch(providers, 4)),
                Arguments.of("FIXED-8", new FixedPoolBlackListSearch(providers, 8)),
                Arguments.of("VIRTUAL", new VirtualThreadBlackListSearch(providers)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    void shouldConsultEveryProvider(String strategyName, BlackListSearch search) {
        SearchResult result = search.search(IP_ADDRESS, ALARM_THRESHOLD);

        assertEquals(PROVIDER_COUNT, result.consultedProviders());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    void shouldNotReportDuplicatedProviderIds(String strategyName, BlackListSearch search) {
        List<Integer> matches = search.search(IP_ADDRESS, ALARM_THRESHOLD).matchingProviderIds();

        assertEquals(matches.size(), new HashSet<>(matches).size(), () -> "duplicated ids in " + matches);
    }
}
