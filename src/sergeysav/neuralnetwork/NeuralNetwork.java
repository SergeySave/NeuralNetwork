package sergeysav.neuralnetwork;

import java.io.Serializable;
import java.util.Random;

/**
 * An implementation of a feedforward neural network
 * 
 * Uses backpropogation in order to learn
 * 
 * Can be saved and loaded from a file thanks to serialization
 * 
 * @author sergeys
 *
 */
public class NeuralNetwork implements Serializable {

	/**
	 * The Serial Version UID
	 * Used for Serialization
	 */
	private static final long serialVersionUID = 1940228265084135859L;

	//A random object used for generating numbers
	public static transient final Random rand = new Random();

	//The neuron layers excluding the input layer
	private Neuron[][] layers;

	//The number of input neurons
	private int inputNeurons;

	/**
	 * Create a new neural network
	 * 
	 * @param neuronsPerLayer the count of neurons per layer. The first number will be the count of input neurons. The last number will be the count of output neurons.
	 */
	public NeuralNetwork(boolean stepFunctionLast, int... neuronsPerLayer) {
		long seed = rand.nextLong();
		//seed = -1698630836038324742L;
		//System.out.println(seed);
		rand.setSeed(seed);
		
		//Set the number of input neurons
		inputNeurons = neuronsPerLayer[0];

		//Create the array of neurons
		layers = new Neuron[neuronsPerLayer.length-1][0];

		//For each layer of neurons except the input layer
		for (int i = 1; i<neuronsPerLayer.length; i++) {
			//Set the number of neurons based on the value that was inputed
			layers[i-1] = new Neuron[neuronsPerLayer[i]];
			//For each neuron in this layer
			for (int j = 0; j<layers[i-1].length; j++) {
				//Put the neuron in the array and set it's number of parent neurons
				if (stepFunctionLast && i == neuronsPerLayer.length-1) {
					layers[i-1][j] = new Neuron(neuronsPerLayer[i-1]); //, Neuron::identity, Neuron::sigmoidDerivative
				} else {
					layers[i-1][j] = new Neuron(neuronsPerLayer[i-1]);
				}
			}
		}
	}

	/**
	 * Get the output neuron values for a given set of input neuron values
	 * 
	 * @param inputs the values of the input neurons
	 * @return the values of the output neurons
	 */
	public double[] testAll(double... inputs) {
		//Define an array that represents the last layer that was evaluated
		double[] lastLayer = inputs;

		//Loop through each layer of neurons
		for (int i = 0; i<layers.length; i++) {
			//Create an array representing the outputs of this layer
			double[] newLayer = new double[layers[i].length];

			//Loop through each neuron in this layer
			for (int j = 0; j<newLayer.length; j++) {
				//Evaluate the given neuron using the values of the previous layer
				newLayer[j] = layers[i][j].getOutput(lastLayer);
			}

			//Set the current layer as the last layer calculated
			lastLayer = newLayer;
		}

		//Return the output layer
		return lastLayer;
	}

	/**
	 * Gets the output value for a single neuron for a given set of input neuron values
	 * 
	 * @param outputNeuron the index of the neuron in the output layer to get the value of
	 * @param inputs the values of the input neurons
	 * @return the value of the output neuron at the given index
	 */
	public double testSingle(int outputNeuron, double... inputs) {
		//Calculate all of the values and return the desired value
		return testAll(inputs)[outputNeuron];

		//Performance can be improved by only calculating the value of desired neuron in the output layer
	}

	public int getMaxNeuron(double... inputs) {
		int maxIndex = -1;
		double maxVal = Double.MIN_VALUE;

		double[] outputs = testAll(inputs);
		for (int i = 0; i<outputs.length; i++) {
			if (outputs[i] > maxVal) {
				maxVal = outputs[i];
				maxIndex = i;
			}
		}

		return maxIndex;
	}

	public int getMinNeuron(double... inputs) {
		int minIndex = -1;
		double minVal = Double.MAX_VALUE;

		double[] outputs = testAll(inputs);
		for (int i = 0; i<outputs.length; i++) {
			if (outputs[i] < minVal) {
				minVal = outputs[i];
				minIndex = i;
			}
		}

		return minIndex;
	}

	public int[] getMaxMinNeurons(double... inputs) {
		int[] indicies = {-1,-1};
		double[] maxVals = new double[] {Double.MIN_VALUE, Double.MAX_VALUE};

		double[] outputs = testAll(inputs);
		for (int i = 0; i<outputs.length; i++) {
			if (outputs[i] > maxVals[0]) {
				maxVals[0] = outputs[i];
				indicies[0] = i;
			}
			if (outputs[i] < maxVals[1]) {
				maxVals[1] = outputs[i];
				indicies[1] = i;
			}
		}

		return indicies;
	}

	/**
	 * Get the number of input neurons
	 * 
	 * @return the number of input neurons
	 */
	public int getInputNeurons() {
		return inputNeurons;
	}

	/**
	 * Gets the neural data of this network
	 * 
	 * @return the two dimensional array of neurons
	 */
	public Neuron[][] getNeuralData() {
		return layers;
	}

	/**
	 * Set the neural data of this network
	 * 
	 * @throws IllegalArgumentException thrown if the number of input neurons expected by the first layer of the neurons in the data is not equal to the number of input neurons in the network
	 * 
	 * @param neurons the two dimensional array of neurons
	 */
	public void setNeuralData(Neuron[][] neurons) {
		if (neurons[0][0].getParentNeurons() != inputNeurons) new IllegalArgumentException("The inputted neurons expect " + neurons[0][0].getParentNeurons() + " input neurons. " + inputNeurons + " expected.");
		layers = neurons;
	}
	
	public void init() {
		//Loop through each layer of neurons
		for (int i = 0; i<layers.length; i++) {
			//Loop through each neuron in this layer
			for (int j = 0; j<layers[i].length; j++) {
				layers[i][j].init();
			}
		}
	}
}
