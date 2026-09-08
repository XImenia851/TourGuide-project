package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

import tripPricer.Provider;

@Service
public class TripPricerWebClient {

    private final WebClient webClient = WebClient.create("http://localhost:8083");

    public List<Provider> getPrice(String apiKey, UUID userId, int numberOfAdults, int numberOfChildren,
                                   int tripDuration, int rewardPoints) {
        String json = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/getPrice")
                        .queryParam("apiKey", apiKey)
                        .queryParam("userId", userId)
                        .queryParam("numberOfAdults", numberOfAdults)
                        .queryParam("numberOfChildren", numberOfChildren)
                        .queryParam("tripDuration", tripDuration)
                        .queryParam("rewardPoints", rewardPoints)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Provider has no no-arg constructor either (same situation as
        // Attraction) - parse generically with Any and rebuild manually.
        Any providersJson = JsonIterator.deserialize(json);
        List<Provider> providers = new ArrayList<>();
        for (Any item : providersJson.asList()) {
            UUID tripId = new UUID(
                    item.get("tripId", "mostSigBits").toLong(),
                    item.get("tripId", "leastSigBits").toLong());
            Provider provider = new Provider(tripId, item.get("name").toString(), item.get("price").toDouble());
            providers.add(provider);
        }
        return providers;
    }
}