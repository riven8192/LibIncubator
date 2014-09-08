package net.indiespot.loadbalancer.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool implements Executor {
	private final ThreadFactory factory;
	private final SimpleBlockingQueue<Runnable> taskQueue;
	private final int maxThreads;
	private final long idleTimeout;
	private final AtomicInteger threadCount;
	private final AtomicInteger activeCount;
	private volatile boolean isShutdown;

	public ThreadPool(int maxThreads) {
		this(maxThreads, default_idle_timeout);
	}

	public ThreadPool(int maxThreads, long idleTimeout) {
		this(maxThreads, idleTimeout, default_thread_factory);
	}

	public ThreadPool(int maxThreads, long idleTimeout, ThreadFactory factory) {

		if (maxThreads < 1) {
			throw new IllegalArgumentException();
		}
		if (idleTimeout < 0) {
			throw new IllegalArgumentException();
		}
		if (factory == null) {
			throw new NullPointerException();
		}

		this.factory = factory;
		this.taskQueue = new SimpleBlockingQueue<>();
		this.maxThreads = maxThreads;
		this.idleTimeout = idleTimeout;
		this.threadCount = new AtomicInteger(0);
		this.activeCount = new AtomicInteger(0);
		this.isShutdown = false;

		GLOBAL_POOL_MANAGER.monitor(this);
	}

	@Override
	public void execute(Runnable task) {
		if (task == null) {
			throw new IllegalStateException();
		}

		if (isShutdown) {
			throw new IllegalStateException("pool is shutdown");
		}

		taskQueue.put(task);
	}

	//

	public int getActiveCount() {
		return activeCount.get();
	}

	public int getWorkerCount() {
		return threadCount.get();
	}

	public int getQueueBacklog() {
		return taskQueue.size();
	}

	//

	public boolean requestWorker() {
		if (threadCount.incrementAndGet() > maxThreads) {
			// we were too optimistic
			threadCount.decrementAndGet();

			// there are enough workers already
			return false;
		}

		// w00t!
		new Worker();
		return true;
	}

	public int requestWorkers(int amount) {
		int got = 0;
		for (int i = 0; i < amount; i++) {
			if (!this.requestWorker()) {
				break;
			}
			got++;
		}
		return got;
	}

	public int requestAllWorkers() {
		return this.requestWorkers(maxThreads);
	}

	//

	public void signalWorkerShutdown() {
		taskQueue.put(null);
	}

	public void signalWorkersShutdown(int amount) {
		for (int i = 0; i < amount; i++) {
			this.signalWorkerShutdown();
		}
	}

	public void signalAllWorkersShutdown() {
		this.signalWorkersShutdown(maxThreads);
	}

	//

	public void shutdown() {
		this.execute(new Runnable() {
			@Override
			public void run() {
				isShutdown = true;
				signalAllWorkersShutdown();
			}
		});
	}

	private class Worker implements Runnable {
		public Worker() {
			factory.newThread(this).start();
		}

		@Override
		public void run() {
			try {
				while (true) {
					Runnable task = taskQueue.poll(idleTimeout);
					if (task == null) {
						// idle, or signal to terminate!
						break;
					}

					try {
						activeCount.incrementAndGet();
						task.run();
					} catch (Throwable cause) {
						// report problem, but keep the worker alive
						cause.printStackTrace();
					} finally {
						activeCount.decrementAndGet();
					}
				}
			} finally {
				if (threadCount.decrementAndGet() == 0 && isShutdown) {
					// get rid of potential left behind null-tasks (shutdown
					// signallers)
					taskQueue.clear();
				}
			}
		}
	}

	//

	private static final Manager GLOBAL_POOL_MANAGER = new Manager(10, 2500);

	private static class Manager implements Runnable {
		private final int minDelay, maxDelay;
		private final List<ThreadPool> pools;

		public Manager(int minDelay, int maxDelay) {
			this.minDelay = minDelay;
			this.maxDelay = maxDelay;
			this.pools = new CopyOnWriteArrayList<>();

			Thread thread = new Thread(this, this.getClass().getName());
			thread.setDaemon(true);
			thread.start();
		}

		public void monitor(ThreadPool pool) {
			pools.add(pool);
		}

		@Override
		public void run() {
			final double growRate = 1.25;
			final double shrinkRate = 0.75;

			int delay = minDelay;
			while (true) {
				HighLevel.sleep(delay);

				for (ThreadPool pool : pools) {
					if (pool.isShutdown) {
						pools.remove(pool);
						continue;
					}

					if (pool.taskQueue.isEmpty()) {
						continue;
					}

					// sleep a bit, and test whether we're still busy afterwards
					HighLevel.sleep(1);

					if (pool.taskQueue.isEmpty()) {
						continue;
					}

					// the queue still has a task, add a worker
					pool.requestWorker();
					delay *= shrinkRate;
				}

				delay = (int) (delay * growRate);
				if (delay < minDelay)
					delay = minDelay;
				if (delay > maxDelay)
					delay = maxDelay;
			}
		}
	}

	private static final long default_idle_timeout = 30_000L;

	private static final ThreadFactory default_thread_factory = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable task) {
			return new Thread(task);
		}
	};
}
