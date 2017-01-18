package com.casad.weatherwatcher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amphibian.weather.response.WeatherResponse;
import com.casad.weatherwatcher.controller.RampController;
import com.casad.weatherwatcher.controller.RampController.RampState;

/**
 * The WeatherEventEngine will invoke callbacks on specific events returned from
 * the {@link WeatherService}. When the temperature drops below the idle
 * threshold, the state will move from IDLE to READY. If SNOW is returned as the
 * current or immediate upcoming weather, the state will move to ACTIVE.
 * 
 * @author dave
 *
 */
public class WeatherEventEngine {
	private static final Logger logger = LoggerFactory.getLogger(WeatherEventEngine.class);

	private int readyThreshold = 35;

	private WeatherService ws = null;
	private NotificationService ns = null;

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
						setControllerState(RampState.ACTIVE, "Ramp activating - snow is incoming!");
					} else if (isCold(response)) {
						setControllerState(RampState.READY, "Ramp is on standby, heat is ready!");
					} else {
						setControllerState(RampState.IDLE, "Ramp shutting down - enjoy warm the weather!");
					}

				} catch (Throwable t) {
					t.printStackTrace();
					ns.sendMessage("Error occured, see logs on device. WEE:63");
				}
			}
		};

		future = executor.scheduleAtFixedRate(task, initialStartDelay, periodLength, periodUnits);

	}
	
	private void setControllerState(RampState newState, String message) {
		if (newState != rampController.getState()) {
			rampController.setState(newState);
			ns.sendMessage(message);
		}
	}

	public void setPeriodLength(long period, TimeUnit unit) {
		periodLength = period;
		periodUnits = unit;
	}

	public void setWeatherService(WeatherService ws) {
		this.ws = ws;
	}

	public void setNotificationService(NotificationService service) {
		ns = service;
	}

	public boolean stop() {
		return future.cancel(false);
	}

	public int getIdleThreshold() {
		return readyThreshold;
	}

	public void setIdleThreshold(int temprature) {
		readyThreshold = temprature;
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
		return tempCurrent < readyThreshold;
	}

	public void setRampController(RampController controller) {
		rampController = controller;
	}
}
