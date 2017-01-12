package sergeysav.stream;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A class containing stream utility methods
 * 
 * @author sergeys
 *
 */
public class StreamUtil {

	public static int count = 0;

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

		final int MAX_SPLITS = (int) (Math.ceil(Math.log(ForkJoinPool.getCommonPoolParallelism())/Math.log(2)) + 1);

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
			private PriorityBlockingQueue<Spliterator<T>> spliteratorQueue = new PriorityBlockingQueue<>(16, (s1, s2)->Long.compare(s1.estimateSize(), s2.estimateSize()));
			//An executor service for the Async Queue actions
			private ExecutorService executor = Executors.newCachedThreadPool();

			public MainStreamIterator() {
			}

			/**
			 * Gets a spliterator for iterating over
			 * 
			 * @param maxSplits the maximum number of times that this will split the spliterator
			 * @return a new spliterator
			 */
			public synchronized Spliterator<T> getSpliterator(int maxSplits, int size) {
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

				while (newSplit.estimateSize() > 1) {
					Spliterator<T> offSplit = newSplit.trySplit();
					if (offSplit == null) {
						//Stop looping
						break;
					} else { //If it isn't null
						//Add the new spliterator to the queue
						spliteratorQueue.add(offSplit);
					}
				}

				//Return the spliterator
				return newSplit instanceof SingleBufferWrappingSpliterator ? newSplit : new SingleBufferWrappingSpliterator(newSplit);
			}

			@Override
			public boolean hasNext() {
				//Returns if we have a next element
				return hasNextElement;
			}

			@Override
			public Stream<T> next() {
				//Create a new batched spliterator of the batched size
				BatchedSpliterator subIter = new BatchedSpliterator(batchSize, 0);
				//Stream this spliterator using the parallel settings provided
				Stream<T> stream = StreamSupport.stream(subIter, parallel);
				//Return this new stream
				return stream;
			}
			
			/**
			 * An inner class representing a spliterator that acts as a wrapper for another spliterator in order to manually cache the data one element at a time
			 * Unfortunately the input must not be able to return a null
			 * 
			 * @author sergeys
			 *
			 */
			class SingleBufferWrappingSpliterator implements Spliterator<T> {
				private Spliterator<T> wrapped; //The wrapped spliterator
				private boolean empty = false; //Is the input empty
				//A queue for the elements coming from the wrapped spliterator
				//This queue has an internal size of 0
				private SynchronousQueue<T> queue;
				

				/**
				 * Create a new Single Buffer Wrapping Spliterator from another spliterator
				 * 
				 * @param wrapping the spliterator to wrap
				 */
				public SingleBufferWrappingSpliterator(Spliterator<T> wrapping) {
					wrapped = wrapping;
				}

				@Override
				public boolean tryAdvance(Consumer<? super T> action) {
					//If we are already empty register that we failed to advance
					if (empty) return false;

					//If the queue doesn't exist
					if (queue == null) {
						//Make a new synchronous queue
						queue = new SynchronousQueue<>();
						//Add a thread to our executor using the following runnable
						executor.execute(()-> {
							//For each element in the input spliterator as we recieve them
							wrapped.forEachRemaining((o)->{
								try {
									//Put it into the queue if there is space
									queue.put(o);
								} catch (Exception e) {
									//Exception will be thrown if the thread is interupted
									e.printStackTrace();
								}
							});
							empty = true;
						});
					}

					//Loop until we get something or we are empty
					do {
						//The value that we recived from our input
						T val = null;
						try {
							//Get the next thing in the queue taking a maximum time of 10 ms
							//If we fail in that time val will be null
							val = queue.poll(10, TimeUnit.MILLISECONDS);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						//If we recieved a value
						if (val != null) {
							//Pass it along to the consumer
							action.accept(val);
							//And return that we succeeded
							return true;
						}
					} while (!empty);

					//Return that we failed (this means that queue was not yet empty when we started but it became empty in the process
					return false;
				}

				@Override
				public void forEachRemaining(Consumer<? super T> action) {
					//If we are empty do nothing
					if (empty) return;
					
					//If we haven't yet made our queue (so we have no thread putting things into it)
					if (queue == null) {
						//We don't need to use the queue at all
						//We are now empty (prevents other things from using this)
						empty = true;
						//Pipe everything into the consumer
						wrapped.forEachRemaining(action);
					} else {
						//If we already made a queue then continuously try to advance
						do {} while (tryAdvance(action));
					}
				}

				@Override
				public Spliterator<T> trySplit() {
					//If we don't yet have a queue
					if (queue == null) {
						//Try to split our input spliterator
						Spliterator<T> newWrapped = wrapped.trySplit();
						//If we got a new spliterator
						if (newWrapped != null) {
							//Return a new single buffer wrapping spliterator of this new spliterator
							return new SingleBufferWrappingSpliterator(newWrapped);
						}
					}
					
					//If we have a queue or unable to split our input fail to split
					return null;
				}

				@Override
				public long estimateSize() {
					//If we are empty return 0
					if (empty) return 0;
					//If we are not return the wrapped spliterator's estimated size
					return wrapped.estimateSize();
				}

				@Override
				public int characteristics() {
					//The characteristics are those of the input and non null
					return wrapped.characteristics() | NONNULL;
				}
			}


			/**
			 * An inner class representing a spliterator that will return a set amount of total elements over all of its splits
			 * 
			 * @author sergeys
			 *
			 */
			class BatchedSpliterator implements Spliterator<T> {
				private int elementsRemaining = 0; //The amount of items remaining that this spliterator can return
				private int numSplits = 0;
				private Spliterator<T> mySplit; //The current spliterator that it is taking elements from

				/**
				 * Creates a new batched spliterator using a given size
				 * 
				 * @param size
				 */
				protected BatchedSpliterator(int size, int numSplits) {
					count++;
					this.elementsRemaining = size;
					this.numSplits = numSplits;
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
						mySplit = getSpliterator(1, elementsRemaining);
						//If the spliterator is null fail to advance
						if (mySplit == null) return false;
					}

					//Continue looping until the current input spliterator sucessfully advances using the action
					while (!mySplit.tryAdvance(action)) { // 
						//Get a new spliterator with a maximum of one extra split
						mySplit = getSpliterator(1, elementsRemaining);
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
					//If we have already done enough splitting
					if (numSplits >= MAX_SPLITS) return null;

					//Determine the size of the new spliterator to be half (rounded down) of the current spliterator
					int newSize = elementsRemaining/2;
					//If this number is 0 then we cannot be split further
					if (newSize == 0) return null;

					//Decrement the number of elements remaining by the number that we gave to the new spliterator
					elementsRemaining -= newSize;

					numSplits++;

					//Create a new batched spliterator using this calculated size
					return new BatchedSpliterator(newSize, numSplits);
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
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new MainStreamIterator(), 0), false).onClose(original::close);
	}
}
