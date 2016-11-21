package sergeysav.neuralnetwork.chess;

import java.util.LinkedList;

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
}
