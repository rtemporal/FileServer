package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Map;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Retrieves all entries under a directory from the remote in-memory
 * store and saves them to local disk.
 *
 * <p>Each value is stored GZIP-compressed. This client decompresses
 * before writing each file.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.GetAll directory</pre>
 */
public class GetAll
{
	private static final Logger logger = Logger.getLogger(GetAll.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the directory to filter by in the
	 *             in-memory store
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an RMI error occurs or no entries were
	 *                           found
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + GetAll.class.getSimpleName() + " directory");
			return;
		}

		final File file = new File(args[0]);

		logger.info("Running " + GetAll.class.getSimpleName() + "...");
		logger.info("getAll " + file);

		FileServerAPI api = Util.connect();

		Map<File, byte[]> fileMap = api.getAll(file);

		if (fileMap == null)
			throw new IOException("No entries found for: " + file);

		Util.save(fileMap);
	}
}
