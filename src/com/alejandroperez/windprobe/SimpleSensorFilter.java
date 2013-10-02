package com.alejandroperez.windprobe;

/**
 * Simple grammatical representation of a filter of sensor data.
 * Notices that filters, in general, can consume multiple sensor streams,
 * of different types, and output an altogether different type. However,
 * many sensors just consume a single source of a type, and output the
 * same type.
 * @author alperez
 *
 * @param <K>
 */
public interface SimpleSensorFilter<K> extends Sensor<K>, SampleStreamConsumer<K> {

}
