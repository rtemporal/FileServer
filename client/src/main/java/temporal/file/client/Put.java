package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Reads a local file and stores it in the remote in-memory store.
 *
 * <p>GZIP is the protocol: the file content is GZIP-compressed before
 * sending to the server.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.Put file</pre>
 */
public class Put
{
	private static final Logger logger = Logger.getLogger(Put.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the local file to store in memory
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an I/O error occurs or the file does
	 *                           not exist
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + Put.class.getSimpleName() + " file");
			return;
		}

		final File file = new File(args[0]);

		if (!file.exists())
			throw new IOException("File not found: " + file);

		logger.info("Running " + Put.class.getSimpleName() + "...");
		logger.info("put " + file);

		FileServerAPI api = Util.connect();

		byte[] data = Util.load(file);
		api.put(file, data);
	}
}
