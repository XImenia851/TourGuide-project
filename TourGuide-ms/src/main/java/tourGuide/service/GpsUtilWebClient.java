package tourGuide.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

@Service
public class GpsUtilWebClient {

    private final WebClient webClient = WebClient.create("http://localhost:8081");

    public VisitedLocation getUserLocation(UUID userId) {
        String json = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/getUserLocation").queryParam("userId", userId).build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Any root = JsonIterator.deserialize(json);

        UUID visitedUserId = new UUID(
                root.get("userId", "mostSigBits").toLong(),
                root.get("userId", "leastSigBits").toLong());

        Location location = new Location(
                root.get("location", "latitude").toDouble(),
                root.get("location", "longitude").toDouble());

        // Date serializes as a huge nested structure (internal JDK calendar
        // cache fields) via jsoniter's reflection encoder - "time" is the
        // only field that matters: the epoch millisecond timestamp.
        Date timeVisited = new Date(root.get("timeVisited", "time").toLong());

        return new VisitedLocation(visitedUserId, location, timeVisited);
    }

    public List<Attraction> getAttractions() {
        String json = webClient.get()
                .uri("/getAttractions")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Any attractionsJson = JsonIterator.deserialize(json);
        List<Attraction> attractions = new ArrayList<>();
        for (Any item : attractionsJson.asList()) {
            Attraction attraction = new Attraction(
                    item.get("attractionName").toString(),
                    item.get("city").toString(),
                    item.get("state").toString(),
                    item.get("latitude").toDouble(),
                    item.get("longitude").toDouble());
            attractions.add(attraction);
        }
        return attractions;
    }

}