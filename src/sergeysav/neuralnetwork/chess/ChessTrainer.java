package sergeysav.neuralnetwork.chess;

import java.io.Serializable;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import sergeysav.neuralnetwork.NeuralNetwork;
import sergeysav.neuralnetwork.Neuron;
import sergeysav.stream.StreamUtil;

/**
 * An implementation of a backpropogation neural network trainer for the chess games
 * 
 * @author sergeys
 *
 */
public class ChessTrainer implements Serializable {

	private static final long serialVersionUID = 3561024763642610745L;

	private transient double learningRate;
	private transient Supplier<Stream<double[]>> trainingData;
	//private transient int trainingSize;
	private transient Supplier<Stream<double[]>> testingData;
	private transient double epsilon;
	//private transient int testingSize;

	private double lastTestingError = Double.MAX_VALUE;

	private transient NeuralNetwork network;
	private double[][][] learningMomentum;
	private int epochs = 0;

	public ChessTrainer(double k, Supplier<Stream<double[]>> trainingData, Supplier<Stream<double[]>> testingData, NeuralNetwork network, double epsilon) {
		init(k, trainingData, testingData, network, epsilon);
		learningMomentum = generateMomentumArr();
	}

	private double[][][] generateMomentumArr() {
		double[][][] m = new double[network.getNeuralData().length][][];
		for (int i = 0; i<m.length; i++) {
			m[i] = new double[network.getNeuralData()[i].length][];
			for (int j = 0; j<m[i].length; j++) {
				m[i][j] = new double[network.getNeuralData()[i][j].getParentNeurons() + 1];
			}
		}
		return m;
	}

	public void init(double k, Supplier<Stream<double[]>> trainingData, Supplier<Stream<double[]>> testingData, NeuralNetwork network, double epsilon) {
		this.learningRate = k;
		this.trainingData = trainingData;
		this.testingData = testingData;
		this.network = network;
		this.epsilon = epsilon;
	}

	public boolean isNextEpochNeeded() {
		return true;
		/*
		double err = calculateAverageError(trainingData);
		ChessAIMain.print("Training Error: " + err);
		testingCheck(); //Just used to calculate and print
		return err > epsilon;*/
	}

	private boolean testingCheck() {
		double e = calculateAverageError(testingData);
		ChessAIMain.print("Testing Error: " + e);
		boolean b = lastTestingError > e;
		lastTestingError = e;
		return b;
	}

	/**
	 * Is next epoch needed MUST ALWAYS be called before this
	 */
	public void performEpoch(Runnable backup) {
		epochs++;
		
		int batchSize = 1;
		double actualLearningRate = learningRate/batchSize;
		
		trainingData.get().sequential().forEach((data)->{
			BackpropData bp = performBackpropogation(data);
			ChessAIMain.print("Training Error Estimate: "+Math.sqrt(bp.err/batchSize), true, true);
			
			//Do learning
			for (int i = 0; i<bp.val.length; i++) {
				for (int j = 0; j<bp.val[i].length; j++) {
					Neuron n = network.getNeuralData()[i][j];
					n.setBias(n.getBias() + actualLearningRate*bp.val[i][j][0]);
					for (int k = 1; k<bp.val[i][j].length; k++) {
						n.getWeights()[k-1] += actualLearningRate*bp.val[i][j][k];
					}
				}
			}

			//Run the backup code
			backup.run();
		});
		
		
		/*
		
		int batchSize = 100;
		double actualLearningRate = learningRate/batchSize;
		double friction = 0.9;

		//Batch the input data into batches
		StreamUtil.batchStream(trainingData.get(), batchSize, true).forEach((s)->{
			//Calculate the momentum given by these batches
			BackpropData bpval = s.map(this::performBackpropogation).reduce(generateMomentumBP(), this::sumBPData, this::sumBPData);

			ChessAIMain.print("Training Error Estimate: "+Math.sqrt(bpval.err/batchSize), true, true);
			
			learningMomentum = sumArray3(bpval.val, learningMomentum);

			//Apply the learning momentum
			for (int i = 0; i<learningMomentum.length; i++) {
				for (int j = 0; j<learningMomentum[i].length; j++) {
					Neuron n = network.getNeuralData()[i][j];
					n.setBias(n.getBias() + actualLearningRate*learningMomentum[i][j][0]);
					learningMomentum[i][j][0] *= friction;
					for (int k = 1; k<learningMomentum[i][j].length; k++) {
						n.getWeights()[k-1] += actualLearningRate*learningMomentum[i][j][k];
						learningMomentum[i][j][k] *= friction;
					}
				}
			}

			//Run the backup code
			backup.run();
		});*/
	}

