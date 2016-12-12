package sergeysav.neuralnetwork.chess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;

import sergeysav.neuralnetwork.NeuralNetwork;
import sergeysav.neuralnetwork.chess.ChessTrainer.TrainingResult;

/*
 * 
 * Inputs 0-5 are type of piece at (0,0), Inputs (6-11) are type of piece at (1,0)
 * 
 */
public class ChessAIMain {
	
	private static double trainingRatio = 0.75;

	public static void main(String[] args) throws InterruptedException, ExecutionException, FileNotFoundException {
		print("Initializing games");
		File gamesDirectory = new File("games");

		List<File> trainingFiles = new ArrayList<File>();
		List<File> testingFiles = new ArrayList<File>();

		for (File pgnFile : gamesDirectory.listFiles()) {
			if (!pgnFile.isDirectory() && !pgnFile.isHidden() && pgnFile.getName().endsWith(".pgn")) {
				if (Math.random() <= trainingRatio) {
					trainingFiles.add(pgnFile);
				} else {
					testingFiles.add(pgnFile);
				}
			}
		}
		
		print("Creating Neural Network");
		//Create a new neural network
		NeuralNetwork network = new NeuralNetwork(true, 384, 512, 512, 512, 512, 134); //384 inputs, 4 layers of 512 neurons, 134 outputs (128 tiles + 6 upgrade types)

		print("Creating Network Trainer");
		ChessTrainer trainer = new ChessTrainer(0.2, ()->{
			//Generate a stream of double arrays for the training data
			Collections.shuffle(trainingFiles);
			
			return trainingFiles.stream().flatMap((f)->{
				//Convert each file to a stream of transcripts
				List<Transcript> trans = new LinkedList<Transcript>();
				readTranscripts(f, trans);
				return trans.stream();
			}).flatMap((t)->StreamSupport.stream(t.spliterator(), false)).parallel();
		}, ()->{
			//Generate a stream of double arrays for the testing data
			Collections.shuffle(testingFiles);
			
			return testingFiles.stream().flatMap((f)->{
				//Convert each file to a stream of transcripts
				List<Transcript> trans = new LinkedList<Transcript>();
				readTranscripts(f, trans);
				return trans.stream();
			}).flatMap((t)->StreamSupport.stream(t.spliterator(), false)).parallel();
		}, network, 1e-2);
		
		ChessStore store = new ChessStore();
		store.network = network;
		store.trainer = trainer;
		store.epoch = 0;
		
		print("Saving Backup 0");
		store.save();
		print("Calculating if next epoch needed\n");
		while (trainer.isNextEpochNeeded()) {
			print("Epoch " + (store.epoch+1) + " starting");
			trainer.performEpoch();
			print("Epoch completed");
			store.epoch++;
			print("Saving Backup " + store.epoch);
			store.save(); 
			print("Calculating if next epoch needed\n");
		}
		
		//I don't ever expect this code to be reached
		print("Training Completed");
		TrainingResult result = trainer.getResult();
		print("Took " + result.epochs + " epochs");
	}

	private static void readTranscripts(File file, List<Transcript> transcripts) {
		int transcript = 0;
		try (Scanner scan = new Scanner(file)) {
			Transcript t = null;
			String moveList = null;
			while(scan.hasNextLine()) {
				String line = scan.nextLine();

				if (line.startsWith("[Event \"")) {
					if (t != null) {
						transcript++;
						try {
							if (moveList != null) {
								populateTranscript(t, moveList);
								moveList = null;
								transcripts.add(t);
							}
						} catch (Exception e) {
							System.err.println("Error reading " + file.getName() + "#" + transcript);
							e.printStackTrace();
						}
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

	private static void populateTranscript(Transcript t, String moveList) {
		String old;
		do {
			old = moveList;
			moveList  = moveList.replaceAll("(?:\\([^\\(\\)]*\\))", ""); //Remove areas enclosed in parenthesis
			moveList  = moveList.replaceAll("(?:\\{[^\\{\\}]*\\})", ""); //Remove areas enclosed in braces
		} while (!old.equals(moveList));
		
		moveList = moveList.replaceAll("(?:\\d+\\.\\.\\.)", ""); //Remove all elipses
		
		//if (moveList.contains("+")) checks++;
		//if (moveList.contains("#")) checkmates++;
		
		String[] moved = moveList.split("\\d*[\\\\.]\\s*");

		ChessBoard board = new ChessBoard();
		LinkedList<String> moves = t.getMoves();

		for (int i = 1; i<moved.length; i++) {
			if (i == moved.length - 1) {
				String[] parts = moved[i].split("\\s+");
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
				String[] steps = moved[i].split("\\s+");
				String movew = board.getMoveConverted(steps[0], true);
				moves.add(movew);
				board.applyConvertedMove(movew);
				String moveb = board.getMoveConverted(steps[1], false);
				moves.add(moveb);
				board.applyConvertedMove(moveb);
			}
		}
		//games++;
		//ChessAIMain.moves += moves.size();
	}
	
	public static void print(String arg) {
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS")) + "] " + arg);
	}
}
