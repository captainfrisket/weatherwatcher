package com.casad.weatherwatcher;

import java.io.PrintWriter;
import java.io.StringWriter;
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

	private int readyActivateThreshold = 35;
	private int readyDeactivateThreshold = 40;
	
	private long deactivateDelay = 0;

	private WeatherService ws = null;
	private NotificationService ns = null;

	private ScheduledExecutorService executor = null;
	private ScheduledFuture<?> future = null;

	private long periodLength = 4;
	private TimeUnit periodUnits = TimeUnit.HOURS;
	private long initialStartDelay = 0;
	
	private RampController rampController = null;
	protected long deactivateTime = 1;

	public WeatherEventEngine() {
		executor = Executors.newScheduledThreadPool(1);
	}

	public void start() {
		assertNotNull("A weather service must be provided", ws);
		assertNotNull("A notification service must be provided", ns);
		assertNotNull("A ramp controller must be provided", rampController);
		
		Runnable task = new Runnable() {

			@Override
			public void run() {
				try {
					WeatherResponse response = ws.getWeatherReport();
					logWeather(response);
					
					if (isSnowingNowOrSoon(response)) {
						setControllerState(RampState.ACTIVE);
						ns.sendMessage(
								"Ramp activating - snow is incoming!", 
								"Snow has been detected in the forecast.  The ramp heater is now activating.\n\n" + getTextReport(response));
						deactivateTime = 0;
					} else if (deactivateTime == 0 && deactivateDelay != 0) {
						ns.sendMessage(
								"The snow has stopped staying active for a bit longer",
								"The snow has recently stopped.  The ramp will be heated for a bit longer to ensure the snow is all gone.\n\n" + getTextReport(response));
						deactivateTime = System.currentTimeMillis() + deactivateDelay;
					} else if (deactivateTime > System.currentTimeMillis()) {
						logger.info("Deactivating cooldown in progress...");
					} else if (isCold(response)) {
						setControllerState(RampState.READY);
						ns.sendMessage(
								"Ramp is going on standby due to cold weather",
								"The ramp head is going on standby due to cold weather.\n\n" + getTextReport(response));
					} else if (isWarm(response)) {
						setControllerState(RampState.IDLE);
						ns.sendMessage(
								"Ramp shutting down - enjoy warm the weather!",
								"The weather is currently warm - the ramp is deactivating.\n\n" + getTextReport(response));
					} else if (rampController.getState() == RampState.ACTIVE) {
						// The ramp state is active but it is no longer snowing,
						// but we are between the ready thresholds. Put the ramp
						// into a READY state.
						setControllerState(RampState.READY);
						ns.sendMessage(
								"Ramp is on standby, weather is looking up!",
								"It is no longer snowing, the ramp is going on standby.\n\n" + getTextReport(response));

					} else {
						// It has not been snowing, but we are between our ready and idle temps.
					}

				} catch (Throwable t) {
					t.printStackTrace();
					logger.error("Exception Caught (WEE:0002)", t);
					StringWriter sw = new StringWriter();
					t.printStackTrace(new PrintWriter(sw));
					ns.sendMessage(
							"An unexpected error occured in the garage ramp controller",
							"Error occured, see logs for more information.\n\n\nWEE:0002 - " + sw.toString());
					
				}
			}
		};
		
		future = executor.scheduleAtFixedRate(task, initialStartDelay, periodLength, periodUnits);

	}

	private void assertNotNull(String message, Object obj) {
		if (obj == null) {
			throw new RuntimeException(message);
		}
	}

	private void setControllerState(RampState newState) {
		if (newState != rampController.getState()) {
			rampController.setState(newState);
		}
	}

	public void setDeactivationDelay(long time) {
		deactivateDelay = time;
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

	public void setReadyThreshold(int turnOn, int turnOff) {
		readyActivateThreshold = turnOn;
		readyDeactivateThreshold = turnOff;
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
	
	private String getTextReport(WeatherResponse response) {
		StringBuffer message = new StringBuffer("Weather Report:").append("\n");
		
		String conditionsCurrent = response.getConditions().getWeather();
		float tempCurrent = response.getConditions().getTempF();
		String conditionsPeriod1 = response.getSimpleForecast().getDays2().get(0).getConditions();
		String conditionsPeriod2 = response.getSimpleForecast().getDays2().get(1).getConditions();
		String conditionsPeriod3 = response.getSimpleForecast().getDays2().get(2).getConditions();

		message.append("Current weather is ").append(conditionsCurrent).append(" and ").append(tempCurrent).append("F. ");
		if(isSnowingNowOrSoon(response)) {
			message.append("Analysis: Snow is incoming!");
		} else if (isCold(response)) {
			message.append("Analysis: It is cold.");
		} else if (isWarm(response)) {
			message.append("Analysis: It is warm out.");
		}
		
		message.append("\n");
		message.append("Incoming weather for the next three periods: ").append(conditionsPeriod1).append(", ");
		message.append(conditionsPeriod2).append(", ");
		message.append(conditionsPeriod3).append(".");
		
		return message.toString();
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
		return tempCurrent < readyActivateThreshold;
	}
	
	private boolean isWarm(WeatherResponse response) {
		float tempCurrent = response.getConditions().getTempF();
		return tempCurrent > readyDeactivateThreshold;
	}

	public void setRampController(RampController controller) {
		rampController = controller;
	}
}
