package temporal.file.client;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX GUI for the file server client.
 *
 * <p>Loads the user interface from {@code FileClientGUI.fxml} and starts
 * the JavaFX application. All operations are handled by
 * {@link FileClientController}.
 *
 * <h3>Usage</h3>
 * <pre>mvn javafx:run</pre>
 */
public class FileClientGUI extends Application
{
	private static final Logger logger = Logger.getLogger(FileClientGUI.class.getName());

	/**
	 * Entry point called by the JavaFX launcher.
	 *
	 * @param stage the primary stage
	 */
	@Override
	public void start(Stage stage)
	{
		try
		{
			FXMLLoader loader = new FXMLLoader(getClass().getResource("FileClientGUI.fxml"));

			Parent root = loader.load();
			stage.setTitle("File Client");
			stage.setScene(new Scene(root));
			stage.show();
		}
		catch (IOException e)
		{
			logger.log(Level.SEVERE, "Failed to load GUI", e);
		}
	}

	/**
	 * Launches the JavaFX application.
	 *
	 * @param args command-line arguments (none expected)
	 */
	public static void main(String[] args)
	{
		launch(args);
	}
}
