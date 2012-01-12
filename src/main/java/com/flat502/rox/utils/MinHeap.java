package com.flat502.rox.utils;

/**
 * A minimum binary heap implementation.
 * <p>
 * This heap implementation is keyed on long values.
 * It provides O(log n) inserts, O(C) remove minumum element,
 * O(N) remove arbitary element.
 * <p>
 * This implementation is unsynchronized. Concurrent access
 * from multiple threads should be controlled by the caller.
 */
public class MinHeap {
	private int growthIncrement;
	private int currentSize;
	private boolean isOrdered;
	private Element[] heap;

	/**
	 * Constructs a new MinimumBinaryHeap instance with an initial size of 10
	 * and a growth increment of 10.
	 */
	public MinHeap() {
		this(10, 10);
	}

	/**
	 * Constructs a new MinimumBinaryHeap instance the specified initial size
	 * and a growth increment of 10.
	 * @param initial_size
	 *        The initial size of the heap.
	 */
	public MinHeap(int initial_size) {
		this(initial_size, 10);
	}

	/**
	 * Constructs a new MinimumBinaryHeap instance the specified initial size
	 * and growth increment
	 * @param initial_size
	 *        The initial size of the heap.
	 * @param growth_step
	 *        The growth increment to use.
	 */
	public MinHeap(int initial_size, int growth_step) {
		this.growthIncrement = growth_step;
		this.isOrdered = true;
		this.currentSize = 0;

		heap = new Element[initial_size];
		heap[0] = new Element(Long.MIN_VALUE, null); // Set up the sentinel
	}

	/**
	 * @return
	 *        <code>true</code> if the heap is empty, <code>false</code>
	 *        if not.
	 */
	public boolean isEmpty() {
		return currentSize == 0;
	}

	/**
	 * @return
	 *        The number of elements stored in the heap.
	 */
	public int size() {
		return currentSize;
	}

	public void insert(long key, Object value) {
		if (currentSize + 1 == heap.length) {
			// Out of space, grow the heap
			Element[] new_heap = new Element[heap.length + growthIncrement];
			System.arraycopy(heap, 0, new_heap, 0, heap.length);
			heap = new_heap;
		}

		if (!isOrdered) {
			reorderHeap();
		}

		// Insert in the lowest left most position of the tree and
		// percolate up.
		int hole_idx = ++currentSize;
		while (key < heap[hole_idx / 2].key) {
			heap[hole_idx] = heap[hole_idx / 2];
			hole_idx /= 2;
		}
		heap[hole_idx] = new Element(key, value);
	}

	public void insertUnordered(long key, Object value) {
		if (currentSize + 1 == heap.length) {
			// Out of space, grow the heap
			Element[] new_heap = new Element[heap.length + growthIncrement];
			System.arraycopy(heap, 0, new_heap, 0, heap.length);
			heap = new_heap;
		}

		heap[++currentSize] = new Element(key, value);
		isOrdered = false;
	}

	public Object getSmallest() {
		if (isEmpty()) {
			throw new IllegalStateException("MinimumBinaryHeap is empty");
		}

		if (!isOrdered) {
			reorderHeap();
		}

		return heap[1].value;
	}

	public Object removeSmallest() {
		//
		// getSmallest() will reorder the heap if required
		//

		Object value = getSmallest();
		heap[1] = heap[currentSize--];
		percolateDown(1);
		return value;
	}

	public void removeKey(long key) {
		if (isEmpty()) {
			return;
		}

		int removals = 0;
		for (int i = 1; i <= currentSize; i++) {
			if (heap[i].key == key) {
				heap[i] = null;
				removals++;
			}
		}

		int idx = currentSize;
		while (removals > 0) {
			// Find next gap and shuffle everyone down.
			// Since we're shuffling left and it's more efficient
			// to scan right to left.
			if (heap[idx] == null) {
				System.arraycopy(heap, idx + 1, heap, idx, currentSize - idx);
				heap[currentSize--] = null;
				removals--;
			}

			idx--;
		}

		isOrdered = false;
	}

	/**
	 * This method makes use of the <code>equals()</code>
	 * method to locate entries in the heap that are to be
	 * removed.
	 * <P>
	 * This method does not reorder the heap
	 */
	public void removeValue(Object value) {
		if (isEmpty()) {
			return;
		}

		int removals = 0;
		for (int i = 1; i <= currentSize; i++) {
			if (heap[i].value.equals(value)) {
				heap[i] = null;
				removals++;
			}
		}

		int idx = currentSize;
		while (removals > 0) {
			// Find next gap and shuffle everyone down.
			// Since we're shuffling left and it's more efficient
			// to scan right to left.
			if (heap[idx] == null) {
				System.arraycopy(heap, idx + 1, heap, idx, currentSize - idx);
				heap[currentSize--] = null;
				removals--;
			}

			idx--;
		}

		isOrdered = false;
	}

	public void clear() {
		for (int i = 1; i <= currentSize; i++) {
			heap[i] = null;
		}
		currentSize = 0;
		isOrdered = true;
	}

	/**
	 * Reorders the heap so that the indicated index is at the left most
	 * position open in the bottom row of the tree.
	 */
	private void percolateDown(int hole_idx) {
		int child_idx;
		Element tmp = heap[hole_idx];

		while (hole_idx * 2 <= currentSize) {
			child_idx = hole_idx * 2;
			if (child_idx != currentSize && heap[child_idx + 1].key < heap[child_idx].key) {
				child_idx++;
			}
			if (heap[child_idx].key < tmp.key) {
				heap[hole_idx] = heap[child_idx];
			} else {
				break;
			}

			hole_idx = child_idx;
		}

		heap[hole_idx] = tmp;
	}

	/**
	 * Reestablishes order in the heap. This is an O(N) operation.
	 */
	private void reorderHeap() {
		for (int i = currentSize / 2; i > 0; i--) {
			percolateDown(i);
		}
		isOrdered = true;
	}

	private void dump(String s) {
		System.out.print(s);
		System.out.print("S ");
		for (int i = 1; i < heap.length; i++) {
			System.out.print((heap[i] != null) ? (heap[i].key + " ") : ("## "));
		}
		System.out.println();
	}

	private static class Element {
		private long key;
		private Object value;
	
		public Element(long key, Object value) {
			this.key = key;
			this.value = value;
		}
	
		public long getKey() {
			return key;
		}
	
		public Object getValue() {
			return value;
		}
	}

	public static void main(String[] args) {
		MinHeap bh = new MinHeap();
		for (int i = 0; i < 10; i++) {
			long k = 20 - i;
			bh.insert(k, String.valueOf(k));
		}
		bh.insert(15, "15(2)");
		bh.insert(15, "15(3)");

		bh.dump("");

		bh.removeKey(15);

		bh.dump("");

		while (!bh.isEmpty()) {
			System.out.println("Removed " + bh.removeSmallest());
		}

		bh.dump("");
	}
}