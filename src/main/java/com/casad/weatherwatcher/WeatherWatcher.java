package com.casad.weatherwatcher;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amphibian.weather.request.Feature;
import com.amphibian.weather.request.WeatherRequest;
import com.amphibian.weather.response.WeatherResponse;

import twitter4j.TwitterException;

public class WeatherWatcher {
	private static final Logger logger = LoggerFactory.getLogger(WeatherWatcher.class);

	public static void main(String[] args) throws TwitterException {
		
		// preflight checks
		logger.info("Waking up!");
		
		Configuration c = Configuration.getConfig();
		
		final String weatherAPIKey = c.getWundergroundApiKey();
		assertSet("A weather API key must be specified. See README.md for more information.", weatherAPIKey);
		
		final String zipCode = c.getZipCode();
		assertSet("A weather zip key must be specified. See README.md for more information.", zipCode);
		
		// Prepare the weather event engine
		WeatherEventEngine eng = new WeatherEventEngine();
		
		eng.setPeriodLength(60, TimeUnit.MINUTES);
//		eng.setActivate(() -> logger.info("Activating!"));
		eng.setActivate(new Runnable() {
			@Override
			public void run() {
				logger.info("Activating!");
			}
		});
		
//		eng.setDeactivate(() -> logger.info("Deactivating!"));
		eng.setDeactivate(new Runnable() {
			@Override
			public void run() {
				logger.info("Deactivating!");
			}
		});
		
		eng.setWeatherService(new WeatherService() {
			
			@Override
			public WeatherResponse getWeatherReport() {
				WeatherRequest req = new WeatherRequest();
				req.setApiKey(weatherAPIKey);
				req.addFeature(Feature.CONDITIONS);
				req.addFeature(Feature.FORECAST);

				return req.query(zipCode);
			}
		});
		
		eng.start();
		while(true) {}
	}

	private static void assertSet(String message, String value) {
		if (value == null || "".equals(value)) {
			logger.error(message);
			System.exit(0);
		}
	}

}
