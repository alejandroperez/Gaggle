package com.alejandroperez.windprobe;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public abstract class PollingSensor<K> extends BaseSensor<K> {
	private final int pollingInterval;
	private final Timer timer;
	private final TimerTask tt;
	
	/**
	 * Polling method to implement by the concrete subclass.
	 * @return
	 */
	protected abstract Sample<K> getLatestSample();
	
	/**
	 * 
	 * @param pollingInterval polling interval in ms.
	 */
	public PollingSensor(int pollingInterval) {
		this.pollingInterval = pollingInterval;
		timer = new Timer();
		tt = new TimerTask() {
			public void run() {
				emit(getLatestSample());
			}
		};
		timer.scheduleAtFixedRate(tt, 0, pollingInterval);
	}
	
		
}
