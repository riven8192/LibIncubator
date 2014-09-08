package net.indiespot.loadbalancer.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HighLevel {

	public static final boolean UNDETERMINEABLE_TRUE = Math.random() >= 0.0;
	public static final boolean UNDETERMINEABLE_FALSE = Math.random() < 0.0;

	public static final void sleep(long ms) {
		sleep(ms, false);
	}

	public static final void sleep(long ms, boolean consumeInterrupts) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException exc) {
			if (consumeInterrupts) {
				Thread.currentThread().interrupt();
				Thread.interrupted();
			}
		}
	}

	public static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException exc) {
			// ignore
		}
	}

	public static boolean await(CountDownLatch latch, long ms) {
		try {
			return latch.await(ms, TimeUnit.MILLISECONDS);
		} catch (InterruptedException exc) {
			// ignore
			return false;
		}
	}

	public static final boolean eq(Object a, Object b) {
		if (a == b) {
			return true;
		}
		if (a == null ^ b == null) {
			return false;
		}

		if (a != null) {
			return a.equals(b);
		}
		if (b != null) {
			return b.equals(a);
		}

		return false;
	}
}
