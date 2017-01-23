package com.casad.weatherwatcher.controller;

public class RampController {

	public enum RampState {
		IDLE, READY, ACTIVE
	};

	private Runnable setIdle, setReady, setActive;
	private RampState currentState = null;

	public RampController(Runnable makeIdle, Runnable makeReady, Runnable makeActive) {
		setIdle = makeIdle;
		setReady = makeReady;
		setActive = makeActive;
		
		currentState = RampState.IDLE;
		setIdle.run();
	}

	public void setState(RampState newState) {
		if (newState == null) {
			throw new IllegalArgumentException("Unknown or invalid state specified: " + newState);
		} else if (currentState == newState) {
			return;
		}
		
		switch (newState) {
		case IDLE:
			setIdle.run();
			break;
		case READY:
			setReady.run();
			break;
		case ACTIVE:
			setActive.run();
			break;
		default:
			throw new IllegalArgumentException("Unknown or invalid state specified: " + newState);
		}
		
		currentState = newState;
	}

	public RampState getState() {
		return currentState;
	}
}
