package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Removes a single entry from the remote in-memory store.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.Remove name</pre>
 */
public class Remove
{
	private static final Logger logger = Logger.getLogger(Remove.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the key to remove from the in-memory store
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an RMI error occurs or the key was not
	 *                           found
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + Remove.class.getSimpleName() + " name");
			return;
		}

		final File file = new File(args[0]);

		logger.info("Running " + Remove.class.getSimpleName() + "...");
		logger.info("remove " + file);

		FileServerAPI api = Util.connect();

		byte[] data = api.remove(file);

		if (data == null)
			throw new IOException("Not found in memory: " + file);
	}
}
