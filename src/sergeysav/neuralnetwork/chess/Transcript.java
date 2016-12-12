package sergeysav.neuralnetwork.chess;

import java.util.Iterator;
import java.util.LinkedList;

public class Transcript implements Iterable<double[]> {
	private LinkedList<String> moves = new LinkedList<String>();
	private String outcome = "ERROR";

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public LinkedList<String> getMoves() {
		return moves;
	}

	@Override
	public Iterator<double[]> iterator() {
		return new TranscriptIterator();
	}

	private class TranscriptIterator implements Iterator<double[]> {

		private Iterator<String> moveIterator;
		private ChessBoard board;
		private boolean whiteMoving;

		public TranscriptIterator() {
			moveIterator = moves.iterator();
			board = new ChessBoard();
			whiteMoving = true;
		}

		@Override
		public boolean hasNext() {
			return moveIterator.hasNext();
		}

		@Override
		public double[] next() {
			double[] output = new double[518]; // 6*8*8 input + 2*8*8+6 output

			String move = moveIterator.next();
			String[] parts = move.split(";");

			int fromRow = Integer.parseInt(parts[0].charAt(0) + "");
			int fromCol = Integer.parseInt(parts[0].charAt(1) + "");

			int toRow = Integer.parseInt(parts[1].charAt(0) + "");
			int toCol = Integer.parseInt(parts[1].charAt(1) + "");

			int piece = Integer.parseInt(parts[2]);
			int pieceTypeMove = Math.abs(piece);

			if (whiteMoving) { //Generate the data as if the white team is moving
				//Add the inputs to the array
				for (int i = 0; i<8; i++) {
					for (int j = 0; j<8; j++) {
						int pieceType = board.getPieceAt(i, j);
						if (pieceType == 7 || pieceType == -7) pieceType = 0;
						int absType = Math.abs(pieceType);
						if (absType > 0) {
							output[6*8*i + 6*j + absType - 1] = Math.signum(pieceType);
						}
					}
				}

				//Add the outputs to the array
				output[384 + fromRow * 8 + fromCol] = 1;
				output[448 + toRow * 8 + toCol] = 1;
				output[512 + pieceTypeMove - 1] = 1;
			} else { //Generate the data as if the black team is moving
				//Add the inputs to the array
				for (int i = 7; i>=0; i--) {
					for (int j = 0; j<8; j++) {
						int pieceType = board.getPieceAt(i, j);
						if (pieceType == 7 || pieceType == -7) pieceType = 0;
						int absType = Math.abs(pieceType);
						if (absType > 0) {
							output[6*8*i + 6*j + absType - 1] = -Math.signum(pieceType);
						}
					}
				}

				//Add the outputs to the array
				output[384 + (7-fromRow) * 8 + fromCol] = 1;
				output[448 + (7-toRow) * 8 + toCol] = 1;
				output[512 + pieceTypeMove - 1] = 1;
			}

			board.applyConvertedMove(move);

			whiteMoving = !whiteMoving;

			return output;
		}
	}
}
