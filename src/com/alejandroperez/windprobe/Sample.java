package com.alejandroperez.windprobe;

/**
 * A sample is the executed measurement.
 * It contains not only the value, but also contextual information relevant to
 * that particular instance of measuring, for example the moment in time when
 * it occurred.
 * @author alperez
 * @param <V>
 */
public class Sample<K> {
	private final long timestamp;
	private final K value;
	private final K error;
	
	public Sample(K value, K error, long timestamp) {
		this.value = value;
		this.error = error;
		this.timestamp = timestamp;
	}
	
	public K getValue() {
		return value;
	}
	
	public K getError() {
		return error;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

}
