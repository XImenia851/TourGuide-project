package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

@Service
public class GpsUtilWebClient {

    private final WebClient webClient = WebClient.create("http://localhost:8081");

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