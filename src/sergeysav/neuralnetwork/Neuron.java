package sergeysav.neuralnetwork;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A class representing a single neuron
 * 
 * @author sergeys
 * 
 */
public class Neuron implements Serializable {
	
	/**
	 * The Serial Version UID
	 * Used for Serialization
	 */
	private static final long serialVersionUID = -2054648138743362458L;
	
	//The activation function to use for this neuron
	private Function<Double, Double> activationFunction;
	
	//The weights to the previous layer of neurons
	private double[] weights;
	
	//The bias value for this neuron
	private double bias;
	
	/**
	 * Create a new Neuron with a specified number of parents.
	 * This neuron must always be called with an array of the same length as the number of parents.
	 * 
	 * This neuron will use the sigmoid activation function.
	 * 
	 * @see Neuron#sigmoid(double)
	 * @see Neuron#Neuron(int, Function)
	 * 
	 * @param numParents the number of parent nodes for this neuron
	 */
	public Neuron(int numParents) {
		this(numParents, Neuron::sigmoid);
	}
	
	/**
	 * Create a new Neuron with a specified number of parents.
	 * This neuron must always be called with an array of the same length as the number of parents.
	 * 
	 * This neuron will use the specified activation function.
	 * 
	 * @see Neuron#Neuron(int)
	 * 
	 * @param numParents the number of parent nodes for this neuron
	 * @param activationFunction the activation function to use
	 */
	public Neuron(int numParents, Function<Double, Double> activationFunction) {
		//Set the weights as random values from -1 to 1
		weights = NeuralNetwork.rand.doubles(numParents, -1, 2).toArray();
		//Set the bias to 0
		bias = 0;
		//Set the activation function as the given function
		this.activationFunction = activationFunction;
	}
	
	/**
	 * Get the output of this neuron with a given layer of neuron values as input
	 * 
	 * @throws IllegalArgumentException when the length of the input array is not equal to the expected length
	 * 
	 * @param prevLayer the outputs of the previous layer of neurons
	 * @return the activated value of this neuron
	 */
	public double getOutput(double... prevLayer) {
		//Throw exception if the number of parameters is not the same as the number of parent neurons 
		if (prevLayer.length != weights.length) throw new IllegalArgumentException("There are " + prevLayer.length + " input neuron values when " + weights.length + " were expected.");
		
		//Calculate the total value as bias + ∑(weight * value)
		double total = bias;
		for (int i = 0; i < prevLayer.length; i++) {
			total += weights[i] * prevLayer[i];
		}
		
		//Return the activated value of this total value
		return activationFunction.apply(total);
	}
	
	/**
	 * The sigmoid function of x.
	 * 
	 * y = 1/(1 + e^-x)
	 * 
	 * @param x the input x value to the sigmoid function
	 * @return the result of the calculation of the sigmoid function
	 */
	public static double sigmoid(double x) {
		//Calculate the value of the sigmoid function
		return 1d/(1d + Math.exp(-x));
	}
	
	/**
	 * The step function of x
	 * 
	 * @param x the input x value to the step function
	 * @return 1 if x is greater than or equal to 1. 0 if x is less than 1
	 */
	public static double step(double x) {
		//Calculate the value of the step function
		if (x >= 1) return 1;
		return 0;
	}
}
