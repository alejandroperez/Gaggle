package com.alejandroperez.windprobe;

public interface SampleStreamConsumer<K> {
	/**
	 * Some preconditions to this method:
	 * 1) Should only be called once for a given sample.
	 * 2) Successive samples will have a time that is monotonically increasing.
	 * (non decreasing is not enough, since it will screw up most filters)
	 */
	public void consume(Sample<K> sample);

}
