package sergeysav.neuralnetwork.chess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import sergeysav.neuralnetwork.NeuralNetwork;

public class ChessStore implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6555883153860584085L;

	public NeuralNetwork network;
	public ChessTrainer trainer;
	public int epoch;

	public void save() {
		try {
			new File("backups").mkdirs();
			
			File outputFile = new File("backups/backup-" + epoch + ".store");
			outputFile.createNewFile();
			
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFile));
			oos.writeObject(this);
			oos.flush();
			oos.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
