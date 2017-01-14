package com.casad.weatherwatcher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amphibian.weather.response.WeatherResponse;
import com.casad.weatherwatcher.RampController.RampState;

/**
 * The WeatherEventEngine will invoke callbacks on specific events returned from
 * the {@link WeatherService}. When the temprature drops below the idle
 * threshold, the state will move from IDLE to READY. If SNOW is returned as the
 * current or immediate upcoming weather, the state will move to ACTIVE.
 * 
 * @author dave
 *
 */
public class WeatherEventEngine {
	private static final Logger logger = LoggerFactory.getLogger(WeatherEventEngine.class);

	private int idleThreshold = 35;

	private WeatherService ws = null;
//	private Runnable activate = null, deactivate = null;

	private ScheduledExecutorService executor = null;
	private ScheduledFuture<?> future = null;

	private long periodLength = 4;
	private TimeUnit periodUnits = TimeUnit.HOURS;
	private long initialStartDelay = 0;

	private RampController rampController = null;

	public WeatherEventEngine() {
		executor = Executors.newScheduledThreadPool(1);
	}

	public void start() {
		Runnable task = new Runnable() {

			@Override
			public void run() {
				try {
					WeatherResponse response = ws.getWeatherReport();
					logWeather(response);
					
					if (isSnowingNowOrSoon(response)) {
						rampController.setState(RampState.ACTIVE);
					} else if (isCold(response)) {
						rampController.setState(RampState.READY);
					} else {
						rampController.setState(RampState.IDLE);
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

	public boolean stop() {
		return future.cancel(false);
	}

	public int getIdleThreshold() {
		return idleThreshold;
	}

	public void setIdleThreshold(int temprature) {
		idleThreshold = temprature;
	}

	private void logWeather(WeatherResponse response) {
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
	}
	
	private boolean isSnowingNowOrSoon(WeatherResponse response) {
		String conditionsCurrent = response.getConditions().getWeather();
		String conditionsPeriod1 = response.getSimpleForecast().getDays2().get(0).getConditions();

		if (conditionsCurrent.toUpperCase().contains("SNOW") || conditionsPeriod1.toUpperCase().contains("SNOW")) {
			return true;
		}

		return false;

	}

	private boolean isCold(WeatherResponse response) {
		float tempCurrent = response.getConditions().getTempF();
		return tempCurrent < idleThreshold;
	}

	public void setRampController(RampController controller) {
		rampController  = controller;
	}
}
