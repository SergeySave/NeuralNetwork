package sergeysav.neuralnetwork.chess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class ChessAIMain {

	public static void main(String[] args) throws InterruptedException, ExecutionException, FileNotFoundException {
		File gamesDirectory = new File("games");

		List<Transcript> transcripts = new LinkedList<Transcript>();

		for (File pgnFile : gamesDirectory.listFiles()) {
			if (!pgnFile.isDirectory() && !pgnFile.isHidden() && pgnFile.getName().endsWith(".pgn")) {
				readTranscripts(pgnFile, transcripts);
				System.out.println(pgnFile.getName());
			}
		}
		/*
		//Create a new neural network
		NeuralNetwork network = new NeuralNetwork(true, 384, 512, 512, 512, 512, 128); //384 inputs, 4 layers of 512 neurons, 128 outputs

		//Training data for the network
		double[][] trainingData = null;

		//Testing data for the network
		double[][] testingData = null;

		//Teach the XOR function to the neural network
		Trainer trainer = new Trainer(0.2, trainingData, testingData, network);

		//Train the network
		TrainingResult result = trainer.train(1e-2, 100000);

		//Print the error
		System.out.println(result);*/
	}

	private static void readTranscripts(File file, List<Transcript> transcripts) {
		try (Scanner scan = new Scanner(file)) {
			Transcript t = null;
			String moveList = null;
			while(scan.hasNextLine()) {
				String line = scan.nextLine();

				if (line.startsWith("[Event \"")) {
					if (t != null) {
						populateTranscript(t, moveList);
						moveList = null;
						transcripts.add(t);
					}
					t = new Transcript();
				}
				if (line.startsWith("1.")) {
					moveList = "";
				}
				if (moveList != null) moveList += line + " ";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int a = 0;
	
	private static void populateTranscript(Transcript t, String moveList) {
		String[] moved = moveList.split("\\d*[\\\\.]\\s*");
		
		ChessBoard board = new ChessBoard();
		LinkedList<String> moves = t.getMoves();
		
		for (int i = 1; i<moved.length; i++) {
			if (i == moved.length - 1) {
				String[] parts = moved[i].split(" ");
				if (parts.length > 2) {
					String movew = board.getMoveConverted(parts[0], true);
					moves.add(movew);
					board.applyConvertedMove(movew);
					String moveb = board.getMoveConverted(parts[1], false);
					moves.add(moveb);
					board.applyConvertedMove(moveb);
					t.setOutcome(parts[2]);
				} else {
					String movew = board.getMoveConverted(parts[0], true);
					moves.add(movew);
					board.applyConvertedMove(movew);
					t.setOutcome(parts[1]);
				}
			} else {
				String[] steps = moved[i].split(" ");
				String movew = board.getMoveConverted(steps[0], true);
				moves.add(movew);
				board.applyConvertedMove(movew);
				String moveb = board.getMoveConverted(steps[1], false);
				moves.add(moveb);
				board.applyConvertedMove(moveb);
			}
		}
		a++;
		System.out.println(a + " " + moves);
	}
}
