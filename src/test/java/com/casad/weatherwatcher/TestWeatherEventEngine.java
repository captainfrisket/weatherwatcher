package com.casad.weatherwatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

//import org.junit.Assert;
import org.junit.Test;

import com.amphibian.weather.response.Conditions;
import com.amphibian.weather.response.Forecast;
import com.amphibian.weather.response.ForecastDay;
import com.amphibian.weather.response.ForecastWrapper;
import com.amphibian.weather.response.WeatherResponse;
import com.casad.weatherwatcher.WeatherEventEngine;
import com.casad.weatherwatcher.WeatherService;

public class TestWeatherEventEngine {

	private static final int WAIT_TIMEOUT = 5_000; // milliseconds
	private static final int POLL_FREQUENCY = 50; // milliseconds

	private static final long weatherEventEnginePeriodLength = 100;
	private static final TimeUnit weatherEventEnginePeriodUnit = TimeUnit.MILLISECONDS;
	
	private static int activateCount;
	private static int deactivateCount;
	
	private static final String CLEAR = "Clear";
	private static final String SNOW = "SNOW";

	private static final WeatherResponse GOOD_WEATHER = createWeatherResponse(CLEAR, 72, CLEAR, CLEAR, CLEAR, CLEAR);
	private static final WeatherResponse SNOWING = createWeatherResponse(SNOW, 72, CLEAR, CLEAR, CLEAR, CLEAR);

	
	@Test
	public void testStartStop() throws Exception {
		resetTriggers();
		MockWeatherService mockWS = new MockWeatherService();
		mockWS.setWeatherReport(GOOD_WEATHER);

		WeatherEventEngine eng = getWeatherEngineForTest(mockWS); 

		eng.start();

		mockWS.waitForQuery(3);

		assertTrue(eng.stop());
		assertEquals(0, activateCount);
		assertEquals(0, deactivateCount);
	}

	private WeatherEventEngine getWeatherEngineForTest(MockWeatherService mockWS) {
		WeatherEventEngine eng = new WeatherEventEngine();
		eng.setPeriodLength(weatherEventEnginePeriodLength, weatherEventEnginePeriodUnit);

		eng.setActivate(() -> TestWeatherEventEngine.activateTriggered());
		eng.setDeactivate(() -> TestWeatherEventEngine.deactivateTriggered());
		
		eng.setWeatherService(mockWS);
		return eng;
	}

	@Test
	public void testActivate() throws Exception {
		resetTriggers();
		MockWeatherService mockWS = new MockWeatherService();
		mockWS.setWeatherReport(GOOD_WEATHER);

		WeatherEventEngine eng = getWeatherEngineForTest(mockWS); 

		eng.start();

		mockWS.waitForQuery(3);
		mockWS.setWeatherReport(SNOWING);
		
		mockWS.waitForQuery(3);
		
		assertTrue(eng.stop());
		assertEquals(1, activateCount);
		assertEquals(0, deactivateCount);
	}
	
	@Test
	public void testDeactivate() throws Exception {
		resetTriggers();
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
		assertEquals(1, deactivateCount);
	}

	private void resetTriggers() {
		activateCount = 0;
		deactivateCount = 0;
	}

	private static void activateTriggered() {
		activateCount++;
	}

	private static void deactivateTriggered() {
		deactivateCount++;
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
