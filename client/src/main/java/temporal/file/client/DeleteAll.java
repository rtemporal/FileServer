package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Recursively deletes a directory and all its contents from the remote
 * file server.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.DeleteAll directory</pre>
 */
public class DeleteAll
{
	private static final Logger logger = Logger.getLogger(DeleteAll.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the directory to delete on the server
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an RMI error occurs or the delete fails
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + DeleteAll.class.getSimpleName() + " directory");
			return;
		}

		final File directory = new File(args[0]);

		logger.info("Running " + DeleteAll.class.getSimpleName() + "...");
		logger.info("deleteAll " + directory.getName());

		FileServerAPI api = Util.connect();

		if (!api.deleteAll(directory))
			throw new IOException("Failed to delete: " + directory);
	}
}
