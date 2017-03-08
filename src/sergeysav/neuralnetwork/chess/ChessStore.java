package sergeysav.neuralnetwork.chess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;

import sergeysav.neuralnetwork.NeuralNetwork;

public class ChessStore implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6555883153860584085L;

	public NeuralNetwork network;
	public ChessTrainer trainer;
	private int epoch;
	public int callsInEpoch;

	public void setEpoch(int epoch) {
		this.epoch = epoch;
		callsInEpoch = 0;
	}

	public int getEpoch() {
		return epoch;
	}

	public void save() {
		try {
			new File("backups").mkdirs();

			if (callsInEpoch == 0) {
				ChessAIMain.print("Saving Backup " + epoch + "-" + callsInEpoch);
			}
			//ChessAIMain.print("Saving Backup " + epoch + "-" + callsInEpoch);
			File outputFile = new File("backups/backup-" + epoch + "-" + callsInEpoch++ + ".store");
			outputFile.createNewFile();

			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFile));
			oos.writeObject(this);
			oos.flush();
			oos.close();

			if ((callsInEpoch-3) % 10 != 0) {
				File toDelete = new File("backups/backup-" + epoch + "-" + (callsInEpoch-3) + ".store");
				if (toDelete.exists()) {
					//ChessAIMain.print("Deleting Backup " + epoch + "-" + (callsInEpoch-3));
					Files.delete(toDelete.toPath());
				}
			}
			if ((callsInEpoch-21) % 100 != 0) {
				File toDelete = new File("backups/backup-" + epoch + "-" + (callsInEpoch-21) + ".store");
				if (toDelete.exists()) {
					//ChessAIMain.print("Deleting Backup " + epoch + "-" + (callsInEpoch-21));
					Files.delete(toDelete.toPath());
				}
			}
			if ((callsInEpoch-201) % 1000 != 0) {
				File toDelete = new File("backups/backup-" + epoch + "-" + (callsInEpoch-201) + ".store");
				if (toDelete.exists()) {
					//ChessAIMain.print("Deleting Backup " + epoch + "-" + (callsInEpoch-201));
					Files.delete(toDelete.toPath());
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static ChessStore load(File file) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			ChessStore store = (ChessStore)ois.readObject();
			ois.close();
			return store;
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
