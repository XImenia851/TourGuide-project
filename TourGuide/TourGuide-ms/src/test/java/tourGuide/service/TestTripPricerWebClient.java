package tourGuide.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import tripPricer.Provider;

public class TestTripPricerWebClient {

    @Test
    public void getPrice() {
        TripPricerWebClient client = new TripPricerWebClient();
        List<Provider> providers = client.getPrice("test-server-api-key", UUID.randomUUID(), 2, 1, 5, 100);

        assertNotNull(providers);
        assertTrue(providers.size() > 0);
    }
}