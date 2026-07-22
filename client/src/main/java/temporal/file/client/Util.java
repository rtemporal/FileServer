package temporal.file.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import temporal.file.environment.FileServerEnvironment;
import temporal.file.server.FileServerAPI;

/**
 * Shared utilities for file client operations.
 *
 * <p>GZIP is the wire and in-memory protocol: {@link #load(File)} returns
 * GZIP-compressed data, {@link #save(File, byte[])} expects GZIP-compressed
 * input and decompresses before writing.
 *
 * <p>{@link #load_raw(File)} and {@link #save_raw(File, byte[])} bypass
 * GZIP for direct raw byte I/O.
 */
public class Util
{
	private static final Logger logger = Logger.getLogger(Util.class.getName());

	/**
	 * Connects to the file server via RMI.
	 *
	 * @return a stub for the remote {@link FileServerAPI}
	 * @throws RemoteException  if an RMI communication error occurs
	 * @throws NotBoundException if the server name is not bound in the registry
	 */
	public static FileServerAPI connect() throws RemoteException, NotBoundException
	{
		String host = FileServerEnvironment.variables().host();
		short port = FileServerEnvironment.variables().port();

		logger.fine(() -> "Connecting to " + host + ":" + port);
		Registry registry = LocateRegistry.getRegistry(host, port);
		return (FileServerAPI) registry.lookup(FileServerAPI.class.getSimpleName());
	}

	/**
	 * Loads all files under the given paths, returning a map from
	 * {@link File} to GZIP-compressed content.
	 *
	 * @param fileArray the files or directories to load
	 * @return a map of every file to its GZIP-compressed content
	 * @throws IOException if an I/O error occurs
	 */
	public static Map<File, byte[]> loadAll(File... fileArray) throws IOException
	{
		Map<File, byte[]> fileMap = new HashMap<>(fileArray.length);

		for (File file : fileArray)
			loadRecursive(fileMap, file);

		return fileMap;
	}

	/**
	 * Recursively loads all files under the given path into the map.
	 * Each value is GZIP-compressed.
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
			fileMap.put(file, load(file));
	}

	/**
	 * Reads a file and returns its GZIP-compressed content.
	 *
	 * @param file the file to read
	 * @return GZIP-compressed bytes
	 * @throws IOException if an I/O error occurs
	 */
	public static byte[] load(File file) throws IOException
	{
		return gzip(load_raw(file));
	}

	/**
	 * Reads a file and returns its raw (uncompressed) content.
	 *
	 * @param file the file to read
	 * @return raw bytes
	 * @throws IOException if an I/O error occurs
	 */
	public static byte[] load_raw(File file) throws IOException
	{
		return Files.readAllBytes(file.toPath());
	}

	/**
	 * Saves all entries in the map to disk, decompressing each value.
	 *
	 * @param fileMap map from file path to GZIP-compressed content
	 * @throws IOException if an I/O error occurs
	 */
	public static void save(Map<File, byte[]> fileMap) throws IOException
	{
		for (Entry<File, byte[]> entry : fileMap.entrySet())
			save(entry.getKey(), entry.getValue());
	}

	/**
	 * Saves GZIP-compressed data to disk, decompressing it first.
	 *
	 * @param file the destination file
	 * @param data GZIP-compressed content
	 * @throws IOException if an I/O error occurs
	 */
	public static void save(File file, byte[] data) throws IOException
	{
		save_raw(file, ungzip(data));
	}

	/**
	 * Saves raw bytes to disk, creating parent directories as needed.
	 *
	 * @param file the destination file
	 * @param data the raw bytes to write
	 * @throws IOException if an I/O error occurs or parent directories
	 *                     cannot be created
	 */
	public static void save_raw(File file, byte[] data) throws IOException
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
	public static byte[] gzip(byte[] data) throws IOException
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
	public static byte[] ungzip(byte[] data) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(data)))
		{
			byte[] buffer = new byte[1024];

			for (int length; (length = gzis.read(buffer)) >= 0; baos.write(buffer, 0, length));
		}

		return baos.toByteArray();
	}
}