	private double[][][] sumArray3(double[][][] a1, double[][][] a2) {
		double[][][] result = new double[a1.length][][];
		for (int i = 0; i<result.length; i++) {
			result[i] = new double[a1[i].length][];
			for (int j = 0; j<result[i].length; j++) {
				result[i][j] = new double[a1[i][j].length];
				for (int k = 0; k<result[i][j].length; k++) {
					result[i][j][k] = a1[i][j][k] + a2[i][j][k];
				}
			}
		}
		return result;
	}

	private BackpropData generateMomentumBP() {
		BackpropData result = new BackpropData();
		result.val = generateMomentumArr();
		result.count = 0;
		result.err = 0;
		return result;
	}

	private BackpropData sumBPData(BackpropData a1, BackpropData a2) {
		BackpropData result = new BackpropData();
		result.val = sumArray3(a1.val, a2.val);
		result.count = a1.count + a2.count;
		result.err = a1.err + a2.err;
		return result;
	}

	public TrainingResult getResult() {
		return new TrainingResult(calculateAverageError(trainingData), calculateAverageError(testingData), epochs, calculateAverageError(trainingData) <= epsilon);
	}

	private BackpropData performBackpropogation(double[] data) {
		Neuron[][] neuralData = network.getNeuralData();
		double[][] neuralOutputs = new double[network.getNeuralData().length][];
		double[][] neuralDeltas = new double[network.getNeuralData().length][];

		double[][] trial = splitArray(data, network.getInputNeurons());

		double[] input =  trial[0];
		double[] target = trial[1];

		//double[][][] result = generateMomentumArr();
		BackpropData result = new BackpropData();
		result.val = generateMomentumArr();
		result.count = 1;

		{
			double[] lastLayer = trial[0];
			for (int i = 0; i<neuralOutputs.length; i++) {
				neuralOutputs[i] = new double[network.getNeuralData()[i].length];
				for (int j = 0; j<neuralOutputs[i].length; j++) {
					neuralOutputs[i][j] = neuralData[i][j].getOutput(lastLayer);
				}
				lastLayer = neuralOutputs[i];
			}
		}
		
		for (int i = 0; i<target.length; i++) {
			double e = neuralOutputs[neuralOutputs.length-1][i] - target[i];
			result.err += e*e;
		}
		result.err /= target.length;
		result.err /= target.length;

		//double[][][] result = new double[network.getNeuralData().length][][];

		for (int i = neuralOutputs.length-1; i>=0; i--) {
			//result[i] = new double[network.getNeuralData()[i].length][];
			neuralDeltas[i] = new double[network.getNeuralData()[i].length];
			for (int j = 0; j<neuralOutputs[i].length; j++) {
				//result[i][j] = new double[network.getNeuralData()[i][j].getParentNeurons() + 1];
				Neuron neuron = neuralData[i][j];
				double output = neuralOutputs[i][j];
				double deltaWeight;
				if (i == neuralOutputs.length-1) { //Last Hidden -> Output Layer Connection Delta Weight
					deltaWeight = neuron.getDerivativeFunction().apply(output) * (target[j] - output); //Derivative of error squared (Chain rule). 2 ignored b/c of later multiplied constant
				} else { //Layer -> Hidden Layer Connection Delta Weight
					final int iVal = i;
					final int jVal = j;
					double sum = IntStream.range(0, neuralOutputs[i+1].length).mapToDouble((idx)->neuralData[iVal+1][idx].getWeights()[jVal] * neuralDeltas[iVal+1][idx]).sum();
					deltaWeight = neuron.getDerivativeFunction().apply(output) * sum;
				}
				neuralDeltas[i][j] = deltaWeight;
				//New weight = oldWeight + k * output(source) * deltaWeight(thisNode)
				result.val[i][j][0] += deltaWeight;
				for (int k = 0; k < neuron.getWeights().length; k++) {
					result.val[i][j][k+1] += (i > 0 ? neuralOutputs[i-1][k] : input[k]) * deltaWeight;
					//neuron.getWeights()[k] += learningRate * (i > 0 ? neuralOutputs[i-1][k] : input[k]) * deltaWeight;
				}
				//neuron.setBias(neuron.getBias() + learningRate * deltaWeight);
			}
		}
		//System.out.println(Arrays.deepToString(neuralOutputs));
		return result;
	} 
	//Output neurons: constant * FromOutput * error
	//Hidden neurons: constant * FromOutput * error

