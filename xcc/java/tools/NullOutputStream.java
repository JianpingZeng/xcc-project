/*
 *  Extremely Compiler Collection
 *    Copyright (c) 2015-2020, Jianping Zeng.
 *
 *  Licensed under the BSD License version 3. Please refer LICENSE for details.
 */

package tools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This is a null output stream which will discards wll contents.
 * @author  Jianping Zeng.
 */
public class NullOutputStream extends OutputStream {

  public static PrintStream nulls() {
    return new PrintStream(new NullOutputStream());
  }

  public NullOutputStream() {
    super();
  }

  @Override
  public void write(int b) { }
  @Override
  public void write(byte[] b) { }
  @Override
  public void write(byte[] buf, int off, int len) { }
  @Override
  public void flush() { }
  @Override
  public void close() { }
}
