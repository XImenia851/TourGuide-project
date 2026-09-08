package tourGuide.service;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class TestRewardsWebClient {

    @Test
    public void getAttractionRewardPoints() {
        RewardsWebClient client = new RewardsWebClient();
        int points = client.getAttractionRewardPoints(UUID.randomUUID(), UUID.randomUUID());

        assertTrue(points >= 0);
    }
}