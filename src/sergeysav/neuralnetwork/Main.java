package sergeysav.neuralnetwork;

public class Main {

	public static void main(String[] args) {
		//Create a new neural network
		NeuralNetwork network = new NeuralNetwork(true, 2, 2, 1); //2 inputs, 2 hidden neurons, 1 output, and use the step function for the last layer
		
		//Teach the XOR function to the neural network
		network.learn(new double[][]{
				new double[]{0,0,0},
				new double[]{0,1,1},
				new double[]{1,0,1},
				new double[]{1,1,0}});
		
		//Test all of the outputs of the neural network
		System.out.println(network.testSingle(0,0,0));
		System.out.println(network.testSingle(0,0,1));
		System.out.println(network.testSingle(0,1,0));
		System.out.println(network.testSingle(0,1,1));
	}
}
