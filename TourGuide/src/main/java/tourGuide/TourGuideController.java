package tourGuide;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tripPricer.Provider;

@RestController
public class TourGuideController {

    @Autowired
    TourGuideService tourGuideService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @RequestMapping("/getLocation")
    public String getLocation(@RequestParam String userName) {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        return JsonStream.serialize(visitedLocation.location);
    }

    //  TODO: Change this method to no longer return a List of Attractions.
    //  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
    //  Return a new JSON object that contains:
    // Name of Tourist attraction,
    // Tourist attractions lat/long,
    // The user's location lat/long,
    // The distance in miles between the user's location and each of the attractions.
    // The reward points for visiting each Attraction.
    //    Note: Attraction reward points can be gathered from RewardsCentral
    @RequestMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) {
        User user = getUser(userName);
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
        return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation, user));
    }

    @RequestMapping("/getRewards")
    public String getRewards(@RequestParam String userName) {
        return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }

    // TODO: Get a list of every user's most recent location as JSON
    //- Note: does not use gpsUtil to query for their current location,
    //        but rather gathers the user's current location from their stored location history.
    //
    // Return object should be the just a JSON mapping of userId to Locations similar to:
    //     {
    //        "019b04a9-067a-4c76-8817-ee75088c3822": {"longitude":-48.188821,"latitude":74.84371}
    //        ...
    //     }
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
        return JsonStream.serialize(tourGuideService.getAllCurrentLocations());
    }

    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
        List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
        return JsonStream.serialize(providers);
    }

    // New endpoint: nothing in the original codebase ever let a user's real
    // preferences (trip duration, number of children, etc.) reach TripPricer -
    // every trip deal was silently priced against the hardcoded defaults.
    @RequestMapping(method = RequestMethod.PUT, value = "/updatePreferences")
    public String updatePreferences(@RequestParam String userName,
                                    @RequestParam(required = false) Integer tripDuration,
                                    @RequestParam(required = false) Integer ticketQuantity,
                                    @RequestParam(required = false) Integer numberOfAdults,
                                    @RequestParam(required = false) Integer numberOfChildren) {
        User user = getUser(userName);
        UserPreferences preferences = user.getUserPreferences();

        // Only overwrite fields the caller actually provided; keep defaults for the rest.
        if (tripDuration != null) {
            preferences.setTripDuration(tripDuration);
        }
        if (ticketQuantity != null) {
            preferences.setTicketQuantity(ticketQuantity);
        }
        if (numberOfAdults != null) {
            preferences.setNumberOfAdults(numberOfAdults);
        }
        if (numberOfChildren != null) {
            preferences.setNumberOfChildren(numberOfChildren);
        }

        tourGuideService.updateUserPreferences(user, preferences);
        return JsonStream.serialize(preferences);
    }

    private User getUser(String userName) {
        return tourGuideService.getUser(userName);
    }

}