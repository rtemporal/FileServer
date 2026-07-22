package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import temporal.file.environment.FileServerEnvironment;
import temporal.file.server.FileServerAPI;

/**
 * Controller for the file client GUI.
 *
 * <p>Handles all user interactions: connecting to the server, performing
 * memory-store and file-store operations, and shutting down.  RMI calls
 * run on background threads to keep the UI responsive.
 */
public class FileClientController
{
	private static final Logger logger = Logger.getLogger(FileClientController.class.getName());

	private FileServerAPI api;

	@FXML private MenuItem menuConnect;
	@FXML private MenuItem menuShutdown;
	@FXML private MenuItem menuExit;
	@FXML private Label statusLabel;
	@FXML private TextArea outputArea;

	@FXML private TextField memoryFileField;
	@FXML private Button putBtn;
	@FXML private Button getBtn;
	@FXML private Button removeBtn;

	@FXML private TextField memoryDirField;
	@FXML private Button putAllBtn;
	@FXML private Button getAllBtn;
	@FXML private Button removeAllBtn;

	@FXML private TextField fileStoreFileField;
	@FXML private Button uploadBtn;
	@FXML private Button downloadBtn;
	@FXML private Button deleteBtn;

	@FXML private TextField fileStoreDirField;
	@FXML private Button uploadAllBtn;
	@FXML private Button downloadAllBtn;
	@FXML private Button deleteAllBtn;

	@FXML
	void initialize()
	{
		setDisconnected();
	}

	private void setConnected()
	{
		String host = FileServerEnvironment.variables().host();
		short port = FileServerEnvironment.variables().port();

		statusLabel.setText("Connected to " + host + ":" + port);
		menuConnect.setDisable(true);
		menuShutdown.setDisable(false);
		setButtonsDisabled(false);
	}

	private void setDisconnected()
	{
		statusLabel.setText("Disconnected");
		menuConnect.setDisable(false);
		menuShutdown.setDisable(true);
		setButtonsDisabled(true);
	}

	private void setButtonsDisabled(boolean disabled)
	{
		putBtn.setDisable(disabled);
		getBtn.setDisable(disabled);
		removeBtn.setDisable(disabled);
		putAllBtn.setDisable(disabled);
		getAllBtn.setDisable(disabled);
		removeAllBtn.setDisable(disabled);
		uploadBtn.setDisable(disabled);
		downloadBtn.setDisable(disabled);
		deleteBtn.setDisable(disabled);
		uploadAllBtn.setDisable(disabled);
		downloadAllBtn.setDisable(disabled);
		deleteAllBtn.setDisable(disabled);
	}

