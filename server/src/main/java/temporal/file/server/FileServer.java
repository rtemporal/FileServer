package temporal.file.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import temporal.file.environment.FileServerEnvironment;

/**
 * RMI-based file server providing an in-memory key-value store and
 * persistent file I/O with GZIP compression on the wire.
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   main()  &rarr;  startup()  &rarr;  await shutdown  &rarr;  exit
 *                             &uarr;
 *                        RMI shutdown(password)
 * </pre>
 * {@link #startup()} creates and exports the server, binds it to an RMI
 * registry on the configured port, and returns the server instance.
 * {@code main()} then blocks on a {@link CountDownLatch} until a client
 * calls {@link #shutdown(String)} with the correct password.
 *
 * <h3>Memory store</h3>
 * Operations {@code put}, {@code get}, {@code remove}, {@code putAll},
 * {@code getAll}, and {@code removeAll} operate on an in-memory
 * {@link HashMap} keyed by {@link File} paths. Data is lost on restart.
 *
 * <h3>Persistent file store</h3>
 * Operations {@code upload}, {@code uploadAll}, {@code download},
 * {@code downloadAll}, {@code delete}, and {@code deleteAll} read and
 * write real files on disk. Network transfer uses GZIP: data sent to
 * {@code upload} methods must be compressed; data returned by
 * {@code download} methods is compressed.
 *
 * @see FileServerAPI
 */
public class FileServer implements FileServerAPI
{
	private static final Logger logger = Logger.getLogger(FileServer.class.getName());

	/**
	 * Constructs a new file server with an empty in-memory map.
	 */
	public FileServer()
	{
		fileMap = new HashMap<>();
	}

	public void put(File file, byte[] data) throws RemoteException
	{
		logger.fine(() -> "put " + file);
		fileMap.put(file, data);
	}

	public byte[] get(File file) throws RemoteException
	{
		logger.fine(() -> "get " + file);
		return fileMap.get(file);
	}

	public byte[] remove(File file)
	{
		logger.fine(() -> "remove " + file);
		return fileMap.remove(file);
	}

	public void putAll(Map<File, byte[]> fileMap) throws RemoteException
	{
		logger.fine(() -> "putAll size = " + fileMap.size());
		this.fileMap.putAll(fileMap);
	}

