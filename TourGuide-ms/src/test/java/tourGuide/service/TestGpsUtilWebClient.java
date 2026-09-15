package tourGuide.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

public class TestGpsUtilWebClient {

    @Test
    public void getAttractions() {
        GpsUtilWebClient client = new GpsUtilWebClient();
        List<Attraction> attractions = client.getAttractions();

        assertNotNull(attractions);
        assertTrue(attractions.size() > 0);
    }

    @Test
    public void getUserLocation() {
        GpsUtilWebClient client = new GpsUtilWebClient();
        VisitedLocation visitedLocation = client.getUserLocation(UUID.randomUUID());

        assertNotNull(visitedLocation);
        assertNotNull(visitedLocation.location);
    }
}