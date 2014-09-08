package net.indiespot.loadbalancer.util;

import java.util.*;

public class ByteList {
	private byte[] array;
	private int size;

	public ByteList() {
		this.array = new byte[16];
	}

	public void clear() {
		this.size = 0;
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public int size() {
		return this.size;
	}

	public byte removeLast() {
		return this.array[--this.size];
	}

	public void add(byte b) {
		if (this.size == this.array.length) {
			this.array = Arrays.copyOf(this.array, this.array.length * 2);
		}
		this.array[this.size++] = b;
	}

	public byte get(int index) {
		if (index >= this.size) {
			throw new IndexOutOfBoundsException();
		}
		return this.array[index];
	}

	public byte[] toArray() {
		return Arrays.copyOf(this.array, this.size);
	}

	public void fillArray(byte[] dst, int off, int len) {
		if (len != this.size) {
			throw new IllegalStateException();
		}
		System.arraycopy(this.array, 0, dst, off, len);
	}
}
