package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Map;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Uploads local files and directories recursively to the remote
 * persistent store.
 *
 * <p>Each file is GZIP-compressed before sending. The server decompresses
 * before writing to disk.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.UploadAll directory</pre>
 */
public class UploadAll
{
	private static final Logger logger = Logger.getLogger(UploadAll.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the local directory to upload
	 *             recursively
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an I/O error occurs or the directory
	 *                           does not exist
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + UploadAll.class.getSimpleName() + " directory");
			return;
		}

		final File directory = new File(args[0]);

		if (!directory.exists())
			throw new IOException("Directory not found: " + directory);

		logger.info("Running " + UploadAll.class.getSimpleName() + "...");

		Map<File, byte[]> fileMap = Util.loadAll(new File[] { directory });

		logger.info("uploadAll " + directory.getName());

		for (File file : fileMap.keySet())
			logger.fine(() -> "uploadAll(" + file + ")");

		FileServerAPI api = Util.connect();
		api.uploadAll(fileMap);
	}
}
