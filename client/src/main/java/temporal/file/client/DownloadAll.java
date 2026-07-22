package temporal.file.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import temporal.file.server.FileServerAPI;

/**
 * Downloads all files under a directory from the remote file server to
 * local disk.
 *
 * <p>Each value returned by the server is GZIP-compressed. This client
 * decompresses before writing each file.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.DownloadAll directory</pre>
 */
public class DownloadAll
{
	private static final Logger logger = Logger.getLogger(DownloadAll.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args {@code args[0]} — the directory to download (server path)
	 * @throws NotBoundException if the server is not bound in the RMI registry
	 * @throws IOException       if an RMI error occurs or the directory was
	 *                           not found on the server
	 */
	public static void main(String[] args) throws NotBoundException, IOException
	{
		if (args.length != 1)
		{
			System.err.println("Usage: java " + DownloadAll.class.getSimpleName() + " directory");
			return;
		}

		final File directory = new File(args[0]);

		logger.info("Running " + DownloadAll.class.getSimpleName() + "...");
		logger.info("downloadAll " + directory.getName());

		FileServerAPI api = Util.connect();

		Map<File, byte[]> fileMap = api.downloadAll(directory);

		if (fileMap == null)
			throw new IOException("Directory not found: " + directory);

		directory.mkdir();

		for (Entry<File, byte[]> entry : fileMap.entrySet())
		{
			File file = entry.getKey();
			byte[] data = entry.getValue();

			Util.save(file, data);
		}
	}
}
