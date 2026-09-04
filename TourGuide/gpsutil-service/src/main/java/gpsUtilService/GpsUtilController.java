package gpsUtilService;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import gpsUtil.GpsUtil;
import gpsUtil.location.VisitedLocation;

@RestController
public class GpsUtilController {

    // Same GpsUtil instance the monolith used to create directly - now it
    // lives here instead, behind an HTTP boundary.
    private final GpsUtil gpsUtil = new GpsUtil();

    @GetMapping("/getUserLocation")
    public String getUserLocation(@RequestParam UUID userId) {
        VisitedLocation visitedLocation = gpsUtil.getUserLocation(userId);
        return JsonStream.serialize(visitedLocation);
    }

    @GetMapping("/getAttractions")
    public String getAttractions() {
        return JsonStream.serialize(gpsUtil.getAttractions());
    }
}