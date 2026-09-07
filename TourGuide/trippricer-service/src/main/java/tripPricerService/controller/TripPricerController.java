package tripPricerService.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import tripPricer.Provider;
import tripPricer.TripPricer;

@RestController
public class TripPricerController {

    // Same TripPricer instance the monolith used to create directly - now it
    // lives here instead, behind an HTTP boundary.
    private final TripPricer tripPricer = new TripPricer();

    @GetMapping("/getPrice")
    public String getPrice(@RequestParam String apiKey, @RequestParam UUID userId,
                           @RequestParam int numberOfAdults, @RequestParam int numberOfChildren,
                           @RequestParam int tripDuration, @RequestParam int rewardPoints) {
        List<Provider> providers = tripPricer.getPrice(apiKey, userId, numberOfAdults, numberOfChildren, tripDuration, rewardPoints);
        return JsonStream.serialize(providers);
    }
}