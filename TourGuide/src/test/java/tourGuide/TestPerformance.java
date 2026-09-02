package tourGuide;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserReward;

public class TestPerformance {
	
	/*
	 * A note on performance improvements:
	 *     
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *     
	 *     		InternalTestHelper.setInternalUserNumber(100000);
	 *     
	 *     
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent. 
	 * 
	 *     These are performance metrics that we are trying to hit:
	 *     
	 *     highVolumeTrackLocation: 100,000 users within 15 minutes:
	 *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     *     highVolumeGetRewards: 100,000 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */
	
//	@Ignore
@Test
public void highVolumeTrackLocation() {
	GpsUtil gpsUtil = new GpsUtil();
	RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

	// Target volume per the spec: 100,000 users within 15 minutes.
	InternalTestHelper.setInternalUserNumber(100000);
	TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

	// Stop the background Tracker right away, before starting the stopwatch.
	// Otherwise it keeps polling all users every 5 minutes and competes with this
	// test for the same thread pool, roughly doubling the measured time.
	tourGuideService.tracker.stopTracking();

	List<User> allUsers = tourGuideService.getAllUsers();

	StopWatch stopWatch = new StopWatch();
	stopWatch.start();

	// Fan out: submit every user's location tracking as an async task on the
	// thread pool. This call returns immediately with 100,000 "promises"
	// (CompletableFuture), it does not wait for any of them to finish.
	List<CompletableFuture<VisitedLocation>> futures = allUsers.stream()
			.map(tourGuideService::trackUserLocationAsync)
			.collect(Collectors.toList());

	// Fan in: block here until every future above has completed.
	// This is the async equivalent of the old sequential for-loop, except the
	// actual work ran in parallel across the pool while we were waiting.
	CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

	stopWatch.stop();
	// Release the thread pool now that all tasks are done.
	tourGuideService.shutdownExecutor();

	System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
	assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
}
	
	@Test
	public void highVolumeGetRewards() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		InternalTestHelper.setInternalUserNumber(100);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
	    Attraction attraction = gpsUtil.getAttractions().get(0);
		List<User> allUsers = new ArrayList<>();
		allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));
	     
	    allUsers.forEach(u -> rewardsService.calculateRewards(u));
	    
		for(User user : allUsers) {
			assertTrue(user.getUserRewards().size() > 0);
		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds."); 
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	
}
