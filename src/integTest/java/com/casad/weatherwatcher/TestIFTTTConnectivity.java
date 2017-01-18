package com.casad.weatherwatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.lodenrogue.jmaker.JMaker;

public class TestIFTTTConnectivity {
	@Test
	public void createRequest() throws IOException {
		final Configuration config = Configuration.getConfig();
		final String jMakerIFTTTKey = config.getIFTTTApiKey();
		
		JMaker ifttt = new JMaker("GarageStatusUpdate", jMakerIFTTTKey);
		String message = UUID.randomUUID().toString();
		String tag = "[testing]";
		
		
		List<String> values = new ArrayList<String>();
		values.add(message);
		values.add(tag);
		values.add("");
		
		ifttt.trigger(values);
		System.out.println("Sent: " + message);
	}
}
