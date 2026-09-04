package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;


//added import
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	//NEW
	private final ExecutorService trackingExecutor = Executors.newFixedThreadPool(50);
	boolean testMode = true;
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}
	
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user);
		return visitedLocation;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(), 
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	// Applies new travel preferences to a user. Needed because nothing in the
// original codebase ever called User.setUserPreferences() - every trip
// deal was silently priced against the hardcoded UserPreferences defaults,
// regardless of the actual user.
	public UserPreferences updateUserPreferences(User user, UserPreferences preferences) {
		user.setUserPreferences(preferences);
		return preferences;
	}

	// Async wrapper around trackUserLocation(), submitted to the dedicated thread pool.
	// Used for high-volume batch processing (see TestPerformance); the synchronous
	// trackUserLocation() above is kept unchanged for the REST controller and the Tracker.
	public CompletableFuture<VisitedLocation> trackUserLocationAsync(User user) {
		return CompletableFuture.supplyAsync(() -> trackUserLocation(user), trackingExecutor);
	}

	// Returns the 5 closest attractions to the user's last known location,
	// regardless of how far away they are (the old version filtered by a
	// fixed 200-mile radius, which could return zero results).
	public List<NearbyAttraction> getNearByAttractions(VisitedLocation visitedLocation, User user) {
		return gpsUtil.getAttractions().stream()
				.sorted((a1, a2) -> Double.compare(
						rewardsService.getDistance(a1, visitedLocation.location),
						rewardsService.getDistance(a2, visitedLocation.location)))
				.limit(5)
				.map(attraction -> new NearbyAttraction(
						attraction.attractionName,
						attraction.latitude,
						attraction.longitude,
						visitedLocation.location.latitude,
						visitedLocation.location.longitude,
						rewardsService.getDistance(attraction, visitedLocation.location),
						rewardsService.getRewardPoints(attraction, user)))
				.collect(Collectors.toList());
	}


	// Returns each user's most recent known location, read from their stored
	// visited-location history (per spec: no fresh gpsUtil call here - gpsUtil
	// is only used upstream, by trackUserLocation, to populate that history).
	public Map<String, Location> getAllCurrentLocations() {
		Map<String, Location> currentLocations = new HashMap<>();
		for (User user : getAllUsers()) {
			if (!user.getVisitedLocations().isEmpty()) {
				currentLocations.put(user.getUserId().toString(), user.getLastVisitedLocation().location);
			}
		}
		return currentLocations;
	}


	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
				shutdownExecutor();
			}
		});
	}

	// Cleanly shuts down the tracking thread pool. Must be called once batch processing
	// is finished (see TestPerformance), otherwise the pool's threads stay alive.
	public void shutdownExecutor() {
		trackingExecutor.shutdown();
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}


	// Bundles everything the spec asks for about one nearby attraction: its
	// name/location, the user's location, the distance between them, and the
	// reward points for that attraction. A static nested class keeps this
	// data holder local to TourGuideService instead of adding a new file.
	public static class NearbyAttraction {
		public String attractionName;
		public double attractionLatitude;
		public double attractionLongitude;
		public double userLatitude;
		public double userLongitude;
		public double distanceInMiles;
		public int rewardPoints;

		public NearbyAttraction(String attractionName, double attractionLatitude, double attractionLongitude,
								double userLatitude, double userLongitude, double distanceInMiles, int rewardPoints) {
			this.attractionName = attractionName;
			this.attractionLatitude = attractionLatitude;
			this.attractionLongitude = attractionLongitude;
			this.userLatitude = userLatitude;
			this.userLongitude = userLongitude;
			this.distanceInMiles = distanceInMiles;
			this.rewardPoints = rewardPoints;
		}
	}

}
