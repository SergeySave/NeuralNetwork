package sergeysav.neuralnetwork;

import java.io.Serializable;
import java.util.Arrays;
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
	private transient Function<Double, Double> activationFunction;
	private transient Function<Double, Double> derivativeFunction;
	
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
		this(numParents, Neuron::sigmoid, Neuron::sigmoidDerivative);
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
	private Neuron(int numParents, Function<Double, Double> activationFunction, Function<Double, Double> derivativeFunction) {
		//Set the weights as random values from -1 to 1
		weights = NeuralNetwork.rand.doubles(numParents, -1, 1).toArray();
		//Set the bias to 0
		bias = 0;
		//Set the activation function as the given function
		this.activationFunction = activationFunction;
		this.derivativeFunction = derivativeFunction;
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
	 * The derivative of the sigmoid function.
	 * 
	 * y = x * (1 - x)
	 * 
	 * @param x the input value into the derivative function
	 * @return the derivative of the sigmoid function
	 */
	public static double sigmoidDerivative(double x) {
		return x * (1-x);
	}
		
	/**
	 * Get the expected number of parent neurons to this neuron
	 * 
	 * @return the number of parent neurons
	 */
	public int getParentNeurons() {
		return weights.length;
	}
	
	/**
	 * Get the weights of this neuron
	 * 
	 * @return an array of the weights of this neuron
	 */
	public double[] getWeights() {
		return weights;
	}
	
	
	/**
	 * Set the weights of this neuron
	 * 
	 * @throws IllegalArgumentException when the length of the input array is not equal to the expected length
	 * 
	 * @param weights an array of the weights of this neuron
	 */
	public void setWeights(double[] weights) {
		if (weights.length != this.weights.length) throw new IllegalArgumentException("There are " + this.weights.length + " input neuron values when " + weights.length + " were expected.");
		this.weights = weights;
	}
	
	/**
	 * Get the bias parameter of this neuron
	 * 
	 * @return the bias parameter
	 */
	public double getBias() {
		return bias;
	}
	
	/**
	 * Set the bias parameter of the neuron
	 * 
	 * @param bias the bias parameter
	 */
	public void setBias(double bias) {
		this.bias = bias;
	}
	
	public Function<Double, Double> getDerivativeFunction() {
		return derivativeFunction;
	}
	
	@Override
	public String toString() {
		return "Neuron[weights=" + Arrays.toString(weights) + ",bias=" + bias + "" + "]";
	}
}
