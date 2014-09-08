package net.indiespot.loadbalancer.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SimpleBlockingQueue<T> implements Iterable<T> {
	private static final Object NULL_VALUE = new Object();

	private final BlockingQueue<T> backing;
	private final int capacity;

	public SimpleBlockingQueue() {
		this(new LinkedBlockingQueue<T>(), Integer.MAX_VALUE);
	}

	public SimpleBlockingQueue(int cap) {
		this(new ArrayBlockingQueue<T>(cap), cap);
	}

	private SimpleBlockingQueue(BlockingQueue<T> backing, int capacity) {
		this.backing = backing;
		this.capacity = capacity;
	}

	//

	public void clear() {
		this.backing.clear();
	}

	public boolean isEmpty() {
		return this.backing.isEmpty();
	}

	public int size() {
		return this.backing.size();
	}

	public int remaining() {
		return this.capacity - this.size();
	}

	public int capacity() {
		return this.capacity;
	}

	@Override
	public Iterator<T> iterator() {
		return this.backing.iterator();
	}

	public void put(T item) {
		item = wrap(item);

		for (;;) {
			try {
				this.backing.put(item);

				break;
			} catch (InterruptedException exc) {
				exc.printStackTrace();
				continue;
			}
		}
	}

	public boolean offer(T item) {
		item = wrap(item);

		return this.backing.offer(item);
	}

	public boolean offer(T item, long ms) {
		item = wrap(item);

		try {
			return this.backing.offer(item, ms, TimeUnit.MILLISECONDS);
		} catch (InterruptedException exc) {
			return false;
		}
	}

	public T peek() {
		return unwrap(this.backing.peek());
	}

	public T poll() {
		return unwrap(this.backing.poll());
	}

	public T poll(long ms) {
		return unwrap(this.pollWrapped(ms));
	}

	T pollWrapped(long ms) {
		try {
			return this.backing.poll(ms, TimeUnit.MILLISECONDS);
		} catch (InterruptedException exc) {
			exc.printStackTrace();
			return null;
		}
	}

	public T take() {
		for (;;) {
			try {
				return unwrap(this.backing.take());
			} catch (InterruptedException exc) {
				exc.printStackTrace();
				continue;
			}
		}
	}

	public Iterable<T> poller() {

		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {

					@Override
					public boolean hasNext() {
						return this.current() != null;
					}

					@Override
					public T next() {
						return this.consume();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

					//

					private T wrappedNext = null;

					private T current() {
						if (this.wrappedNext == null) {
							this.wrappedNext = SimpleBlockingQueue.this.backing.poll();
						}
						return this.wrappedNext;
					}

					private T consume() {
						if (this.current() == null) {
							throw new NoSuchElementException();
						}

						T next = unwrap(this.wrappedNext);
						this.wrappedNext = null;
						return next;
					}
				};
			}
		};
	}

	public Iterable<T> poller(long timeout) {
		if (timeout > 1000L) {
			throw new IllegalStateException();
		}

		final long end = System.currentTimeMillis() + timeout;

		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {

					@Override
					public boolean hasNext() {
						return this.current() != null;
					}

					@Override
					public T next() {
						return this.consume();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

					//

					private T wrappedNext = null;

					private T current() {
						if (this.wrappedNext == null) {
							long rem = end - System.currentTimeMillis();
							if (rem > 0) {
								this.wrappedNext = pollWrapped(rem);
							}
						}
						return this.wrappedNext;
					}

					private T consume() {
						if (this.current() == null) {
							throw new NoSuchElementException();
						}

						T next = unwrap(this.wrappedNext);
						this.wrappedNext = null;
						return next;
					}

				};
			}
		};
	}

	@SuppressWarnings("unchecked")
	T wrap(T item) {
		return (item == null) ? (T) NULL_VALUE : item;
	}

	T unwrap(T item) {
		return (item == NULL_VALUE) ? null : item;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[size=" + this.size() + "]";
	}
}
