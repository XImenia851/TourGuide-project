package tourGuide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;

@Configuration
public class TourGuideModule {

	// GpsUtil bean kept for now - TourGuideService still uses it directly.
	// Will be removed once TourGuideService is switched to GpsUtilWebClient
	// in the next step.
	@Bean
	public GpsUtil getGpsUtil() {
		return new GpsUtil();
	}

}