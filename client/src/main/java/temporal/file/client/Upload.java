package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Uploads a single local file to the remote persistent store.
 *
 * <p>GZIP is the protocol: the file is GZIP-compressed before sending.
 * The server decompresses before writing to disk.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.Upload file</pre>
 */
public class Upload
{
	private static final Logger logger = Logger.getLogger(Upload.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the local file to upload
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an I/O error occurs or the file does
	 *                           not exist
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + Upload.class.getSimpleName() + " file");
			return;
		}

		final File file = new File(args[0]);

		if (!file.exists())
			throw new IOException("File not found: " + file);

		logger.info("Running " + Upload.class.getSimpleName() + "...");
		logger.info("upload " + file.getName());

		FileServerAPI api = Util.connect();

		byte[] data = Util.gzip(Util.load_raw(file));
		api.upload(file, data);
	}
}
