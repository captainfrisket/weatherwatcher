package com.casad.weatherwatcher.controller;

public class StatusNotificationController {
	
	public enum StatusState {
		GOOD, BAD
	};

	private Runnable setGood, setBad;
	private StatusState currentStatus = null;

	public StatusNotificationController(Runnable makeGood, Runnable makeBad) {
		setGood = makeGood;
		setBad = makeBad;
		currentStatus = null;
	}

	public void setStatus(StatusState newStatus) {
		if (newStatus == null) {
			throw new IllegalArgumentException("Unknown or invalid status specified: " + newStatus);
		} else if (currentStatus == newStatus) {
			return;
		}
		
		switch (newStatus) {
		case GOOD:
			setGood.run();
			break;
		case BAD:
			setBad.run();
			break;
		default:
			throw new IllegalArgumentException("Unknown or invalid status specified: " + newStatus);
		}
		
		currentStatus = newStatus;
	}

	public StatusState getStatus() {
		return currentStatus;
	}
}
