package backend.support;

import backend.value.Module;

import java.io.PrintStream;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public interface Printable {
  void print(PrintStream os, Module m);

  default void dump() {
    print(System.err, null);
  }
}
