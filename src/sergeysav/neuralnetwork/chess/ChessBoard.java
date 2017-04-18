package sergeysav.neuralnetwork.chess;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChessBoard {
	private static final Pattern matchingPattern;
	private static final Pattern positionPattern;

	static {
		String capturePieceType = "([KQRBN])";
		String optionalCapturePieceType = capturePieceType + "?";

		String groupColumn = "(?:[a-h])";
		String optionalGroupColumn = groupColumn + "?";
		String groupRow = "(?:[1-8])";
		String optionalGroupRow = groupRow + "?";

		String captureLocation = "(" + groupColumn + groupRow + ")";
		String optionalCaptureLocation = "(" + optionalGroupColumn + optionalGroupRow + ")?";

		String optionalEqualsGroup = "(?:[=])?";

		String optionalCaptureCapture = "([x])?";

		String optionalGroupState = "(?:[+#])?";

		//									Capture the type		 Capture the from location				        Capture the to location               Capture the type of piece that it became
		matchingPattern = Pattern.compile(optionalCapturePieceType + optionalCaptureLocation + optionalCaptureCapture + captureLocation + optionalEqualsGroup + optionalCapturePieceType + optionalGroupState);
		positionPattern = Pattern.compile("([a-h])?([1-8])?");
	}

	//[Row][Col]
	// ±1 = Pawn
	// ±2 = Rook
	// ±3 = Knight
	// ±4 = Bishop
	// ±5 = Queen
	// ±6 = King
	// ±7 = En passant rule
	// + = White, - = Black
	private int[][] board;
	private int enpassantCol = -1;

	public ChessBoard() {
		board = new int[8][8];

		//Set pawns
		for (int i = 0; i<8; i++) {
			board[1][i] = 1;
			board[6][i] = -1;
		}

		//Set rooks
		board[0][0] = 2;
		board[7][0] = -2;
		board[0][7] = 2; 
		board[7][7] = -2;

		//Set knights
		board[0][1] = 3;
		board[7][1] = -3;
		board[0][6] = 3;
		board[7][6] = -3;

		//Set bishops
		board[0][2] = 4;
		board[7][2] = -4;
		board[0][5] = 4;
		board[7][5] = -4;

		//Set kings
		board[0][4] = 6;
		board[7][4] = -6;

		//Set queens
		board[0][3] = 5;
		board[7][3] = -5;
	}
	
	public ChessBoard(ChessBoard toCopy) {
		board = toCopy.deepCopyBoard();
		enpassantCol = toCopy.enpassantCol;
	}

	//Input format: RowCol;RowCol;NewType;CASTLE
	//CASTLE not required
	//Assumes the move is legal
	public void applyConvertedMove(String move) {

		String[] moveParts = move.split(";");

		int fromRow = Integer.parseInt(moveParts[0].charAt(0) + "");
		int fromCol = Integer.parseInt(moveParts[0].charAt(1) + "");

		int toRow = Integer.parseInt(moveParts[1].charAt(0) + "");
		int toCol = Integer.parseInt(moveParts[1].charAt(1) + "");

		int piece = board[fromRow][fromCol];//Integer.parseInt(moveParts[2]);
		int pieceType = Math.abs(board[fromRow][fromCol]);
		int team = Math.round(Math.signum(board[fromRow][fromCol]));

		boolean doResetEnpassant = true;

		if (pieceType == 1) { //If this is a pawn
			if (Math.abs(board[toRow][toCol]) == 7) { //If it is moving onto an enpassant stored tile
				board[toRow + board[toRow][toCol]/7][toCol] = 0; //Kill the pawn that produced the enpassant tile
			} else if (Math.abs(fromRow - toRow) == 2) { //If it just did the double move thing
				//Reset the current enpassant tile
				resetEnpassant(team);
				doResetEnpassant = false;

				//Set an empassant tile
				board[(toRow + fromRow)/2][toCol] = 7 * team;
				enpassantCol = toCol;
			}
		} else if (pieceType == 6) { //If this is a king
			if (fromCol == 4) {
				if (doResetEnpassant) resetEnpassant(team);
				if (toCol == 7) { //Kingside Castle
					board[toRow][6] = piece;
					board[toRow][5] = board[toRow][7];
					board[toRow][7] = 0;
					board[toRow][4] = 0;
					return;
				} else if (toCol == 0) { //Queenside Castle
					board[toRow][2] = piece;
					board[toRow][3] = board[toRow][0];
					board[toRow][0] = 0;
					board[toRow][4] = 0;
					return;
				}
			}
		}

		if (doResetEnpassant) resetEnpassant(team);
		board[fromRow][fromCol] = 0;
		if (moveParts.length > 2) {
			piece = Integer.parseInt(moveParts[2]);
		}
		board[toRow][toCol] = piece;
	}

	private void resetEnpassant(int ofTeam) {
		if (enpassantCol >= 0 && enpassantCol < 8) {
			if (Math.abs(board[ofTeam == -1 ? 2 : 5][enpassantCol]) == 7) {
				board[ofTeam == -1 ? 2 : 5][enpassantCol] = 0;
			}
			enpassantCol = -1;
		}
	}

	public boolean isLegalMove(String from, String to) {
		int fromRow = Integer.parseInt(from.charAt(0) + "");
		int fromCol = Integer.parseInt(from.charAt(1) + "");

		int toRow = Integer.parseInt(to.charAt(0) + "");
		int toCol = Integer.parseInt(to.charAt(1) + "");

		if ((board[fromRow][fromCol] == 6 && fromRow == 0 && toRow == 0) || (board[fromRow][fromCol] == -6 && fromRow == 7 && toRow == 7) && (board[fromRow][toCol]/2 == board[fromRow][fromCol]/6)) { //If king
			if (fromCol == 4) {
				if (toCol == 7) { //Kingside Castle
					if (board[fromRow][5] != 0 || board[fromRow][6] != 0) {
						return false;
					}
					if (board[fromRow][fromCol] == 6) { //White
						return !(isWhiteCheck(fromRow, 4) || isWhiteCheck(fromRow, 5) || isWhiteCheck(fromRow, 6) || isWhiteCheck(fromRow, 7));
					} else { //Black
						return !(isBlackCheck(fromRow, 4) || isBlackCheck(fromRow, 5) || isBlackCheck(fromRow, 6) || isBlackCheck(fromRow, 7));
					}
				} else if (toCol == 0) { //Queenside Castle
					if (board[fromRow][3] != 0 || board[fromRow][2] != 0 || board[fromRow][1] != 0) {
						return false;
					}
					if (board[fromRow][fromCol] == 6) { //White
						return !(isWhiteCheck(fromRow, 0) || isWhiteCheck(fromRow, 1) || isWhiteCheck(fromRow, 2) || isWhiteCheck(fromRow, 3) || isWhiteCheck(fromRow, 4));
					} else { //Black
						return !(isBlackCheck(fromRow, 0) || isBlackCheck(fromRow, 1) || isBlackCheck(fromRow, 2) || isBlackCheck(fromRow, 3) || isBlackCheck(fromRow, 4));
					}
				}
			}
		}

		return isLegalMove(fromRow + "" + fromCol + ";" + toRow + "" + toCol); 
	}

	//Does not check for Castles
	private boolean isLegalMove(String move) {
		String[] moveParts = move.split(";");

		int fromRow = Integer.parseInt(moveParts[0].charAt(0) + "");
		int fromCol = Integer.parseInt(moveParts[0].charAt(1) + "");

		int toRow = Integer.parseInt(moveParts[1].charAt(0) + "");
		int toCol = Integer.parseInt(moveParts[1].charAt(1) + "");

		int minRow = Math.min(fromRow, toRow);
		int minCol = Math.min(fromCol, toCol);

		int maxRow = Math.max(fromRow, toRow);
		int maxCol = Math.max(fromCol, toCol);

		int piece = board[fromRow][fromCol];//Integer.parseInt(moveParts[2]);
		int pieceType = Math.abs(piece);
		int team = Math.round(Math.signum(piece));

		if (team == Math.round(Math.signum(board[toRow][toCol]))) {
			return false;
		}
		
		if (toRow < 0 || toRow > 7 && toCol < 0 && toCol > 7) {
			return false;
		}

		if (team > 0) { // WHITE
			int enpassantBackup = enpassantCol;
			int[][] boardBackup = deepCopyBoard();
			applyConvertedMove(move);
			boolean check = isWhiteCheck();
			board = boardBackup;
			enpassantCol = enpassantBackup;
			if (check) {
				return false;
			}
		} else { //BLACK
			int enpassantBackup = enpassantCol;
			int[][] boardBackup = deepCopyBoard();
			applyConvertedMove(move);
			boolean check = isBlackCheck();
			board = boardBackup;
			enpassantCol = enpassantBackup;
			if (check) {
				return false;
			}
		}

		if (pieceType == 1) { //Pawn
			return ((board[toRow][toCol] != 0 && toRow == fromRow + team && (toCol == fromCol + 1 || toCol == fromCol - 1)) || (toCol == fromCol && board[toRow][toCol] == 0 && ((toRow == fromRow + team) || (((fromRow == 1 && team == 1) || (fromRow == 6 && team == -1)) ? toRow == fromRow + 2*team && board[fromRow + team][fromCol] == 0 : false))));
		} else if (pieceType == 2) { //Rook
			if (toRow == fromRow) {
				for (int i = minCol + 1; i < maxCol; i++) {
					if (board[toRow][i] != 0 && Math.abs(board[toRow][i]) != 7) return false;
				}
				return true;
			} else if (toCol == fromCol) {
				for (int i = minRow + 1; i < maxRow; i++) {
					if (board[i][toCol] != 0 && Math.abs(board[i][toCol]) != 7) return false;
				}
				return true;
			} else {
				return false;
			}
		} else if (pieceType == 3) { //Knight
			int dx = toRow - fromRow;
			int dy = toCol - fromCol;
			return dx*dx + dy*dy == 5;
		} else if (pieceType == 4) { //Bishop
			int dx = toRow - fromRow;
			int dy = toCol - fromCol;
			if (dy != dx && dy != -dx) return false;
			int m = dy/dx;
			int startCol = (m < 0 ? maxCol : minCol);
			for (int i = minRow + 1; i < maxRow; i++) {
				if (board[i][startCol + m*(i - minRow)] != 0 && board[i][startCol + m*(i - minRow)] != 7) return false;
			}
			return true;
		} else if (pieceType == 5) { //Queen
			if (toRow == fromRow) {
				for (int i = minCol + 1; i < maxCol; i++) {
					if (board[toRow][i] != 0 && Math.abs(board[toRow][i]) != 7) return false;
				}
				return true;
			} else if (toCol == fromCol) {
				for (int i = minRow + 1; i < maxRow; i++) {
					if (board[i][toCol] != 0 && Math.abs(board[i][toCol]) != 7) return false;
				}
				return true;
			} else {
				int dx = toRow - fromRow;
				int dy = toCol - fromCol;
				if (dy != dx && dy != -dx) return false;
				int m = dy/dx;
				int startCol = (m < 0 ? maxCol : minCol);
				for (int i = minRow + 1; i < maxRow; i++) {
					if (board[i][startCol + m*(i - minRow)] != 0 && Math.abs(board[i][startCol + m*(i - minRow)]) != 7) return false;
				}
				return true;
			}
		} else if (pieceType == 6) { //King
			int dx = toRow - fromRow;
			int dy = toCol - fromCol;
			return dx*dx + dy*dy <= 2;
		}

		return false;
	}

	//K = king
	//Q = queen
	//R = rook
	//B = bishop
	//N = knight
	//Regex String: ([KQRBN])?((?:[a-h])?(?:[1-8])?)?(?:[x])?((?:[a-h])(?:[1-8]))([KQRBN])?(?:[+#])?
	//Output format: RowCol;RowCol;NewType;CASTLE
	public String getMoveConverted(String move, boolean whiteTeamMoving) {
		if (move.startsWith("O-O-O")) { //Queenside castle
			int row = whiteTeamMoving ? 0 : 7;
			return row + "" + 4 + ";" + row + "" + 0 + ";" + (6 * (whiteTeamMoving ? 1 : -1)) + ";CASTLE";
		} else if (move.startsWith("O-O")) { //Kingside castle
			int row = whiteTeamMoving ? 0 : 7;
			return row + "" + 4 + ";" + row + "" + 7 + ";" + (6 * (whiteTeamMoving ? 1 : -1)) + ";CASTLE";
		}

		Matcher matcher = matchingPattern.matcher(move);
		matcher.find();

		//Get the matching groups from the regex
		String pieceType = matcher.group(1);
		String fromSpecifier = matcher.group(2);
		String capture = matcher.group(3);
		String toLocation = matcher.group(4);
		String upgradeType = matcher.group(5);

		boolean isCapture = capture != null && capture.equals("x");

		//Get the piece type
		int piece = getPieceNumFromStr(pieceType);

		if (piece != 1) {
			resetEnpassant(whiteTeamMoving ? 1 : -1);
		}

		//Get the already known row and column values
		int fromRow = -1;
		int fromCol = -1;
		if (fromSpecifier != null) {
			Matcher fromMatcher = positionPattern.matcher(fromSpecifier);
			fromMatcher.find();

			String col = fromMatcher.group(1);
			String row = fromMatcher.group(2);

			if (col != null) fromCol = (int)col.charAt(0) - (int)'a';
			if (row != null) fromRow = (int)row.charAt(0) - (int)'1';
		}

		//Get the to row and column values
		int toCol = (int)toLocation.charAt(0) - (int)'a';
		int toRow = (int)toLocation.charAt(1) - (int)'1';

		//Figure out what type of piece this will be when it is done
		int newType = piece;
		if (upgradeType != null) newType = getPieceNumFromStr(upgradeType);

		if (fromRow == -1 || fromCol == -1) {
			List<String> possibleFroms = new LinkedList<String>();
			if (piece == 1) { //If it is a pawn
				if (whiteTeamMoving) {
					if (isCapture) {
						possibleFroms.add((toRow-1) + "" + (toCol+1));
						possibleFroms.add((toRow-1) + "" + (toCol-1));
					} else {
						possibleFroms.add((toRow-1) + "" + (toCol));
						if (toRow == 3) {
							possibleFroms.add((toRow-2) + "" + (toCol));
						}
					}
				} else {
					if (isCapture) {
						possibleFroms.add((toRow+1) + "" + (toCol+1));
						possibleFroms.add((toRow+1) + "" + (toCol-1));
					} else {
						possibleFroms.add((toRow+1) + "" + (toCol));
						if (toRow == 4) {
							possibleFroms.add((toRow+2) + "" + (toCol));
						}
					}
				}
			} else if (piece == 2) { //If it is a rook
				for (int i = 0; i<8; i++) {
					if (i != toCol) {
						possibleFroms.add(toRow + "" + i);
					}
				}
				for (int i = 0; i<8; i++) {
					if (i != toRow) {
						possibleFroms.add(i + "" + toCol);
					}
				}
			} else if (piece == 3) { //If it is a knight
				possibleFroms.add((toRow+2) + "" + (toCol+1));
				possibleFroms.add((toRow+2) + "" + (toCol-1));
				possibleFroms.add((toRow+1) + "" + (toCol+2));
				possibleFroms.add((toRow+1) + "" + (toCol-2));
				possibleFroms.add((toRow-1) + "" + (toCol+2));
				possibleFroms.add((toRow-1) + "" + (toCol-2));
				possibleFroms.add((toRow-2) + "" + (toCol+1));
				possibleFroms.add((toRow-2) + "" + (toCol-1));
			} else if (piece == 4) { //If it is a bishop
				for (int i = 0; i<8; i++) {
					if (i != toCol) {
						possibleFroms.add((toRow - toCol + i) + "" + (i));
					}
				}
				for (int i = 0; i<8; i++) {
					if (i != toRow) {
						possibleFroms.add((i) + "" + (toRow + toCol - i));
					}
				}
			} else if (piece == 5) { //If it is a queen
				for (int i = 0; i<8; i++) {
					if (i != toCol) {
						possibleFroms.add(toRow + "" + i);
					}
				}
				for (int i = 0; i<8; i++) {
					if (i != toRow) {
						possibleFroms.add(i + "" + toCol);
					}
				}
				for (int i = 0; i<8; i++) {
					if (i != toCol) {
						possibleFroms.add((toRow - toCol + i) + "" + (i));
					}
				}
				for (int i = 0; i<8; i++) {
					if (i != toRow) {
						possibleFroms.add((i) + "" + (toRow + toCol - i));
					}
				}
			} else if (piece == 6) { //If it is a king
				possibleFroms.add((toRow+1) + "" + (toCol));
				possibleFroms.add((toRow-1) + "" + (toCol));
				possibleFroms.add((toRow) + "" + (toCol+1));
				possibleFroms.add((toRow) + "" + (toCol-1));
				possibleFroms.add((toRow+1) + "" + (toCol+1));
				possibleFroms.add((toRow-1) + "" + (toCol-1));
				possibleFroms.add((toRow-1) + "" + (toCol+1));
				possibleFroms.add((toRow+1) + "" + (toCol-1));
			}

			final int fr = fromRow;
			final int fc = fromCol;
			Optional<String> originalLocationOP = possibleFroms.stream().filter((s)->{
				if (s.contains("-")) return false;

				int r = Integer.parseInt(s.charAt(0)+"");
				int c = Integer.parseInt(s.charAt(1)+"");

				if (r < 0 || r > 7 || c < 0 || c > 7) return false;

				if (board[r][c] != piece * (whiteTeamMoving ? 1 : -1)) return false;

				if (fr != -1 && fr != r) return false;
				if (fc != -1 && fc != c) return false;

				if (!isLegalMove((r + "" + c) + ";" + (toRow + "" + toCol) + ";" + piece * (whiteTeamMoving ? 1 : -1))) return false;

				return true;
			}).findAny(); 

			String originalLoc = originalLocationOP.get();
			fromRow = Integer.parseInt(originalLoc.charAt(0)+"");
			fromCol = Integer.parseInt(originalLoc.charAt(1)+"");
		}

		return (fromRow + "" + fromCol) + ";" + (toRow + "" + toCol) + ";" + newType * (whiteTeamMoving ? 1 : -1);
	}

	private boolean hasLegalMoves(int row, int col) {
		int piece = board[row][col];
		int pieceType = Math.abs(piece);
		int team = Math.round(Math.signum(piece));

		if (pieceType == 1) { //Pawn
			return isLegalMove(row + "" + col + ";" + (row+team) + "" + col) ||
					isLegalMove(row + "" + col + ";" + (row+team+team) + "" + col) ||
					isLegalMove(row + "" + col + ";" + (row+team) + "" + (col+1)) ||
					isLegalMove(row + "" + col + ";" + (row+team) + "" + (col-1));
		} else if (pieceType == 2) { //Rook
			for (int i = 0; i<8; i++) {
				if (i != row) {
					if (isLegalMove(row + "" + col + ";" + i + "" + col)) {
						return true;
					}
				}
			}
			for (int i = 0; i<8; i++) {
				if (i != col) {
					if (isLegalMove(row + "" + col + ";" + row + "" + i)) {
						return true;
					}
				}
			}
		} else if (pieceType == 3) { //Knight
			return isLegalMove(row + "" + col + ";" + (row+2) + "" + (col+1)) ||
					isLegalMove(row + "" + col + ";" + (row+2) + "" + (col-1)) ||
					isLegalMove(row + "" + col + ";" + (row+1) + "" + (col+2)) ||
					isLegalMove(row + "" + col + ";" + (row+1) + "" + (col-2)) ||
					isLegalMove(row + "" + col + ";" + (row-2) + "" + (col-1)) ||
					isLegalMove(row + "" + col + ";" + (row-2) + "" + (col-1)) ||
					isLegalMove(row + "" + col + ";" + (row-1) + "" + (col-2)) ||
					isLegalMove(row + "" + col + ";" + (row-1) + "" + (col-2));
		} else if (pieceType == 4) { //Bishop
			for (int i = 0; i<8; i++) {
				if (i != col) {
					if (isLegalMove(row + "" + col + ";" + (row - col + i) + "" + (i))) {
						return true;
					}
				}
			}
			for (int i = 0; i<8; i++) {
				if (i != row) {
					if (isLegalMove(row + "" + col + ";" + (i) + "" + (row + col - i))) {
						return true;
					}
				}
			}
		} else if (pieceType == 5) { //Queen
			for (int i = 0; i<8; i++) {
				if (i != row) {
					if (isLegalMove(row + "" + col + ";" + i + "" + col)) {
						return true;
					}
				}
			}
			for (int i = 0; i<8; i++) {
				if (i != col) {
					if (isLegalMove(row + "" + col + ";" + row + "" + i)) {
						return true;
					}
				}
			}
			for (int i = 0; i<8; i++) {
				if (i != col) {
					if (isLegalMove(row + "" + col + ";" + (row - col + i) + "" + (i))) {
						return true;
					}
				}
			}
			for (int i = 0; i<8; i++) {
				if (i != row) {
					if (isLegalMove(row + "" + col + ";" + (i) + "" + (row + col - i))) {
						return true;
					}
				}
			}
		} else if (pieceType == 6) { //King
			return isLegalMove(row + "" + col + ";" + (row) + "" + (col+1)) ||
					isLegalMove(row + "" + col + ";" + (row) + "" + (col-1)) ||
					isLegalMove(row + "" + col + ";" + (row+1) + "" + (col)) ||
					isLegalMove(row + "" + col + ";" + (row-1) + "" + (col)) ||
					isLegalMove(row + "" + col + ";" + (row+1) + "" + (col+1)) ||
					isLegalMove(row + "" + col + ";" + (row+1) + "" + (col-1)) ||
					isLegalMove(row + "" + col + ";" + (row-1) + "" + (col+1)) ||
					isLegalMove(row + "" + col + ";" + (row-1) + "" + (col-1));
		}

		return false;
	}

	private int getPieceNumFromStr(String pieceType) {
		if (pieceType == null) return 1;
		else if (pieceType.equals("R")) return 2;
		else if (pieceType.equals("N")) return 3;
		else if (pieceType.equals("B")) return 4;
		else if (pieceType.equals("Q")) return 5;
		else if (pieceType.equals("K")) return 6;
		return -1;
	}

	public int getPieceAt(int row, int col) {
		return board[row][col];
	}

	public boolean isWhiteCheck() {
		for (int i = 0; i<8; i++) {
			for (int j = 0; j<8; j++) {
				if (board[i][j] == 6) {
					return isWhiteCheck(i, j);
				}
			}
		}
		return false;
	}

	private boolean isWhiteCheck(int kI, int kJ) {
		for (int i = 0; i<8; i++) {
			for (int j = 0; j<8; j++) {
				if (board[i][j] < 0) {
					if (isLegalMove(i + "" + j, kI + "" + kJ)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public boolean isBlackCheck() {
		for (int i = 0; i<8; i++) {
			for (int j = 0; j<8; j++) {
				if (board[i][j] == -6) {
					return isBlackCheck(i, j);
				}
			}
		}
		return false;
	}

	private boolean isBlackCheck(int kI, int kJ) {
		for (int i = 0; i<8; i++) {
			for (int j = 0; j<8; j++) {
				if (board[i][j] > 0) {
					if (isLegalMove(i + "" + j, kI + "" + kJ)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Bit flags:
	 * 1 = Game over
	 * 2 = White in Check(mate; if 1)
	 * 4 = Black in Check(mate; if 1)
	 * 8 = Stalemate
	 * 
	 * Check = all legal moves involve protecting king
	 * Checkmate = no legal moves, in check
	 * Stalemate = no legal moves, not in check
	 * 
	 * @return an integer of flags representing the board state
	 */
	public int getBoardState(boolean whiteTeamMoving) {
		int result = 0;
		boolean moves = doMovesExist(whiteTeamMoving);

		result |= moves ? 0 : 1;
		result |= isWhiteCheck() ? 1<<1 : 0;
		result |= isBlackCheck() ? 1<<2 : 0;
		result |= result == 1 ? 0 : 1<<3;

		return result;
	}

	private boolean doMovesExist(boolean whiteTeamMoving) {
		if (whiteTeamMoving) {
			for (int i = 0; i<8; i++) {
				for (int j = 0; j<8; j++) {
					if (board[i][j] > 0) {
						if (hasLegalMoves(i, j)) {
							return true;
						}
					}
				}
			}
		} else {
			for (int i = 0; i<8; i++) {
				for (int j = 0; j<8; j++) {
					if (board[i][j] < 0) {
						if (hasLegalMoves(i, j)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private int[][] deepCopyBoard() {
		int[][] newArr = new int[8][8];
		for (int i = 0; i<8; i++) {
			System.arraycopy(board[i], 0, newArr[i], 0, 8);
		}
		return newArr;
	}
	
	public void transpose(int rows, int cols) {
		if (enpassantCol != -1) enpassantCol += cols;
		//if (enpassantCol < 0 || enpassantCol > 8) enpassantCol = -1;
		int[][] newBoard = new int[8][8];
		for (int i = Math.max(0, -rows); i<8-Math.max(0, rows); i++) {
			System.arraycopy(board[i], 
					Math.max(0, -cols), 
					newBoard[i+rows], 
					0, 
					8-Math.abs(cols));
		}
		board = newBoard;
	}
	
	public double[] generateNeuralInputs(boolean whiteMoving) {
		double[] output = new double[384];
		
		if (whiteMoving) { //Generate the data as if the white team is moving
			//Add the inputs to the array
			for (int i = 0; i<8; i++) {
				for (int j = 0; j<8; j++) {
					int pieceType = getPieceAt(i, j);
					if (pieceType == 7 || pieceType == -7) pieceType = 0;
					int absType = Math.abs(pieceType);
					if (absType > 0) {
						output[6*8*i + 6*j + absType - 1] = Math.signum(pieceType);
					}
				}
			}
		} else { //Generate the data as if the black team is moving
			//Add the inputs to the array
			for (int i = 0; i<8; i++) {
				for (int j = 0; j<8; j++) {
					int pieceType = getPieceAt(i, j);
					if (pieceType == 7 || pieceType == -7) pieceType = 0;
					int absType = Math.abs(pieceType);
					if (absType > 0) {
						output[6*8*(7-i) + 6*j + absType - 1] = -Math.signum(pieceType);
					}
				}
			}
		}
		
		return output;
	}
	
	@Override
	public String toString() {
		return Arrays.deepToString(board);
	}
}
