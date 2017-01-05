package com.casad.weatherwatcher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amphibian.weather.response.WeatherResponse;

public class WeatherEventEngine {
	private static final Logger logger = LoggerFactory.getLogger(WeatherEventEngine.class);

	
	private WeatherService ws = null;
	private Runnable activate = null, deactivate = null;

	private ScheduledExecutorService executor = null;
	private ScheduledFuture<?> future = null;

	private long periodLength = 4;
	private TimeUnit periodUnits = TimeUnit.HOURS;
	private long initialStartDelay = 0;

	public WeatherEventEngine() {
		executor = Executors.newScheduledThreadPool(1);
	}

	public void start() {
		Runnable task = new Runnable() {
			private boolean activated = false;

			@Override
			public void run() {
				try {
					WeatherResponse response = ws.getWeatherReport();
					boolean activateHeat = isSnowingNowOrSoon(response);
					
					if (activateHeat) {
						if (!activated) {
							activate.run();
							activated = true;
						}
					} else {
						if (activated) {
							deactivate.run();
							activated = false;
						}
					}

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		};
		
		future = executor.scheduleAtFixedRate(task, initialStartDelay, periodLength, periodUnits);

	}

	public void setPeriodLength(long period, TimeUnit unit) {
		periodLength = period;
		periodUnits = unit;
	}

	public void setWeatherService(WeatherService ws) {
		this.ws = ws;
	}

	public void setActivate(Runnable activate) {
		this.activate = activate;
	}

	public void setDeactivate(Runnable deactivate) {
		this.deactivate = deactivate;
	}

	public boolean stop() {
		return future.cancel(false);
	}

	private boolean isSnowingNowOrSoon(WeatherResponse response) {
		String conditionsCurrent = response.getConditions().getWeather();
		float tempCurrent = response.getConditions().getTempF();
		String conditionsPeriod1 = response.getSimpleForecast().getDays2().get(0).getConditions();
		String conditionsPeriod2 = response.getSimpleForecast().getDays2().get(1).getConditions();
		String conditionsPeriod3 = response.getSimpleForecast().getDays2().get(2).getConditions();
	
		StringBuffer message = new StringBuffer();
	
		
		message.append("Current Weather: [").append(conditionsCurrent);
		message.append("] (").append(tempCurrent).append("F)");
		message.append(", Upcoming: ");
		message.append("[").append(conditionsPeriod1).append("] ");
		message.append("[").append(conditionsPeriod2).append("] ");
		message.append("[").append(conditionsPeriod3).append("] ");
	
		logger.info(message.toString());
	
		if (conditionsCurrent.toUpperCase().contains("SNOW")
				|| conditionsPeriod1.toUpperCase().contains("SNOW")) {
			return true;
		}
		
		return false;
	
	}
}
