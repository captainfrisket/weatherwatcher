package com.casad.weatherwatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
//import org.junit.Assert;
import org.junit.Test;

import com.amphibian.weather.response.Conditions;
import com.amphibian.weather.response.Forecast;
import com.amphibian.weather.response.ForecastDay;
import com.amphibian.weather.response.ForecastWrapper;
import com.amphibian.weather.response.WeatherResponse;
import com.casad.weatherwatcher.controller.RampController;

public class TestWeatherEventEngine {

	private static final int WAIT_TIMEOUT = 5_000; // milliseconds
	private static final int POLL_FREQUENCY = 50; // milliseconds

	private static final long weatherEventEnginePeriodLength = 100;
	private static final TimeUnit weatherEventEnginePeriodUnit = TimeUnit.MILLISECONDS;
	
	private static int activateCount;
	private static int idleCount;
	private static int readyCount;
	
	private static final String CLEAR = "Clear";
	private static final String SNOW = "SNOW";

	private static final WeatherResponse GOOD_WEATHER = createWeatherResponse(CLEAR, 72, CLEAR, CLEAR, CLEAR, CLEAR);
	private static final WeatherResponse CLEAR_COLD = createWeatherResponse(CLEAR, 10, CLEAR, CLEAR, CLEAR, CLEAR);
	private static final WeatherResponse SNOWING = createWeatherResponse(SNOW, 10, CLEAR, CLEAR, CLEAR, CLEAR);

	
	@Test
	public void testStartStop() throws Exception {
		MockWeatherService mockWS = new MockWeatherService();
		mockWS.setWeatherReport(GOOD_WEATHER);

		WeatherEventEngine eng = getWeatherEngineForTest(mockWS); 

		eng.start();

		mockWS.waitForQuery(3);

		assertTrue(eng.stop());
		assertEquals(0, activateCount);
		assertEquals(0, readyCount);
		assertEquals(1, idleCount);

	}

	private WeatherEventEngine getWeatherEngineForTest(MockWeatherService mockWS) {
		WeatherEventEngine eng = new WeatherEventEngine();
		eng.setPeriodLength(weatherEventEnginePeriodLength, weatherEventEnginePeriodUnit);

		RampController mockController = new RampController(new Runnable() {
			@Override
			public void run() {
				// Make IDLE / OFF
				TestWeatherEventEngine.idleTriggered();
			}
		}, new Runnable() {
			@Override
			public void run() {
				// Make READY
				TestWeatherEventEngine.readyTriggered();
			}
		}, new Runnable() {
			@Override
			public void run() {
				// Make ACTIVE
				TestWeatherEventEngine.activateTriggered();
			}
		});
		
		eng.setRampController(mockController);
		eng.setWeatherService(mockWS);
		return eng;
	}

	@Test
	public void testActivate() throws Exception {
		MockWeatherService mockWS = new MockWeatherService();
		mockWS.setWeatherReport(GOOD_WEATHER);

		WeatherEventEngine eng = getWeatherEngineForTest(mockWS); 

		eng.start();

		mockWS.waitForQuery(3);
		mockWS.setWeatherReport(SNOWING);
		
		mockWS.waitForQuery(3);
		
		assertTrue(eng.stop());
		assertEquals(1, activateCount);
		assertEquals(0, readyCount);
		assertEquals(1, idleCount);
	}
	
	@Test
	public void testReady() throws Exception {
		MockWeatherService mockWS = new MockWeatherService();
		mockWS.setWeatherReport(GOOD_WEATHER);

		WeatherEventEngine eng = getWeatherEngineForTest(mockWS); 

		eng.start();

		mockWS.waitForQuery(3);
		mockWS.setWeatherReport(CLEAR_COLD);
		
		mockWS.waitForQuery(3);
		
		assertTrue(eng.stop());
		assertEquals(0, activateCount);
		assertEquals(1, readyCount);
		assertEquals(1, idleCount);
	}
	
	@Test
	public void testDeactivate() throws Exception {
		MockWeatherService mockWS = new MockWeatherService();
		mockWS.setWeatherReport(GOOD_WEATHER);

		WeatherEventEngine eng = getWeatherEngineForTest(mockWS); 

		eng.start();

		mockWS.waitForQuery(3);
		mockWS.setWeatherReport(SNOWING);
		mockWS.waitForQuery(3);
		mockWS.setWeatherReport(GOOD_WEATHER);
		mockWS.waitForQuery(3);
		
		assertTrue(eng.stop());
		assertEquals(1, activateCount);
		assertEquals(0, readyCount);
		assertEquals(2, idleCount);
	}

	@Before
	public void resetTriggers() {
		activateCount = 0;
		readyCount = 0;
		idleCount = 0;
	}

	private static void activateTriggered() {
		activateCount++;
	}

	private static void readyTriggered() {
		readyCount++;
		
	}

	private static void idleTriggered() {
		idleCount++;
		
	}

	public static WeatherResponse createWeatherResponse(String current, long temp, String period1Conditions,
			String period2Conditions, String period3Conditions, String period4Conditions) {
		
		WeatherResponse response = new WeatherResponse();
	
		Conditions currentConditions = new Conditions();
		currentConditions.setWeather(current);
		currentConditions.setTempF(temp);
		response.setConditions(currentConditions);
	
		ForecastDay day1 = new ForecastDay();
		day1.setConditions(period1Conditions);
	
		ForecastDay day2 = new ForecastDay();
		day1.setConditions(period2Conditions);
	
		ForecastDay day3 = new ForecastDay();
		day1.setConditions(period3Conditions);
	
		ForecastDay day4 = new ForecastDay();
		day1.setConditions(period4Conditions);
	
		Forecast forecast = new Forecast();
		forecast.setDays(Arrays.asList(day1, day2, day3, day4));
		ForecastWrapper forecastWrapper = new ForecastWrapper();
		forecastWrapper.setSimpleForecast(forecast);
		response.setForecasts(forecastWrapper);
	
		return response;
	}

	public class MockWeatherService implements WeatherService {

		private int queries;

		private WeatherResponse report;

		public MockWeatherService() {
			report = createWeatherResponse(CLEAR, 72, CLEAR, CLEAR, CLEAR, CLEAR);
			queries = 0;
		}

		public void waitForQuery(int queryCount) throws InterruptedException {
			int currentCount = queries;
			int timeoutCount = WAIT_TIMEOUT / POLL_FREQUENCY;
			while (timeoutCount-- > 0 && queries < currentCount + queryCount) {
				System.out.println("Timeout Count: " + timeoutCount + ", Queries: " + queries);
				Thread.sleep(POLL_FREQUENCY);
			}
			if (queries < currentCount + queryCount) {
				fail("Failed");
			}
		}

		public void setWeatherReport(WeatherResponse r) {
			report = r;
		}
		
		@Override
		public WeatherResponse getWeatherReport() {
			queries++;

			return report;
		}

	}
}
