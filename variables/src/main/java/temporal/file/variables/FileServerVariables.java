package temporal.file.variables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;

public record FileServerVariables
(
	String	host,
	String	password,
	short	port
)
{
	public String	toString()
	{
		StringBuilder	stringBuilder = new StringBuilder();

		stringBuilder.append("hashCode = ").append(hashCode()).append('\n');
		stringBuilder.append(getClass().getName()).append('\n');
		stringBuilder.append("{\n");

		for(RecordComponent	rc : FileServerVariables.class.getRecordComponents())
		{
			Object	value;

			try
			{
				value = rc.getAccessor().invoke(this, new Object[] {});
			}
			catch (IllegalAccessException | InvocationTargetException e)
			{
				throw new RuntimeException(e);
			}

			stringBuilder.append('\t').append(rc.getName()).append(" = ").append(value).append('\n');
		}

		stringBuilder.append("}\n");

		return stringBuilder.toString();
	}

	public static void main(String[]	args)
	{
		System.out.format("hashCode = %d\n", FileServerVariables.class.hashCode());
		System.out.println(FileServerVariables.class.getSimpleName());
		System.out.println("{");

		for(RecordComponent	rc : FileServerVariables.class.getRecordComponents())
			System.out.format("\t%s\t%s\n", rc.getType().getSimpleName(), rc.getName());

		System.out.println("}");
	}
}
