package rewardCentralService.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import rewardCentral.RewardCentral;

@RestController
public class RewardCentralController {

    // Same RewardCentral instance the monolith used to create directly - now
    // it lives here instead, behind an HTTP boundary.
    private final RewardCentral rewardCentral = new RewardCentral();

    @GetMapping("/getAttractionRewardPoints")
    public String getAttractionRewardPoints(@RequestParam UUID attractionId, @RequestParam UUID userId) {
        int rewardPoints = rewardCentral.getAttractionRewardPoints(attractionId, userId);
        return JsonStream.serialize(rewardPoints);
    }
}