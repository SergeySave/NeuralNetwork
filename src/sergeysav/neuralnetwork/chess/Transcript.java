package sergeysav.neuralnetwork.chess;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;

public class Transcript {
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

	public Spliterator<double[]> spliterator() {
		return new TranscriptSpliterator();
	}

	private class TranscriptSpliterator implements Spliterator<double[]> {
		private Queue<double[]> outputQueue;
		private Iterator<String> moveIterator;
		private int sizeLeft;
		private ChessBoard board;
		private boolean whiteMoving;

		public TranscriptSpliterator() {
			outputQueue = new LinkedList<>();
			moveIterator = moves.iterator();
			board = new ChessBoard();
			whiteMoving = true;
			sizeLeft = moves.size();
		}

		@Override
		public boolean tryAdvance(Consumer<? super double[]> action) {
			if (moveIterator.hasNext()) {
				if (outputQueue.isEmpty()) {
					String move = moveIterator.next();
					String[] parts = move.split(";");

					int fromRow = Integer.parseInt(parts[0].charAt(0) + "");
					int fromCol = Integer.parseInt(parts[0].charAt(1) + "");

					int toRow = Integer.parseInt(parts[1].charAt(0) + "");
					int toCol = Integer.parseInt(parts[1].charAt(1) + "");

					int piece = Integer.parseInt(parts[2]);
					int pieceTypeMove = Math.abs(piece);
					
					addData(board, fromRow, fromCol, toRow, toCol, pieceTypeMove);/*
					if (!(pieceTypeMove == 6 && Math.abs(board.getPieceAt(toRow, toCol)) == 3)) {
						if (fromRow < 7 && toRow < 7) {
							ChessBoard b = new ChessBoard(board);
							b.transpose(1, 0);
							if (b.isLegalMove((fromRow+1) + "" + fromCol, (toRow+1) + "" + toCol))
								addData(b, fromRow+1, fromCol, toRow+1, toCol, pieceTypeMove);
						}
						if (fromRow > 0 && toRow > 0) {
							ChessBoard b = new ChessBoard(board);
							b.transpose(-1, 0);
							if (b.isLegalMove((fromRow-1) + "" + fromCol, (toRow-1) + "" + toCol))
								addData(b, fromRow-1, fromCol, toRow-1, toCol, pieceTypeMove);
						}
						if (fromCol < 7 && toCol < 7){
							ChessBoard b = new ChessBoard(board);
							b.transpose(0, 1);
							if (b.isLegalMove((fromRow) + "" + (fromCol+1), (toRow) + "" + (toCol+1)))
								addData(b, fromRow, fromCol+1, toRow, toCol+1, pieceTypeMove);
						}
						if (fromCol > 0 && toCol > 0){
							ChessBoard b = new ChessBoard(board);
							b.transpose(0, -1);
							if (b.isLegalMove((fromRow) + "" + (fromCol-1), (toRow) + "" + (toCol-1)))
								addData(b, fromRow, fromCol-1, toRow, toCol-1, pieceTypeMove);
						}
					}*/

					board.applyConvertedMove(move);

					whiteMoving = !whiteMoving;
				}
				action.accept(outputQueue.poll());
				return true;
			}
			return false;
		}
		
		private void addData(ChessBoard board, int fr, int fc, int tr, int tc, int mt) {
			double[] output = new double[518]; // 6*8*8 input + 2*8*8+6 output

			System.arraycopy(board.generateNeuralInputs(whiteMoving), 0, output, 0, 384);

			if (whiteMoving) { //Generate the data as if the white team is moving
				//Add the outputs to the array
				output[384 + fr * 8 + fc] = 1;
				output[448 + tr * 8 + tc] = 1;
				output[512 + mt - 1] = 1;
			} else { //Generate the data as if the black team is moving
				//Add the inputs to the array
				//Add the outputs to the array
				output[384 + (7-fr) * 8 + fc] = 1;
				output[448 + (7-tr) * 8 + tc] = 1;
				output[512 + mt - 1] = 1;
			}
			
			outputQueue.offer(output);
		}

		@Override
		public Spliterator<double[]> trySplit() {
			return null;
		}

		@Override
		public long estimateSize() {
			return sizeLeft;
		}

		@Override
		public int characteristics() {
			return SIZED | IMMUTABLE;
		}
	}
}
