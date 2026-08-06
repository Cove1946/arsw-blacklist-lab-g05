package edu.eci.arsw.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class FixedPoolBlackListSearchTest {
    private static final String IP_ADDRESS = "202.24.34.55";
    private static final int ALARM_THRESHOLD = 5;
    private static final int PROVIDER_COUNT = 100;

    private final List<BlackListProvider> providers = ProviderFactory.create(PROVIDER_COUNT, false);
    private final SearchResult baseline = new SequentialBlackListSearch(providers).search(IP_ADDRESS, ALARM_THRESHOLD);

    @Test
    void poolOfTwoShouldReturnSameProviderIdsAsSequentialBaseline() {
        SearchResult result = new FixedPoolBlackListSearch(providers, 2).search(IP_ADDRESS, ALARM_THRESHOLD);

        assertEquals(baseline.matchingProviderIds(), result.matchingProviderIds());
    }
}
