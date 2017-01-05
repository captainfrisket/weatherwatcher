package com.casad.weatherwatcher;

import java.util.List;

import org.junit.Test;

import com.amphibian.weather.request.Feature;
import com.amphibian.weather.request.WeatherRequest;
import com.amphibian.weather.response.ForecastDay;
import com.amphibian.weather.response.WeatherResponse;
import com.casad.weatherwatcher.Configuration;

public class WundergroundAPIWeatherAccessExample {
	
	
	@Test
	public void createRequest() {
		WeatherRequest req = new WeatherRequest();
		req.setApiKey(Configuration.getConfig().getWundergroundApiKey());
		req.addFeature(Feature.CONDITIONS);
		req.addFeature(Feature.FORECAST);

		WeatherResponse resp = req.query(Configuration.getConfig().getZipCode());
		System.out.println("Current weather: " + resp.getConditions().getWeather() + ", " + resp.getConditions().getTempC() + " deg C");
		
		List<ForecastDay> forecast = resp.getSimpleForecast().getDays2();
		
		for (ForecastDay day : forecast) {
			System.out.println("Upcoming weather: " + day.getConditions());
		}
		
	}
}
