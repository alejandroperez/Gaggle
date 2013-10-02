package com.alejandroperez.windprobe;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseSensor<K> implements Sensor<K> {
	private final Set<SampleStreamConsumer<K>> consumers;
	
	protected BaseSensor() {
		consumers = new HashSet<SampleStreamConsumer<K>>();
	}
	
	/**
	 * Sends a sample to all consumers, one after the next.
	 */
	protected void emit(Sample<K> sample) {
		for (SampleStreamConsumer<K> ssc : consumers) {
			ssc.consume(sample);
		}		
	}
	
	@Override
	public void registerConsumer(SampleStreamConsumer<K> ssc) {
		consumers.add(ssc);	
	}

	
}