	@FXML
	void onConnect(ActionEvent event)
	{
		runBackground("Connect", () ->
		{
			try
			{
				api = Util.connect();
				Platform.runLater(this::setConnected);
				log("Connected to server.");
			}
			catch (RemoteException | NotBoundException e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	@FXML
	void onShutdown(ActionEvent event)
	{
		if (api == null)
		{
			log("Not connected.");
			return;
		}

		runBackground("Shutdown", () ->
		{
			try
			{
				api.shutdown(FileServerEnvironment.variables().password());
				api = null;
				Platform.runLater(() ->
				{
					setDisconnected();
					log("Shutdown message sent.");
				});
			}
			catch (RemoteException e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	@FXML
	void onExit(ActionEvent event)
	{
		Platform.exit();
	}

	/*
	 * Memory store — single file.
	 */

	@FXML
	void onPut(ActionEvent event)
	{
		File file = new File(memoryFileField.getText());

		if (!checkFileExists(file))
			return;

		runOperation("Put", () ->
		{
			byte[] data = Util.load(file);

			api.put(file, data);
			log("Put: " + file.getName() + " (" + data.length + " bytes)");
		});
	}

	@FXML
	void onGet(ActionEvent event)
	{
		File file = new File(memoryFileField.getText());

		runOperation("Get", () ->
		{
			byte[] data = api.get(file);

			if (data == null)
			{
				log("Get: " + file + " — not found.");
				return;
			}

			Util.save(file, data);
			log("Get: " + file.getName() + " (" + data.length + " bytes)");
		});
	}

	@FXML
	void onRemove(ActionEvent event)
	{
		File file = new File(memoryFileField.getText());

		runOperation("Remove", () ->
		{
			byte[] data = api.remove(file);

			if (data == null)
				log("Remove: " + file + " — not found.");
			else
				log("Remove: " + file.getName() + " — removed (" + data.length + " bytes)");
		});
	}

	/*
	 * Memory store — bulk.
	 */

	@FXML
	void onPutAll(ActionEvent event)
	{
		File dir = new File(memoryDirField.getText());

		if (!checkDirExists(dir))
			return;

		runOperation("PutAll", () ->
		{
			Map<File, byte[]> fileMap = Util.loadAll(dir);

			api.putAll(fileMap);
			log("PutAll: " + dir.getName() + " (" + fileMap.size() + " files)");
		});
	}

	@FXML
	void onGetAll(ActionEvent event)
	{
		File dir = new File(memoryDirField.getText());

		runOperation("GetAll", () ->
		{
			Map<File, byte[]> fileMap = api.getAll(dir);

			if (fileMap == null || fileMap.isEmpty())
			{
				log("GetAll: " + dir + " — no entries found.");
				return;
			}

			Util.save(fileMap);
			log("GetAll: " + dir.getName() + " (" + fileMap.size() + " files)");
		});
	}

	@FXML
	void onRemoveAll(ActionEvent event)
	{
		File dir = new File(memoryDirField.getText());

		runOperation("RemoveAll", () ->
		{
			api.removeAll(dir);
			log("RemoveAll: " + dir.getName() + " — done.");
		});
	}

	/*
	 * File store — single file.
	 */

	@FXML
	void onUpload(ActionEvent event)
	{
		File file = new File(fileStoreFileField.getText());

		if (!checkFileExists(file))
			return;

		runOperation("Upload", () ->
		{
			byte[] data = Util.load(file);

			api.upload(file, data);
			log("Upload: " + file.getName() + " (" + data.length + " bytes)");
		});
	}

	@FXML
	void onDownload(ActionEvent event)
	{
		File file = new File(fileStoreFileField.getText());

		runOperation("Download", () ->
		{
			byte[] data = api.download(file);

			if (data == null)
			{
				log("Download: " + file + " — not found.");
				return;
			}

			Util.save(file, data);
			log("Download: " + file.getName() + " (" + data.length + " bytes)");
		});
	}

	@FXML
	void onDelete(ActionEvent event)
	{
		File file = new File(fileStoreFileField.getText());

		runOperation("Delete", () ->
		{
			boolean deleted = api.delete(file);

			if (deleted)
				log("Delete: " + file.getName() + " — deleted.");
			else
				log("Delete: " + file.getName() + " — failed.");
		});
	}

	/*
	 * File store — bulk.
	 */

	@FXML
	void onUploadAll(ActionEvent event)
	{
		File dir = new File(fileStoreDirField.getText());

		if (!checkDirExists(dir))
			return;

		runOperation("UploadAll", () ->
		{
			Map<File, byte[]> fileMap = Util.loadAll(dir);

			api.uploadAll(fileMap);
			log("UploadAll: " + dir.getName() + " (" + fileMap.size() + " files)");
		});
	}

	@FXML
	void onDownloadAll(ActionEvent event)
	{
		File dir = new File(fileStoreDirField.getText());

		runOperation("DownloadAll", () ->
		{
			Map<File, byte[]> fileMap = api.downloadAll(dir);

			if (fileMap == null || fileMap.isEmpty())
			{
				log("DownloadAll: " + dir + " — no files found.");
				return;
			}

			dir.mkdir();

			for (Entry<File, byte[]> entry : fileMap.entrySet())
				Util.save(entry.getKey(), entry.getValue());

			log("DownloadAll: " + dir.getName() + " (" + fileMap.size() + " files)");
		});
	}

	@FXML
	void onDeleteAll(ActionEvent event)
	{
		File dir = new File(fileStoreDirField.getText());

		runOperation("DeleteAll", () ->
		{
			boolean deleted = api.deleteAll(dir);

			if (deleted)
				log("DeleteAll: " + dir.getName() + " — deleted.");
			else
				log("DeleteAll: " + dir.getName() + " — failed.");
		});
	}

	/*
	 * Browse buttons.
	 */

	@FXML
	void onMemoryBrowseFile(ActionEvent event)
	{
		File file = showFileChooser("Select File");

		if (file != null)
			memoryFileField.setText(file.getAbsolutePath());
	}

	@FXML
	void onMemoryBrowseDir(ActionEvent event)
	{
		File dir = showDirectoryChooser("Select Directory");

		if (dir != null)
			memoryDirField.setText(dir.getAbsolutePath());
	}

	@FXML
	void onFileStoreBrowseFile(ActionEvent event)
	{
		File file = showFileChooser("Select File");

		if (file != null)
			fileStoreFileField.setText(file.getAbsolutePath());
	}

	@FXML
	void onFileStoreBrowseDir(ActionEvent event)
	{
		File dir = showDirectoryChooser("Select Directory");

		if (dir != null)
			fileStoreDirField.setText(dir.getAbsolutePath());
	}

	/*
	 * Helpers.
	 */

	private boolean checkFileExists(File file)
	{
		if (!file.exists())
		{
			log("File not found: " + file);
			return false;
		}

		return true;
	}

	private boolean checkDirExists(File dir)
	{
		if (!dir.exists() || !dir.isDirectory())
		{
			log("Directory not found: " + dir);
			return false;
		}

		return true;
	}

	private File showFileChooser(String title)
	{
		FileChooser chooser = new FileChooser();

		chooser.setTitle(title);
		return chooser.showOpenDialog(null);
	}

	private File showDirectoryChooser(String title)
	{
		DirectoryChooser chooser = new DirectoryChooser();

		chooser.setTitle(title);
		return chooser.showDialog(null);
	}

	private void log(String message)
	{
		Platform.runLater(() -> outputArea.appendText(message + "\n"));
	}

	@FunctionalInterface
	private interface RemoteOperation
	{
		void run() throws IOException;
	}

	private void runOperation(String name, RemoteOperation op)
	{
		runBackground(name, () ->
		{
			try
			{
				op.run();
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	private void runBackground(String name, Runnable rmiTask)
	{
		new Thread(() ->
		{
			try
			{
				rmiTask.run();
			}
			catch (Exception e)
			{
				logger.log(Level.WARNING, name + " failed", e);
				Platform.runLater(() -> showError(name + " failed", e.getMessage()));
			}
		}).start();
	}

	private void showError(String header, String content)
	{
		Alert alert = new Alert(AlertType.ERROR);

		alert.setTitle("Error");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
