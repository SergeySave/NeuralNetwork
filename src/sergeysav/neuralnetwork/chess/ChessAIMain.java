package sergeysav.neuralnetwork.chess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
	private static BufferedWriter fileWriter;

	private static double LEARNING_K = 1e-5;
	private static double EPSILON = 1e-8;

	public static void main(String[] args) throws InterruptedException, ExecutionException, FileNotFoundException {
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}));

		try {
			fileWriter = new BufferedWriter(new FileWriter(new File("log.log")));
		} catch (IOException e) {
			e.printStackTrace();
		}

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

		ChessStore loaded = null;

		if (args.length > 0 && new File("backups/" + args[0]).exists()) {
			print("Loading Neural Network from file");
			loaded = ChessStore.load(new File("backups/" + args[0]));
		}

		NeuralNetwork network;
		ChessTrainer trainer;
		int startEpoch;
		
		Supplier<Stream<double[]>> trainingData = ()->{
			//Generate a stream of double arrays for the training data
			Collections.shuffle(trainingFiles);

			return trainingFiles.stream().flatMap((f)->{
				//Convert each file to a stream of transcripts
				List<Transcript> trans = new LinkedList<Transcript>();
				readTranscripts(f, trans);
				return trans.stream();
			}).flatMap((t)->StreamSupport.stream(t.spliterator(), false)).parallel();
		};
		
		Supplier<Stream<double[]>> testingData = ()->{
			//Generate a stream of double arrays for the testing data
			Collections.shuffle(testingFiles);

			return testingFiles.stream().flatMap((f)->{
				//Convert each file to a stream of transcripts
				List<Transcript> trans = new LinkedList<Transcript>();
				readTranscripts(f, trans);
				return trans.stream();
			}).flatMap((t)->StreamSupport.stream(t.spliterator(), false)).parallel();
		};

		if (loaded == null) {
			print("Creating Neural Network");
			//Create a new neural network
			network = new NeuralNetwork(true, 384, 259, 259, 134); //384 inputs, 2 layers of 259 neurons, 134 outputs (128 tiles + 6 upgrade types)

			print("Creating Network Trainer");
			trainer = new ChessTrainer(LEARNING_K, trainingData, testingData, network, EPSILON);
			
			startEpoch = 0;
		} else {
			network = loaded.network;
			
			network.init();
			
			trainer = loaded.trainer;
			
			trainer.init(LEARNING_K, trainingData, testingData, network, EPSILON);
			startEpoch = loaded.epoch;
		}

		ChessStore store = new ChessStore();
		store.network = network;
		store.trainer = trainer;
		store.epoch = startEpoch;

		print("Saving Backup " + store.epoch);
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
		String str = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS")) + "] " + arg;
		try {
			fileWriter.write(str+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(str);
	}
}
