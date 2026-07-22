package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Deletes a single file from the remote file server.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.Delete file</pre>
 */
public class Delete
{
	private static final Logger logger = Logger.getLogger(Delete.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the file to delete on the server
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an RMI error occurs or the delete fails
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + Delete.class.getSimpleName() + " file");
			return;
		}

		final File file = new File(args[0]);

		logger.info("Running " + Delete.class.getSimpleName() + "...");
		logger.info("delete " + file.getName());

		FileServerAPI api = Util.connect();

		if (!api.delete(file))
			throw new IOException("Failed to delete: " + file);
	}
}
