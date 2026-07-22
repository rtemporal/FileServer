package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Retrieves a single entry from the remote in-memory store and saves it
 * to local disk.
 *
 * <p>Data is stored GZIP-compressed in the in-memory store. This client
 * decompresses before writing.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.Get file</pre>
 */
public class Get
{
	private static final Logger logger = Logger.getLogger(Get.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the file key to retrieve from the
	 *             in-memory store
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an RMI error occurs or the key was not
	 *                           found
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + Get.class.getSimpleName() + " file");
			return;
		}

		final File file = new File(args[0]);

		logger.info("Running " + Get.class.getSimpleName() + "...");
		logger.info("get " + file);

		FileServerAPI api = Util.connect();

		byte[] data = api.get(file);

		if (data == null)
			throw new IOException("Not found in memory: " + file);

		Util.save(file, data);
	}
}
