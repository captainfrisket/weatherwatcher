package com.casad.weatherwatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

	private static final String SWIRL_PROPERTIES = "ww.properties";
	private static Configuration instance = null;
	private Properties prop = new Properties();

	private Configuration(String propFileName) {
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(propFileName)) {
			prop.load(input);

		} catch (IOException | NullPointerException e) {
			logger.error("Unable to load the properties file.  Please check that '" + SWIRL_PROPERTIES + "' exists.", e);
		}
	}

	public static Configuration getConfig() {
		if (instance == null) {
			instance = new Configuration(SWIRL_PROPERTIES);
		}

		return instance;
	}

	public String getWundergroundApiKey() {
		return prop.getProperty("wunderground.apiKey");
	}
	
	public String getZipCode() {
		return prop.getProperty("wunderground.zipCode");
	}

	public String getIFTTTApiKey() {
		return prop.getProperty("ifttt.apikey");
	}

	public String getTwitterAPIKey() {
		return prop.getProperty("twitter.apikey");
	}
	
	public String getEmailUsername() {
		return prop.getProperty("email.username");
	}
	
	public String getEmailPassword() {
		return prop.getProperty("email.password");
	}
	
	public String getEmailTo() {
		return prop.getProperty("email.notifyAddress");
	}
	
	
}
