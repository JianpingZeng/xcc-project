/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.codegen.pbqp;

import tools.Util;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class PBQPMatrix {
  private double[] data;
  int rows;
  int columns;

  public PBQPMatrix(int rows, int columns) {
    Util.assertion(rows > 1 && columns > 1);
    this.rows = rows;
    this.columns = columns;
    data = new double[rows * columns];
  }

  public double get(int i, int j) {
    Util.assertion(i >= 0 && i < rows, "rows index out of range");
    Util.assertion(j >= 0 && j < columns, "columns index out of range");
    return data[i * columns + j];
  }

  public void set(int i, int j, double cost) {
    Util.assertion(i >= 0 && i < rows, "rows index out of range");
    Util.assertion(j >= 0 && j < columns, "columns index out of range");
    data[i * columns + j] = cost;
  }

  public double[] getRows(int i) {
    Util.assertion(i >= 0 && i < rows, "rows index out of range");
    return Arrays.copyOfRange(data, i * columns, (i + 1) * columns);
  }

  public double[] getDiagonalize() {
    double[] res = new double[rows];
    for (int i = 0; i < rows; i++)
      res[i] = data[i * columns + i];

    return res;
  }

  public PBQPMatrix transpose() {
    double[] res = new double[data.length];
    System.arraycopy(data, 0, res, 0, res.length);

    for (int i = 0; i < res.length; i++) {
      int row = i / columns;
      int col = i % columns;
      if (row < col) {
        double temp = res[i];
        res[i] = res[col * columns + row];
        res[col * columns + row] = temp;
      }
    }
    PBQPMatrix result = new PBQPMatrix(rows, columns);
    result.data = res;
    return result;
  }

  public void add(PBQPMatrix rhs) {
    Util.assertion(rhs != null);
    Util.assertion(rows == rhs.rows && columns == rhs.columns);
    for (int i = 0; i < data.length; i++)
      data[i] += rhs.data[i];
  }

  public double getRowMin(int row) {
    Util.assertion(row >= 0 && row < rows);
    double min = Double.MIN_VALUE;
    for (int i = 0; i < columns; i++)
      if (data[row * columns + i] < min)
        min = data[row * columns + i];
    return min;
  }

  public void subRow(int row, double val) {
    Util.assertion(row >= 0 && row < rows);
    for (int i = 0; i < columns; i++)
      data[row * columns + i] += -val;
  }

  public void setRow(int row, double val) {
    Util.assertion(row >= 0 && row < rows);
    for (int i = 0; i < columns; i++)
      data[row * columns + i] = val;
  }

  public double getColumnMin(int col) {
    Util.assertion(col >= 0 && col < columns);
    double min = Double.MIN_VALUE;
    for (int i = 0; i < rows; i++)
      if (data[i * columns + col] < min)
        min = data[i * columns + col];
    return min;
  }

  public void subCol(int col, double val) {
    Util.assertion(col >= 0 && col < columns);
    for (int i = 0; i < rows; i++)
      data[i * columns + col] += -val;
  }

  public void setCol(int col, double val) {
    Util.assertion(col >= 0 && col < columns);
    for (int i = 0; i < rows; i++)
      data[i * columns + col] = val;
  }

  public boolean isZero() {
    for (double val : data)
      if (!(-0.0000001 <= val && val <= 0.0000001))
        return false;
    return true;
  }

  /**
   * Add a vector to the specified row of this matrix.
   *
   * @param row
   * @param vec
   */
  public void addRow(int row, PBQPVector vec) {
    Util.assertion(vec.getLength() == columns);
    for (int i = 0; i < columns; i++)
      data[row * columns + i] += vec.get(i);
  }

  public void print(PrintStream os) {
    if (os != null) {
      for (int i = 0; i < data.length; i++) {
        if (i % columns == 0)
          os.println();
        if (data[i] == Double.MAX_VALUE)
          os.print("+Inf");
        else
          os.printf("%.2f", data[i]);
        if (i % columns != columns - 1)
          os.print(",");
      }
      os.println();
    }
  }

  public void dump() {
    print(System.err);
  }
}
