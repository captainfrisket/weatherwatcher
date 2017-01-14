package com.casad.weatherwatcher;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amphibian.weather.request.Feature;
import com.amphibian.weather.request.WeatherRequest;
import com.amphibian.weather.response.WeatherResponse;

public class WeatherWatcher {
	private static final Logger logger = LoggerFactory.getLogger(WeatherWatcher.class);

	public static void main(String[] args) throws Exception {
		
		// preflight checks
		logger.info("Waking up!");
		
		// Get configuration settings
		final Configuration c = Configuration.getConfig();
		
		// create gpio controller instance
//		final GpioController gpio = GpioFactory.getInstance();
//		GpioPinDigitalOutput readyPin = gpio.provisionDigitalOutputPin(null, "ReadyRelay", PinState.LOW);
//		GpioPinDigitalOutput activePin = gpio.provisionDigitalOutputPin(null);
		
		// Create RampController instance with pin details
		RampController controller = new RampController(new Runnable() {
			@Override
			public void run() {
				// Make IDLE / OFF
//				readyPin.low();
//				activePin.low();
				
				logger.info("Setting to idle/off!");
			}
		}, new Runnable() {
			@Override
			public void run() {
				// Make READY
//				readyPin.high();
//				activePin.low();
				
				logger.info("Setting to READY (low active)!");
			}
		}, new Runnable() {
			@Override
			public void run() {
				// Make ACTIVE
//				readyPin.high();
//				activePin.low();
				
				logger.info("Setting to ACTIVE - Full ON!");
			}
		});
		
		final String weatherAPIKey = c.getWundergroundApiKey();
		assertSet("A weather API key must be specified. See README.md for more information.", weatherAPIKey);
		
		final String zipCode = c.getZipCode();
		assertSet("A weather zip key must be specified. See README.md for more information.", zipCode);
		
		// Prepare the weather event engine
		WeatherEventEngine eng = new WeatherEventEngine();
		eng.setPeriodLength(60, TimeUnit.MINUTES);
		eng.setRampController(controller);
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
		while(true) {
			Thread.sleep(3600000);
		}
	}

	private static void assertSet(String message, String value) {
		if (value == null || "".equals(value)) {
			logger.error(message);
			System.exit(0);
		}
	}

}
