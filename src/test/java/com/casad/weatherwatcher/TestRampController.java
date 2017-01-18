package com.casad.weatherwatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.casad.weatherwatcher.controller.RampController;
import com.casad.weatherwatcher.controller.RampController.RampState;

public class TestRampController {

	protected static int setIdleInvocationCount;
	protected static int setReadyInvocationCount;
	protected static int setActiveInvocationCount;
	
	Runnable mockSetIdle = new Runnable() {
		@Override
		public void run() {
			TestRampController.setIdleInvocationCount++;
		}
	};
	
	Runnable mockSetReady = new Runnable() {
		@Override
		public void run() {
			TestRampController.setReadyInvocationCount++;
		}
	};
	
	Runnable mockSetActive = new Runnable() {
		@Override
		public void run() {
			TestRampController.setActiveInvocationCount++;
		}
	};
	
	@Before
	public void resetInvocationCounts() {
		setIdleInvocationCount = 0;
		setReadyInvocationCount = 0;
		setActiveInvocationCount = 0;
	}
	
	@Test
	public void defaultInit() {
		RampController controller = new RampController(mockSetIdle, mockSetReady, mockSetActive);
		
		assertEquals(1, setIdleInvocationCount);
		assertEquals(0, setReadyInvocationCount);
		assertEquals(0, setActiveInvocationCount);
		assertEquals(RampState.IDLE, controller.getState());
	}
	
	@Test
	public void setReady() {
		RampController controller = new RampController(mockSetIdle, mockSetReady, mockSetActive);
		controller.setState(RampState.READY);
		
		assertEquals(1, setIdleInvocationCount);	// 1 from setup
		assertEquals(1, setReadyInvocationCount);
		assertEquals(0, setActiveInvocationCount);
		assertEquals(RampState.READY, controller.getState());
	}
	
	@Test
	public void setActive() {
		RampController controller = new RampController(mockSetIdle, mockSetReady, mockSetActive);
		controller.setState(RampState.ACTIVE);
		
		assertEquals(1, setIdleInvocationCount);	// 1 from setup
		assertEquals(0, setReadyInvocationCount);
		assertEquals(1, setActiveInvocationCount);
		assertEquals(RampState.ACTIVE, controller.getState());
	}
	
	@Test
	public void alreadySetTest() {
		RampController controller = new RampController(mockSetIdle, mockSetReady, mockSetActive);
		
		// Ramp is already idle - these should not increment the idle set counter
		controller.setState(RampState.IDLE);	
		controller.setState(RampState.IDLE);
		
		controller.setState(RampState.READY);	
		controller.setState(RampState.READY);
		
		controller.setState(RampState.ACTIVE);
		controller.setState(RampState.ACTIVE);
		
		assertEquals(1, setIdleInvocationCount);	// 1 from setup
		assertEquals(1, setReadyInvocationCount);
		assertEquals(1, setActiveInvocationCount);
		assertEquals(RampState.ACTIVE, controller.getState());
	}
	
	@Test
	public void nullState() {
		RampController controller = new RampController(mockSetIdle, mockSetReady, mockSetActive);
		boolean caughtException = false;

		try {
			controller.setState(null);	
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		
		assertTrue(caughtException);
		assertEquals(RampState.IDLE, controller.getState());
	}
}
