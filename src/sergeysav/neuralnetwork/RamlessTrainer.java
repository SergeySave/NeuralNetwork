package sergeysav.neuralnetwork;

import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * An implementation of a backpropogation neural network trainer where the data is not stored in ram when not needed
 * 
 * @author sergeys
 *
 */
public class RamlessTrainer {

	private double learningRate;
	private Function<Integer, double[]> trainingData;
	private int trainingSize;
	private Function<Integer, double[]> testingData;
	private int testingSize;

	private NeuralNetwork network;
	private double[][][] learningMomentum;
	
	private int[] indexShuffle;

	/**
	 * @param k
	 * @param trainingData
	 * @param testingData
	 */
	public RamlessTrainer(double k, Function<Integer, double[]> trainingData, int trainingSize, Function<Integer, double[]> testingData, int testingSize, NeuralNetwork network) {
		this.learningRate = k;
		this.trainingData = trainingData;
		this.trainingSize = trainingSize;
		this.testingData = testingData;
		this.testingSize = testingSize;
		this.network = network;
		learningMomentum = new double[network.getNeuralData().length][][];
		for (int i = 0; i<learningMomentum.length; i++) {
			learningMomentum[i] = new double[network.getNeuralData()[i].length][];
			for (int j = 0; j<learningMomentum[i].length; j++) {
				learningMomentum[i][j] = new double[network.getNeuralData()[i][j].getParentNeurons() + 1];
			}
		}
		indexShuffle = new int[trainingSize];
		for (int i = 1; i<indexShuffle.length; i++) {
			indexShuffle[i] = i;
		}
	}

	public TrainingResult train(double epsilon) {
		return train(epsilon, -1);
	}

	public TrainingResult train(double epsilon, int maxEpochs) {
		int epochs = 0;
		//Neuron[][] oldNeuralData = network.getNeuralData();
		double trainingError = calculateAverageError(trainingData, trainingSize);
		double testingError = 0;
		while ((maxEpochs > -1 ? epochs < maxEpochs : true) && trainingError > epsilon && testingError <= calculateAverageError(testingData, testingSize)) {
			//System.out.println(trainingError);
			testingError = calculateAverageError(testingData, testingSize);
			epochs++;
			//oldNeuralData = network.getNeuralData().clone();
			indexShuffle = shuffle(indexShuffle);
			for (int i = 0; i<trainingSize; i++) {
				final int iVal = i;
				performBackpropogation(trainingData.apply(indexShuffle[iVal]));
			}
			for (int i = 0; i<learningMomentum.length; i++) {
				for (int j = 0; j<learningMomentum[i].length; j++) {
					Neuron n = network.getNeuralData()[i][j];
					n.setBias(n.getBias() + learningMomentum[i][j][0]);
					learningMomentum[i][j][0] *= 0.9;
					for (int k = 1; k<learningMomentum[i][j].length; k++) {
						n.getWeights()[k-1] += learningMomentum[i][j][k];
						learningMomentum[i][j][k] *= 0.9;
					}
					//learningMomentum[i][j] = new double[network.getNeuralData()[i][j].getParentNeurons() + 1];
				}
			}
			trainingError = calculateAverageError(trainingData, trainingSize);
		}
		//network.setNeuralData(oldNeuralData);
		return new TrainingResult(trainingError, testingError, epochs, trainingError < epsilon);
	}

	private void performBackpropogation(double[] data) {
		Neuron[][] neuralData = network.getNeuralData();
		double[][] neuralOutputs = new double[network.getNeuralData().length][];
		double[][] neuralDeltas = new double[network.getNeuralData().length][];

		double[][] trial = splitArray(data, network.getInputNeurons());

		double[] input =  trial[0];
		double[] target = trial[1];

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
				learningMomentum[i][j][0] += learningRate * deltaWeight;
				for (int k = 0; k < neuron.getWeights().length; k++) {
					learningMomentum[i][j][k+1] += learningRate * (i > 0 ? neuralOutputs[i-1][k] : input[k]) * deltaWeight;
					//neuron.getWeights()[k] += learningRate * (i > 0 ? neuralOutputs[i-1][k] : input[k]) * deltaWeight;
				}
				//neuron.setBias(neuron.getBias() + learningRate * deltaWeight);
			}
		}
		//System.out.println(Arrays.deepToString(neuralOutputs));
		//return result;
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

	private double calculateAverageError(Function<Integer,double[]> dataSet, int length) {
		if (dataSet == null || length == 0) return 0;

		double totalError = 0;
		for (int i = 0; i<length; i++) {
			double[][] arr = splitArray(dataSet.apply(i), network.getInputNeurons());
			totalError += calculateError(arr[0], arr[1]);
		}

		return Math.sqrt(totalError/length);
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
}
