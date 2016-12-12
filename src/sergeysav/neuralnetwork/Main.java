package sergeysav.neuralnetwork;

import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;

import sergeysav.neuralnetwork.RamlessTrainer.TrainingResult;

public class Main {

	public static void main(String[] args) throws InterruptedException, ExecutionException, FileNotFoundException {

		int successes = 0;
		int trials = 0;

		while (true) {
			//Create a new neural network
			NeuralNetwork network = new NeuralNetwork(true, 2, 2, 1); //2 inputs, 2 hidden neurons, 1 output, and use the step function for the last layer

			//Training data for the network
			double[][] trainingData = new double[][] {
				new double[] {0, 0, 0.1},
				new double[] {0, 1, 0.9}, //0.1 and 0.9 used instead of 0 and 1 so that the solution is within the range of the activation function
				new double[] {1, 0, 0.9},
				new double[] {1, 1, 0.1}
			};

			//Testing data for the network
			double[][] testingData = null;

			//Teach the XOR function to the neural network
			//Trainer trainer = new Trainer(0.2, trainingData, testingData, network);
			RamlessTrainer trainer = new RamlessTrainer(0.2, (i)->trainingData[i], trainingData.length, null, 0, network);

			//Train the network
			TrainingResult result = trainer.train(1e-2, 100000);
			
			if (result.success) successes++;
			trials++;
			System.out.println((double)successes / trials);
		}

		//Print the error
		//System.out.println(result);

		//Test all of the outputs of the neural network
		//System.out.println(step(network.testSingle(0,0,0)));
		//System.out.println(step(network.testSingle(0,0,1)));
		//System.out.println(step(network.testSingle(0,1,0)));
		//System.out.println(step(network.testSingle(0,1,1)));
	}

	public static double step(double x) {
		return x > 0.5 ? 1 : 0;
	}
}
