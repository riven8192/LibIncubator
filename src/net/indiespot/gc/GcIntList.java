package net.indiespot.gc;

import java.util.Arrays;

public class GcIntList {
	private int[] data;
	private int size;

	public GcIntList() {
		this.data = new int[4];
	}

	public void add(int value) {
		if (this.size == this.data.length) {
			this.data = Arrays.copyOf(this.data, this.data.length * 2);
		}
		this.data[size++] = value;
	}

	public int remove(int index) {
		if (index < 0 || index >= this.size) {
			throw new IndexOutOfBoundsException();
		}

		int value = this.data[index];
		System.arraycopy(this.data, index + 1, this.data, index, this.size - index - 1);
		this.size -= 1;
		return value;
	}
	
	public int removeLast(){
		if(this.size==0){
			throw new IndexOutOfBoundsException();
		}
		
		return this.data[--this.size];
	}

	public void set(int index, int value) {
		if (index < 0 || index >= this.size) {
			throw new IndexOutOfBoundsException();
		}
		this.data[index] = value;
	}

	public int get(int index) {
		if (index < 0 || index >= this.size) {
			throw new IndexOutOfBoundsException();
		}
		return this.data[index];
	}

	public int size() {
		return this.size;
	}
}
