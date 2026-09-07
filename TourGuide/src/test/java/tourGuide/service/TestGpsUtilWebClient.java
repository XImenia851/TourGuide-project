package tourGuide.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import gpsUtil.location.Attraction;

public class TestGpsUtilWebClient {

    @Test
    public void getAttractions() {
        GpsUtilWebClient client = new GpsUtilWebClient();
        List<Attraction> attractions = client.getAttractions();

        assertNotNull(attractions);
        assertTrue(attractions.size() > 0);
    }
}