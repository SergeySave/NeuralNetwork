package sergeysav.neuralnetwork.chess;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class FileParsingTester {

	public static void main(String[] args) {
		File gamesDirectory = new File("");
		
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Filename");
		
		while (true) {
			String line = scan.nextLine();
			if (line.length() <= 1) {
				break;
			}
			File f = new File(gamesDirectory.getAbsolutePath() + "/" + line);
			List<Transcript> trans = new LinkedList<Transcript>();
			readTranscripts(f, trans);
			System.out.println(trans.stream());
		}
		
		scan.close();
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
							System.err.println("Move list: " + moveList);
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
}
