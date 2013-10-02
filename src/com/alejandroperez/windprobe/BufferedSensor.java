package com.alejandroperez.windprobe;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashSet;
import java.util.Set;

/**
 * A sensor "filter" that decouples execution of the downstream consumers
 * from the upstream producer.
 * In essence, it implements a finite queue (that will drop old samples
 * first if it overfills), and has an internal thread pumping the consumption
 * of the samples.
 * @author alperez
 *
 * @param <K>
 */
public final class BufferedSensor<K> extends BaseSensor<K> implements SimpleSensorFilter<K> {
	private final BlockingQueue<Sample<K>> sampleQueue;
	
	public BufferedSensor(Sensor<K> baseSensor, int bufferSize) {
		sampleQueue = new LinkedBlockingQueue<Sample<K>>(bufferSize);
		Thread thread = new Thread(new Runnable() {
			public void run () {
				try {
					emit(sampleQueue.take());
				} catch (InterruptedException e) {
					//skip to the next
				}
			}
		});
		thread.run();
	}
	
	@Override
	public void consume(Sample<K> sample) {
		synchronized (sampleQueue) {
			if (!sampleQueue.offer(sample)) {
				sampleQueue.poll();
				sampleQueue.offer(sample);
			}
		}
	}
	
}
