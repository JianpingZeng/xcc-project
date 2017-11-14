package backend.support;

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface Printable
{
	void print(PrintStream os);
	default void dump()
	{
		print(System.err);
	}
}
