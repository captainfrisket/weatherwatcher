package com.casad.weatherwatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amphibian.weather.request.Feature;
import com.amphibian.weather.request.WeatherRequest;
import com.amphibian.weather.response.WeatherResponse;
import com.casad.weatherwatcher.controller.RampController;
import com.lodenrogue.JMaker.JMaker;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class WeatherWatcher {
	private static final Logger logger = LoggerFactory.getLogger(WeatherWatcher.class);

	protected static GpioController gpio = null;
	protected static GpioPinDigitalOutput readyPin = null;
	protected static GpioPinDigitalOutput activePin = null;
	protected static GpioPinDigitalOutput relay3 = null;
	protected static GpioPinDigitalOutput onlinePin = null;
	protected static JMaker maker = null;
	
	private static final long HOURS_TO_MILLISECONDS = 3_600_000;
	
	public static void main(String[] args) throws Exception {
		
		// preflight checks
		logger.info("Waking up!");
		
		// Get configuration settings
		final Configuration config = Configuration.getConfig();
		
		final String jMakerIFTTTKey = config.getIFTTTApiKey();
		assertSet("A JMaker API key must be specified. See README.md for more information.", jMakerIFTTTKey);
		maker = new JMaker("GarageStatusUpdate", jMakerIFTTTKey);//"CD2j-Iv6_cq5iRUg71Ixs"

		/*
		 * Create the GPIO controller instance
		 * 
		 * The PIN numbering used here corresponds to the pi4j library - NOT the
		 * GPIO pin numbers!!! See: http://pi4j.com/pins/model-b-plus.html
		 * 
		 * Another oddity here is that the relay being used (JBtek 4 Channel DC
		 * 5V Relay Module for Arduino Raspberry Pi DSP AVR PIC ARM -
		 * https://www.amazon.com/gp/product/B00KTEN3TM) uses HIGH output to
		 * indicate OFF and LOW to indicate ON.
		 */
		gpio = GpioFactory.getInstance();
		readyPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Relay 1", PinState.HIGH);
		activePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Relay 2", PinState.HIGH);
		
		onlinePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Relay 4", PinState.HIGH);
		onlinePin.pulse(500);
		
		// These are unused, configure as OFF to prevent unexpected conditions.
		relay3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Relay 3", PinState.HIGH);
		
		relay3.high();
		
		
		// Diagnostic startup
		readyPin.high();
		activePin.high();
		Thread.sleep(1000);
		readyPin.low();
		Thread.sleep(1000);
		activePin.low();
		Thread.sleep(1000);
		readyPin.high();
		activePin.high();
		Thread.sleep(1000);
		
		Runnable makeIdle = () -> {
			// Make IDLE / OFF
			readyPin.high();
			activePin.high();
		};

		Runnable makeReady = () -> {
			// Make READY
			readyPin.low();
			activePin.high();
		};
		
		Runnable makeActive = () -> {
			// Make ACTIVE
			readyPin.low();
			activePin.low();
		};
		
		
		// Create RampController instance with pin details
		RampController controller = new RampController(makeIdle, makeReady, makeActive);
		
		final String weatherAPIKey = config.getWundergroundApiKey();
		assertSet("A weather API key must be specified. See README.md for more information.", weatherAPIKey);
		
		final String zipCode = config.getZipCode();
		assertSet("A weather zip key must be specified. See README.md for more information.", zipCode);
		
		// Prepare the weather event engine
		WeatherEventEngine eng = new WeatherEventEngine();
		eng.setPeriodLength(60, TimeUnit.MINUTES);
		eng.setDeactivationDelay(24 * HOURS_TO_MILLISECONDS);
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
		
		eng.setNotificationService(new NotificationService() {
			
			@Override
			public void sendMessage(String message) {
				triggerIftt(message);
			}
		});
		
		eng.start();
		
		onlinePin.high();
		triggerIftt("Startup Complete, controller online!");
		while(true) {
			Thread.sleep(3600000);
		}
	}

	private static void triggerIftt(String message) {
		if (maker == null) {
			logger.info(message);
			return;
		}
		
		logger.info("Notifying: " + message);
		List<String> values = new ArrayList<String>();
		values.add(message);
		values.add("");
		values.add("");
		
		try {
			maker.trigger(values);
		} catch (IOException e) {
			logger.error("There was a problem connecting to the IFTT maker channel");
			e.printStackTrace();
		}
	}

	private static void assertSet(String message, String value) {
		if (value == null || "".equals(value)) {
			logger.error(message);
			System.exit(0);
		}
	}

}
