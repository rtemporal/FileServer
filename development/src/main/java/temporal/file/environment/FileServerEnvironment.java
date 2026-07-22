package temporal.file.environment;

import temporal.file.variables.FileServerVariables;

public class FileServerEnvironment
{
	public static void main(String[]	args)
	{
		System.out.println(variables);
	}

	public static FileServerVariables	variables()
	{
		return variables;
	}

	private static FileServerVariables	variables = new FileServerVariables
	(
		System.getProperty("temporal.file.environment.host", "localhost"),
		System.getProperty("temporal.file.environment.password", "change-me"),
		(short)Integer.parseInt(System.getProperty("temporal.file.environment.port", "1100"))
	);
}
