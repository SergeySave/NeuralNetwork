package sergeysav.neuralnetwork;

import java.io.Serializable;
import java.util.Random;
import java.util.function.Function;

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

	/**
	 * Create a new neural network
	 * 
	 * @param neuronsPerLayer the count of neurons per layer. The first number will be the count of input neurons. The last number will be the count of output neurons.
	 */
	public NeuralNetwork(boolean lastLayerStep, int... neuronsPerLayer) {
		//Create the array of neurons
		layers = new Neuron[neuronsPerLayer.length-1][0];

		//For each layer of neurons except the input layer
		for (int i = 1; i<neuronsPerLayer.length; i++) {
			//Set the number of neurons based on the value that was inputed
			layers[i-1] = new Neuron[neuronsPerLayer[i-1]];
			//For each neuron in this layer
			for (int j = 0; j<layers[i-1].length; j++) {
				//If this is the last layer use the step function if the feature was requested
				if (lastLayerStep && i == neuronsPerLayer.length-1) {
					layers[i-1][j] = new Neuron(neuronsPerLayer[i-1], Neuron::step);
				} else {
					//Put the neuron in the array and set it's number of parent neurons
					layers[i-1][j] = new Neuron(neuronsPerLayer[i-1]);
				}
			}
		}
	}

	/**
	 * Create a new neural network
	 * 
	 * @param neuronsPerLayer the count of neurons per layer. The first number will be the count of input neurons. The last number will be the count of output neurons.
	 * @param activationFunctionPerLayer the activation function for each layer. The first layer may be null. It will not be used for anything.
	 */
	public NeuralNetwork(int[] neuronsPerLayer, Function<Double, Double>[] activationFunctionPerLayer) {
		//Create the array of neurons
		layers = new Neuron[neuronsPerLayer.length-1][0];

		//For each layer of neurons except the input layer
		for (int i = 1; i<neuronsPerLayer.length; i++) {
			//Set the number of neurons based on the value that was inputed
			layers[i-1] = new Neuron[neuronsPerLayer[i-1]];
			//For each neuron in this layer
			for (int j = 0; j<layers[i-1].length; j++) {
				//Put the neuron in the array and set it's number of parent neurons
				layers[i-1][j] = new Neuron(neuronsPerLayer[i-1], activationFunctionPerLayer[i]);
			}
		}
	}

	/**
	 * Teach the neural network by performing backpropogation on a set of training data
	 * 
	 * @param data the set of training data
	 */
	public void learn(double[][] data) {
		// TODO Program a computer to learn to do stuff n things
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
}