	public Map<File, byte[]> getAll(File directory)
	{
		logger.fine(() -> "getRecursive " + directory);
		Map<File, byte[]> outputMap = new HashMap<>();

		try
		{
			for (Entry<File, byte[]> entry : fileMap.entrySet())
			{
				File file = entry.getKey();

				if (isChild(directory, file))
				{
					logger.finer(() -> "file = " + file);
					outputMap.put(file, entry.getValue());
				}
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		return outputMap;
	}

	public void removeAll(File directory)
	{
		try
		{
			Iterator<File> iterator = fileMap.keySet().iterator();

			while (iterator.hasNext())
			{
				File file = iterator.next();

				if (isChild(directory, file))
					iterator.remove();
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void upload(File file, byte[] data) throws IOException
	{
		logger.fine(() -> "upload file = " + file);
		save(file, ungzip(data));
	}

	public void uploadAll(Map<File, byte[]> fileMap) throws IOException
	{
		logger.fine(() -> "upload size = " + fileMap.size());

		for (Entry<File, byte[]> entry : fileMap.entrySet())
		{
			File file = entry.getKey();
			byte[] data = entry.getValue();

			upload(file, data);
		}
	}

	public byte[] download(File file) throws IOException
	{
		logger.fine(() -> "download " + file.getName());

		return gzip(load(file));
	}

	public Map<File, byte[]> downloadAll(File directory) throws IOException
	{
		logger.fine(() -> "download " + directory.getName());

		return loadRecursive(directory);
	}

	public boolean delete(File file)
	{
		return file.delete();
	}

	public boolean deleteAll(File directory)
	{
		boolean result = true;

		if (directory.isDirectory())
		{
			File[] children = directory.listFiles();

			if (children != null)
			{
				for (File child : children)
					result &= deleteAll(child);
			}

			result &= directory.delete();
		}
		else
			result &= directory.delete();

		return result;
	}

	public void shutdown(String password) throws RemoteException
	{
		if (FileServerEnvironment.variables().password().equals(password))
		{
			logger.info("shutdown requested.");

			if (registry != null)
			{
				try
				{
					registry.unbind(FileServerAPI.class.getSimpleName());
					UnicastRemoteObject.unexportObject(this, false);
					UnicastRemoteObject.unexportObject(registry, false);
					registry = null;
				}
				catch (RemoteException | NotBoundException e)
				{
					logger.log(Level.WARNING, "Error during RMI shutdown", e);
				}
			}

			shutdownLatch.countDown();
		}
	}

	/**
	 * Recursively loads all files under the given file or directory,
	 * returning a map from {@link File} to GZIP-compressed content.
	 *
	 * @param file the root file or directory to traverse
	 * @return a map of every file under {@code file} to its
	 *         GZIP-compressed content
	 * @throws IOException if an I/O error occurs while reading
	 */
	private static Map<File, byte[]> loadRecursive(File file) throws IOException
	{
		Map<File, byte[]> fileMap = new HashMap<>();

		loadRecursive(fileMap, file);

		return fileMap;
	}

	/**
	 * Recursive helper that populates the given map with every file under
	 * the given path, each value being GZIP-compressed.
	 */
	private static void loadRecursive(Map<File, byte[]> fileMap, File file) throws IOException
	{
		if (file.isDirectory())
		{
			File[] children = file.listFiles();

			if (children != null)
			{
				for (File child : children)
					loadRecursive(fileMap, child);
			}
		}
		else
		{
			byte[] data = gzip(load(file));

			logger.finer(() -> "file = " + file);
			fileMap.put(file, data);
		}
	}

	/**
	 * Reads all bytes from the given file.
	 *
	 * @param file the file to read
	 * @return the file's raw bytes
	 * @throws IOException if an I/O error occurs
	 */
	private static byte[] load(File file) throws IOException
	{
		return Files.readAllBytes(file.toPath());
	}

	/**
	 * Writes raw bytes to the given file, creating parent directories as
	 * needed.
	 *
	 * @param file the destination file
	 * @param data the bytes to write
	 * @throws IOException if an I/O error occurs or parent directories
	 *                     cannot be created
	 */
	private static void save(File file, byte[] data) throws IOException
	{
		File parent = file.getParentFile();

		if (parent != null && !parent.exists() && !parent.mkdirs())
			throw new IOException("Failed to create directory: " + parent);

		try (FileOutputStream os = new FileOutputStream(file))
		{
			os.write(data);
		}
	}

	/**
	 * Compresses the given byte array with GZIP.
	 *
	 * @param data the raw bytes to compress
	 * @return GZIP-compressed data
	 * @throws IOException if an I/O error occurs during compression
	 */
	private static byte[] gzip(byte[] data) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (GZIPOutputStream gzos = new GZIPOutputStream(baos))
		{
			gzos.write(data);
		}

		return baos.toByteArray();
	}

	/**
	 * Decompresses the given GZIP-compressed byte array.
	 *
	 * @param data GZIP-compressed bytes
	 * @return decompressed raw bytes
	 * @throws IOException if an I/O error occurs during decompression
	 */
	private static byte[] ungzip(byte[] data) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(data)))
		{
			byte[] buffer = new byte[1024];

			for (int length; (length = gzis.read(buffer)) >= 0; baos.write(buffer, 0, length));
		}

		return baos.toByteArray();
	}

	/**
	 * Determines whether {@code child} resides under the directory tree
	 * rooted at {@code parent}.
	 *
	 * <p>The comparison uses canonical paths. A trailing separator is
	 * appended to {@code parent} to avoid false positives (e.g. to prevent
	 * {@code /foo/bar} from matching {@code /foo/barbaz}).
	 *
	 * @param parent the ancestor directory
	 * @param child  the file to test
	 * @return {@code true} if {@code child} is a descendant of
	 *         {@code parent}
	 * @throws IOException if a canonical path cannot be resolved
	 */
	private static boolean isChild(File parent, File child) throws IOException
	{
		String parentPath = parent.getCanonicalPath();
		String childPath = child.getCanonicalPath();

		if (!parentPath.endsWith(File.separator))
			parentPath += File.separator;

		logger.finest("isChild " + parentPath + " " + childPath);

		return childPath.startsWith(parentPath);
	}

	/**
	 * Entry point. Starts the server and blocks until a remote shutdown
	 * request is received.
	 *
	 * @param args command-line arguments (none expected)
	 * @throws RemoteException       if an RMI error occurs during startup
	 * @throws AlreadyBoundException if the RMI registry bind fails
	 * @throws InterruptedException  if the main thread is interrupted
	 *                               while awaiting shutdown
	 */
	public static void main(String[] args) throws RemoteException, AlreadyBoundException, InterruptedException
	{
		if (args.length != 0)
		{
			System.err.println("Usage: java " + FileServer.class.getSimpleName());
			return;
		}

		startup();
		shutdownLatch.await();
		logger.info("Shutdown complete.");
	}

	/**
	 * Creates and exports the file server, then binds it to a new RMI
	 * registry on the configured port.
	 *
	 * <p>The port is obtained from
	 * {@code FileServerEnvironment.variables().fileServerPortNumber()}. The exported
	 * remote object listens on {@code port + 1}.
	 *
	 * @return the server instance that was exported
	 * @throws AccessException       if the registry cannot be created
	 * @throws RemoteException       if an RMI error occurs
	 * @throws AlreadyBoundException if the name is already bound in the
	 *                               registry
	 */
	public static FileServer startup() throws AccessException, RemoteException, AlreadyBoundException
	{
		System.setProperty("java.rmi.server.hostname", FileServerEnvironment.variables().host());
		short port = FileServerEnvironment.variables().port();

		logger.info("Running " + FileServer.class.getSimpleName() + " on port " + port + '.');
		FileServer server = new FileServer();

		FileServerAPI stub = (FileServerAPI) UnicastRemoteObject.exportObject(server, port + 1);

		server.registry = LocateRegistry.createRegistry(port);
		server.registry.bind(FileServerAPI.class.getSimpleName(), stub);
		logger.info(FileServer.class.getSimpleName() + " started.");
		return server;
	}

	private Map<File, byte[]> fileMap;
	private Registry registry;
	private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
}
