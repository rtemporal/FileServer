package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Map;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Reads local files and directories and stores them in the remote
 * in-memory store.
 *
 * <p>Each file content is GZIP-compressed before sending.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.PutAll files and directories...</pre>
 */
public class PutAll
{
	private static final Logger logger = Logger.getLogger(PutAll.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args the files or directories to load and store
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an I/O error occurs or any file does
	 *                           not exist
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length < 1)
		{
			System.err.println("Usage: java " + PutAll.class.getSimpleName() + " files and directories...");
			return;
		}

		final File[] fileArray = new File[args.length];

		for (int i = 0; i != args.length; i++)
			fileArray[i] = new File(args[i]);

		for (File file : fileArray)
		{
			if (!file.exists())
				throw new IOException("File not found: " + file);
		}

		logger.info("Running " + PutAll.class.getSimpleName() + "...");

		Map<File, byte[]> fileMap = Util.loadAll(fileArray);

		logger.info("putAll...");

		for (File file : fileMap.keySet())
			logger.fine(() -> "putAll(" + file + ")");

		FileServerAPI api = Util.connect();
		api.putAll(fileMap);
	}
}
