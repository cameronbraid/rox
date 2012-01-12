package com.flat502.rox.processing;

import junit.framework.AssertionFailedError;

public class ThreadUtils {
	private static final String CLIENT_WORKER_NAME = ".*\\.HttpResponseHandler-\\d+";
	private static final String SERVER_WORKER_NAME = ".*\\.HttpRequestHandler-\\d+";
	private static final String HTTP_PROCESSOR_NAME = ".*\\.HttpRpcProcessor";
	private static final String WORKER_POOL_TIMER = "java.util.TimerThread";

	public static void assertZeroThreads() throws Exception {
		waitForTermination("Client Workers", CLIENT_WORKER_NAME);
		waitForTermination("Server Workers", SERVER_WORKER_NAME);
		waitForTermination("Selector", HTTP_PROCESSOR_NAME);
		waitForTermination("Timer", WORKER_POOL_TIMER);
	}

	private static void waitForTermination(String name, String namePattern) throws Exception {
		int delay = 100;
		for(int i = 0; i < 5; i++) {
			if (countThreads(namePattern) == 0) {
				return;
			}
			Thread.sleep(delay);
			delay *= 2;
		}
		throw new AssertionFailedError(name+" never terminated");
	}

	public static int countClientWorkerThreads() {
		return countThreads(CLIENT_WORKER_NAME);
	}
	
	public static int countServerWorkerThreads() {
		return countThreads(SERVER_WORKER_NAME);
	}
	
	public static int countSelectorThreads() {
		return countThreads(HTTP_PROCESSOR_NAME);
	}
	
	public static int countWorkerPoolTimerThreads() {
		return countThreads(WORKER_POOL_TIMER);
	}
	
	private static int countThreads(String namePattern) {
		Thread[] list = new Thread[100];
		Thread.enumerate(list);
		int matches = 0;
		for (int i = 0; i < list.length; i++) {
			if (list[i] != null) {
				if (list[i].getName().matches(namePattern) || list[i].getClass().getName().matches(namePattern)) {
					matches++;
				}
			}
		}
		return matches;
	}
}
