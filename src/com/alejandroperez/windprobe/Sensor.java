package com.alejandroperez.windprobe;

/**
 * A Sensor is a source of Samples.
 * @author alperez
 *
 */
public interface Sensor<K> {
	public void registerConsumer(SampleStreamConsumer<K> ssc);

}
