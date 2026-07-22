package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Removes all entries under a directory from the remote in-memory store.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.RemoveAll directory</pre>
 */
public class RemoveAll
{
	private static final Logger logger = Logger.getLogger(RemoveAll.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the directory to filter by in the
	 *             in-memory store
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an RMI error occurs
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + RemoveAll.class.getSimpleName() + " directory");
			return;
		}

		final File file = new File(args[0]);

		logger.info("Running " + RemoveAll.class.getSimpleName() + "...");
		logger.info("removeAll " + file);

		FileServerAPI api = Util.connect();
		api.removeAll(file);
	}
}
