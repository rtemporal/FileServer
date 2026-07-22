package temporal.file.server;

import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * RMI remote interface for the file server.
 *
 * <p>Exposes two independent storage backends:
 * <ul>
 *   <li><b>In-memory store</b> &mdash; operations on a {@code HashMap} keyed
 *       by {@link File} paths. Data is held in RAM and lost on restart.</li>
 *   <li><b>Persistent file store</b> &mdash; operations that read and write
 *       actual files on disk. Network transfer uses GZIP compression.</li>
 * </ul>
 *
 * <p>Administrative shutdown is protected by a password configured through
 * {@code Environment.variables().fileServerPassword()}.
 *
 * @see FileServer
 */
public interface FileServerAPI extends Remote
{
	/*
	 * Memory server.
	 */

	/**
	 * Stores data in the in-memory map under the given file key.
	 *
	 * @param file the key used to retrieve the data
	 * @param data the raw bytes to store
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public void put(File file, byte[] data) throws RemoteException;

	/**
	 * Retrieves data from the in-memory map.
	 *
	 * @param file the key whose associated data is to be returned
	 * @return the data associated with {@code file}, or {@code null} if absent
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public byte[] get(File file) throws RemoteException;

	/**
	 * Removes the entry for the given key from the in-memory map.
	 *
	 * @param file the key to remove
	 * @return the previous value associated with {@code file}, or
	 *         {@code null} if the key was not present
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public byte[] remove(File file) throws RemoteException;

	/**
	 * Copies all entries from the supplied map into the in-memory store.
	 *
	 * @param fileMap the entries to store
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public void putAll(Map<File, byte[]> fileMap) throws RemoteException;

	/**
	 * Returns all in-memory entries whose key is a child of the given
	 * directory.
	 *
	 * <p>A file {@code f} is considered a child of {@code directory} if its
	 * canonical path starts with the canonical path of {@code directory}
	 * (with a trailing separator appended).
	 *
	 * @param directory the parent directory to filter by
	 * @return a map of matching file-to-data entries
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public Map<File, byte[]> getAll(File directory) throws RemoteException;

	/**
	 * Removes all in-memory entries whose key is a child of the given
	 * directory.
	 *
	 * @param directory the parent directory to filter by
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public void removeAll(File directory) throws RemoteException;

	/*
	 * File server.
	 */

	/**
	 * Uploads a single file to persistent storage.
	 *
	 * <p>The supplied {@code data} is expected to be GZIP-compressed. The
	 * server decompresses it before writing to disk.
	 *
	 * @param file the destination path on disk
	 * @param data GZIP-compressed file content
	 * @throws RemoteException if an RMI communication error occurs
	 * @throws IOException     if an I/O error occurs while writing the file
	 */
	public void upload(File file, byte[] data) throws RemoteException, IOException;

	/**
	 * Uploads multiple files to persistent storage.
	 *
	 * <p>Each value is expected to be GZIP-compressed and is decompressed
	 * before writing.
	 *
	 * @param fileMap the files and their GZIP-compressed contents
	 * @throws RemoteException if an RMI communication error occurs
	 * @throws IOException     if an I/O error occurs while writing a file
	 */
	public void uploadAll(Map<File, byte[]> fileMap) throws RemoteException, IOException;

	/**
	 * Downloads a single file from persistent storage.
	 *
	 * <p>The returned data is GZIP-compressed.
	 *
	 * @param file the path to read from disk
	 * @return GZIP-compressed file content
	 * @throws RemoteException if an RMI communication error occurs
	 * @throws IOException     if an I/O error occurs while reading the file
	 */
	public byte[] download(File file) throws RemoteException, IOException;

	/**
	 * Downloads all files recursively under the given directory.
	 *
	 * <p>Each value in the returned map is GZIP-compressed.
	 *
	 * @param directory the root directory to traverse
	 * @return a map of every file under {@code directory} to its
	 *         GZIP-compressed content
	 * @throws RemoteException if an RMI communication error occurs
	 * @throws IOException     if an I/O error occurs while reading files
	 */
	public Map<File, byte[]> downloadAll(File directory) throws RemoteException, IOException;

	/**
	 * Deletes a single file from persistent storage.
	 *
	 * @param file the file to delete
	 * @return {@code true} if the file was successfully deleted
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public boolean delete(File file) throws RemoteException;

	/**
	 * Recursively deletes a directory and all its contents from persistent
	 * storage.
	 *
	 * @param directory the directory to delete
	 * @return {@code true} if all files and the directory were successfully
	 *         deleted
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public boolean deleteAll(File directory) throws RemoteException;

	/*
	 * Admin.
	 */

	/**
	 * Requests a remote shutdown of the file server.
	 *
	 * <p>The password is compared against the value configured in the
	 * environment variable {@code fileServerPassword}. If the password
	 * matches, the server unbinds from the RMI registry, unexports its
	 * remote objects, and releases the main thread so the JVM can exit.
	 *
	 * @param password the shutdown password
	 * @throws RemoteException if an RMI communication error occurs
	 */
	public void shutdown(String password) throws RemoteException;
}
