package temporal.file.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import temporal.file.environment.FileServerEnvironment;
import temporal.file.server.FileServerAPI;

/**
 * Sends a shutdown request to a running file server.
 *
 * <p>Connects via RMI and invokes {@link FileServerAPI#shutdown(String)}
 * with the password configured in the environment.
 *
 * <h3>Usage</h3>
 * <pre>java temporal.file.client.Shutdown</pre>
 */
public class Shutdown
{
	private static final Logger logger = Logger.getLogger(Shutdown.class.getName());

	/**
	 * Entry point.
	 *
	 * @param args command-line arguments (none expected)
	 * @throws RemoteException  if an RMI communication error occurs
	 * @throws NotBoundException if the server is not bound in the registry
	 */
	public static void main(String[] args) throws RemoteException, NotBoundException
	{
		FileServerAPI api = Util.connect();

		api.shutdown(FileServerEnvironment.variables().password());
		logger.info("shutdown message sent.");
	}
}
