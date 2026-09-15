package tourGuide.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RewardsWebClient {

    private final WebClient webClient = WebClient.create("http://localhost:8082");

    public int getAttractionRewardPoints(UUID attractionId, UUID userId) {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/getAttractionRewardPoints")
                        .queryParam("attractionId", attractionId)
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return Integer.parseInt(response);
    }
}