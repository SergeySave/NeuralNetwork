package sergeysav.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A class containing stream utility methods
 * 
 * @author sergeys
 *
 */
public class StreamUtil {

	/**
	 * Splits a stream into equally sized batched portions
	 * 
	 * @param original the original input stream
	 * @param batchSize the size of each batch
	 * @param parallel should the output streams be parallel
	 * @return a stream that streams streams of the same type as the input stream
	 */
	public static <T> Stream<Stream<T>> batchStream(Stream<T> original, int batchSize, boolean parallel) {
		//Get the main spliterator of the input stream
		Spliterator<T> originalSplit = original.spliterator();

		/**
		 * A local class representing an iterator that returns streams of the same type as the input. 
		 * 
		 * @author sergeys
		 *
		 */
		class MainStreamIterator implements Iterator<Stream<T>> {
			//Does this iterator have more elements to return
			private boolean hasNextElement = true; 
			//A queue of Spliterators to be given out for usage
			private LinkedBlockingDeque<Spliterator<T>> spliteratorQueue = new LinkedBlockingDeque<>();
			
			/**
			 * Gets a spliterator for iterating over
			 * 
			 * @param maxSplits the maximum number of times that this will split the spliterator
			 * @return a new spliterator
			 */
			public synchronized Spliterator<T> getSpliterator(int maxSplits) {
				Spliterator<T> newSplit; //The spliterator that we will return after we finish processing
				
				//If we have nothing in our queue
				if (spliteratorQueue.isEmpty()) {
					//Get a new spliterator by splitting it off of the original spliterator
					newSplit = originalSplit.trySplit();
					//If we are no longer able to generate spliterators and we haven't encountered this before
					if (newSplit == null && hasNextElement) {
						//We have ran out of elements to return
						hasNextElement = false;
						//Our next spliterator is the original spliterator
						newSplit = originalSplit;
					}
				} else { //If we have something in our queue
					//Get if off of the queue
					newSplit = spliteratorQueue.remove();
				}
				
				//If the new spliterator is null (we have ran out of spliterators) then return null
				if (newSplit == null) return null;
				
				//Loops maxSplit number of times 
				for (int i = 0; i<maxSplits; i++) {
					//Splits off a new spliterator
					Spliterator<T> offSplit = newSplit.trySplit();
					//If this new spliteartor is null
					if (offSplit == null) {
						//Stop looping
						break;
					} else { //If it isn't null
						//Add the new spliterator to the queue
						spliteratorQueue.add(offSplit);
					}
				}
				
				//Return the spliterator
				return newSplit;
			}

			@Override
			public boolean hasNext() {
				//Returns if we have a next element
				return hasNextElement;
			}

			@Override
			public Stream<T> next() {
				//Create a new batched spliterator of the batched size
				BatchedSpliterator subIter = new BatchedSpliterator(batchSize);
				//Stream this spliterator using the parallel settings provided
				Stream<T> stream = StreamSupport.stream(subIter, parallel);
				//Return this new stream
				return stream;
			}

			/**
			 * An inner class representing a spliterator that will return a set amount of total elements over all of its splits
			 * 
			 * @author sergeys
			 *
			 */
			class BatchedSpliterator implements Spliterator<T> {
				private int elementsRemaining = 0; //The amount of items remaining that this spliterator can return
				private Spliterator<T> mySplit; //The current spliterator that it is taking elements from

				/**
				 * Creates a new batched spliterator using a given size
				 * 
				 * @param size
				 */
				protected BatchedSpliterator(int size) {
					this.elementsRemaining = size;
				}

				@Override
				public boolean tryAdvance(Consumer<? super T> action) {
					//If we shouldn't return any more elements
					if (elementsRemaining <= 0) {
						//If the current spliterator isn't null
						if (mySplit != null) {
							//Add the spliterator back to the queue
							spliteratorQueue.add(mySplit);
							mySplit = null;
						}
						//Fail to advance (signifies that we are done)
						return false;
					}
					
					//If we don't currently have a spliterator
					if (mySplit == null) {
						//Get a new spliterator with a maximum of one extra split
						mySplit = getSpliterator(1);
						//If the spliterator is null fail to advance
						if (mySplit == null) return false;
					}
					
					//Continue looping until the current input spliterator sucessfully advances using the action
					while (!mySplit.tryAdvance(action)) { 
						//Get a new spliterator with a maximum of one extra split
						mySplit = getSpliterator(1);
						// If the spliterator is null fail to advance
						if (mySplit == null) return false;
					}
					
					//Decrement to number of elements remaining
					--elementsRemaining;
					
					//We have successfully advanced
					return true;
				}

				@Override
				public Spliterator<T> trySplit() {
					//Determine the size of the new spliterator to be half (rounded down) of the current spliterator
					int newSize = elementsRemaining/2;
					//If this number is 0 then we cannot be split further
					if (newSize == 0) return null;
					
					//Decrement the number of elements remaining by the number that we gave to the new spliterator
					elementsRemaining -= newSize;
					//Create a new batched spliterator using this calculated size
					return new BatchedSpliterator(newSize);
				}

				@Override
				public long estimateSize() {
					return elementsRemaining;
				}

				@Override
				public int characteristics() {
					//I could do sized but that means we always 100% of the time know how much elements and i didn't
					//think that would be completely accurate
					return 0;
				}
			}			
		}

		//Stream this generating stream in series
		//This cannot be parallel because that wouldn't work but it is cheap so it shouldn't matter
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new MainStreamIterator(), 0), false);
	}

	/**
	 * This is a tester for the batch stream method
	 * @param args
	 */
	public static void main(String[] args) {//.peek(System.out::println)
		boolean noerror = true;
		int size = 100;
		int correct = 0;

		while (noerror) {
			List<Integer> ints = batchStream(IntStream.range(0, size).parallel().boxed(), size/20, true).map((s)->s.collect(Collectors.toList())).reduce(new ArrayList<Integer>(), (l1,l2)->{l1.addAll(l2); return l1;});
			if (ints.size() != size) {
				noerror = false;
			} else {
				correct++;
			}
			//System.out.println(ints);
			System.out.println(correct);
		}
		System.out.println(correct);
	}
}