	private double[][] splitArray(double[] arr, int lengthFirst) {
		double[][] output = new double[][] {new double[lengthFirst], new double[arr.length-lengthFirst]};

		System.arraycopy(arr, 0, output[0], 0, lengthFirst);
		System.arraycopy(arr, lengthFirst, output[1], 0, arr.length - lengthFirst);

		return output;
	}

	//Basically Standard Deviation
	private double calculateError(double[] inputs, double[] desiredOutputs) {
		double[] outputs = network.testAll(inputs);
		double error = 0;

		for (int i = 0; i<outputs.length; i++) {
			double err = outputs[i] - desiredOutputs[i];
			error += (err*err);
		}
		
		return error/outputs.length/outputs.length;
	}

	private double calculateAverageError(Supplier<Stream<double[]>> dataSet) {
		if (dataSet == null) return 0;

		TempData finalDataVal = dataSet.get().reduce(new TempData(), (td, d)->{
			TempData newData = new TempData();
			newData.count = td.count + 1;
			double[][] arr = splitArray(d, network.getInputNeurons());
			newData.err = calculateError(arr[0], arr[1]) + td.err;
			return newData;
		}, (a, b)->{
			TempData newData = new TempData();
			newData.count = a.count + b.count;
			newData.err = a.err + b.err;
			return newData;
		});

		if (finalDataVal.count == 0) return 0;

		return Math.sqrt(finalDataVal.err/finalDataVal.count);
	} 

	public static int[] shuffle(int[] arr) {
		for (int i = 0; i<arr.length; i++) {
			int x = NeuralNetwork.rand.nextInt(arr.length);
			int temp = arr[x];
			arr[x] = arr[i];
			arr[i] = temp;
		}

		return arr;
	}

	public static class TrainingResult {
		public final double trainingDataError;
		public final double testingDataError;
		public final int epochs;
		public final boolean success;

		/**
		 * Create a new TrainingResult object
		 * 
		 * @param trainingDataError the fraction error in the training data
		 * @param testingDataError the fraction error in the testing data 
		 * @param epochs the number of epochs that it took to train the network
		 */
		public TrainingResult(double trainingDataError, double testingDataError, int epochs, boolean success) {
			this.trainingDataError = trainingDataError;
			this.testingDataError = testingDataError;
			this.epochs = epochs;
			this.success = success;
		}

		@Override
		public String toString() {
			return "Training Result[err= " + trainingDataError + " test= " + testingDataError + " e= " + epochs + " success= " + success + "]";
		}
	}

	private static class TempData {
		public double err;
		public int count;
	}

	private static class BackpropData {
		public double[][][] val;
		public double err;
		public int count;
	}
}
