package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Downloads a single file from the remote file server to local disk.
 *
 * <p>Data returned by the server is GZIP-compressed. This client
 * decompresses it before writing to disk.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.Download file</pre>
 */
public class Download
{
	private static final Logger logger = Logger.getLogger(Download.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the file to download (server path)
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an RMI error occurs or the file was not
	 *                           found on the server
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + Download.class.getSimpleName() + " file");
			return;
		}

		final File file = new File(args[0]);

		logger.info("Running " + Download.class.getSimpleName() + "...");
		logger.info("download " + file.getName());

		FileServerAPI api = Util.connect();

		byte[] data = api.download(file);

		if (data == null)
			throw new IOException("File not found: " + file);

		Util.save(file, data);
	}
}
