package utils.tablegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import tools.Util;

import java.util.*;

/**
 * This class is designed to expand a dag expression to a set of record.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class SetTheory {
  interface Operator {
    /**
     * This function is used to expand a dag expression into a set of records.
     * And save the results to the {@code elts}.
     * @param st
     * @param expr
     * @param elts
     */
    void apply(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> elts);
  }

  /**
   * This class is used to expand a record representing a set into a fully
   * expanded list of elements. The result elements list will be saved to
   * {@code elts}.
   */
  interface Expander {
    void expand(SetTheory st, Record rec, LinkedHashSet<Record> elts);
  }

  /**
   * (add, a, b, ...) Evaluate and union all elements.
   */
  private static class AddOp implements Operator {
    @Override
    public void apply(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> elts) {
      for (int i = 0, e = expr.getNumArgs(); i < e; i++)
        st.evaluate(expr.getArg(i), elts);
    }
  }

  private static class SubOp implements Operator {
    @Override
    public void apply(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> elts) {
      if (expr.getNumArgs() < 2)
        Util.assertion(String.format("Set difference needs at least two arguments: %s", expr.toString()));

      LinkedHashSet<Record> add = new LinkedHashSet<>(), sub = new LinkedHashSet<>();
      st.evaluate(expr.getArg(0), add);
      for (int i = 1, e = expr.getNumArgs(); i < e; ++i)
        st.evaluate(expr.getArg(i), sub);

      add.forEach(e -> {
        if (!sub.contains(e))
          elts.add(e);
      });
    }
  }

  private static class AndOp implements Operator {
    @Override
    public void apply(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> elts) {
      if (expr.getNumArgs() != 2)
        Util.assertion(String.format("Set difference needs exactly two arguments: %s", expr.toString()));

      LinkedHashSet<Record> lhs = new LinkedHashSet<>(), rhs = new LinkedHashSet<>();
      st.evaluate(expr.getArg(0), lhs);
      st.evaluate(expr.getArg(1), rhs);
      lhs.forEach(e -> {
        if (rhs.contains(e))
          elts.add(e);
      });
    }
  }

  /**
   * Abstract base class for (Op S, N) operators.
   */
  private static abstract class SetIntBinOp implements Operator {
    @Override
    public void apply(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> elts) {
      Util.assertion(expr.getNumArgs() == 2, String.format("Operator requires exactly two arguments: %s", expr.toString()));
      LinkedHashSet<Record> set = new LinkedHashSet<>();
      st.evaluate(expr.getArg(0), set);
      Util.assertion(expr.getArg(1) instanceof Init.IntInit,
          String.format("The second argument of '%s' must be an integer", expr.toString()));
      Init.IntInit ii = (Init.IntInit) expr.getArg(1);
      apply2(st, expr, set, ii.getValue(), elts);
    }

    abstract void apply2(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> set,
                         long n, LinkedHashSet<Record> elts);
  }

  private static class ShlOp extends SetIntBinOp {
    @Override
    void apply2(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> set, long n, LinkedHashSet<Record> elts) {
      Util.assertion(n >= 0, String.format("Positive shift required: %s", expr.toString()));
      if (n < set.size()) {
        int i = 0;
        for (Record e : set) {
          if (i >= n)
            elts.add(e);
          ++i;
        }
      }
    }
  }

  private static class TruncOp extends SetIntBinOp {
    @Override
    void apply2(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> set, long n, LinkedHashSet<Record> elts) {
      Util.assertion(n >= 0, String.format("Positive shift required: %s", expr.toString()));
      if (n > set.size())
        n = set.size();

      int i = 0;
      for (Record e : set) {
        if (i < n)
          elts.add(e);
        ++i;
      }
    }
  }

  private static class RotOp extends SetIntBinOp {
    private boolean reverse;
    RotOp(boolean reverse) { this.reverse = reverse; }

    @Override
    void apply2(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> set, long n, LinkedHashSet<Record> elts) {
      if (reverse)
        n = -n;

      // n > 0 -> rotate left, n < 0 ==> rotate right.
      if (set.isEmpty())
        return;

      if (n < 0)
        n = set.size() - (-n % set.size());
      else
        n %= set.size();

      int i = 0;
      for (Record e : set) {
        if (i >= n)
          elts.add(e);
        ++i;
      }

      i = 0;
      for (Record e : set) {
        if (i < n)
          elts.add(e);
        ++i;
      }
    }
  }

  private static class DecimateOp extends SetIntBinOp {
    @Override
    void apply2(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> set, long n, LinkedHashSet<Record> elts) {
      Util.assertion(n > 0, String.format("Positive shift required: %s", expr.toString()));

      int i = 0;
      for (Record e : set) {
        if ((i % n) == 0)
          elts.add(e);
        ++i;
      }
    }
  }

  private static class SequenceOp implements Operator {

    @Override
    public void apply(SetTheory st, Init.DagInit expr, LinkedHashSet<Record> elts) {
      Util.assertion(expr.getNumArgs() == 3, String.format("Bad args to (sequence \"format\", from ,to): $s", expr.toString()));
      Util.assertion(expr.getArg(0) instanceof Init.StringInit, "Format must be a string");
      String format = ((Init.StringInit)expr.getArg(0)).getValue();

      Util.assertion(expr.getArg(1) instanceof Init.IntInit, "from must be an integer");
      Util.assertion(expr.getArg(2) instanceof Init.IntInit, "to must be an integer");
      long from = ((Init.IntInit)expr.getArg(1)).getValue();
      long to = ((Init.IntInit)expr.getArg(2)).getValue();
      Util.assertion(from >= 0 && from < (1 << 30), "from out of range");
      Util.assertion(to >= 0 && to < (1 << 30), "to out of range");

      int step = from <= to ? 1 : -1;
      for (to += step; from != to; from += step) {
        String name = String.format(format, from);
        Record rec = Record.records.getDef(name);
        Util.assertion(rec != null, String.format("No def named '%s': %s", name, expr.toString()));
        ArrayList<Record> result = st.expand(rec);
        if (result != null)
          elts.addAll(result);
        else
          elts.add(rec);
      }
    }
  }

  private static class FieldNameExpander implements Expander {
    private String fieldName;
    FieldNameExpander(String fieldName) { this.fieldName = fieldName; }

    @Override
    public void expand(SetTheory st, Record rec, LinkedHashSet<Record> elts) {
      st.evaluate(rec.getValueInit(fieldName), elts);
    }
  }


  private HashMap<Record, ArrayList<Record>> expandedMap;
  private TreeMap<String, Operator> operators;
  private TreeMap<String, Expander> expanders;

  public SetTheory() {
    expandedMap = new HashMap<>();
    operators = new TreeMap<>();
    expanders = new TreeMap<>();

    addOperator("add", new AddOp());
    addOperator("sub", new SubOp());
    addOperator("and", new AndOp());
    addOperator("shl", new ShlOp());
    addOperator("trunc", new TruncOp());
    addOperator("rotl", new RotOp(false));
    addOperator("rotr", new RotOp(true));
    addOperator("decimate", new DecimateOp());
    addOperator("sequence", new SequenceOp());
  }

  /**
   * Associate the operator name, such as add, and, sequence, etc, to the
   * operator instance.
   * @param name
   * @param op
   */
  public void addOperator(String name, Operator op) {
    operators.put(name, op);
  }

  public void addExpander(String className, Expander e) {
    expanders.put(className, e);
  }

  public void addFieldExpander(String className, String fieldName) {
    addExpander(className, new FieldNameExpander(fieldName));
  }

  void evaluate(Init expr, LinkedHashSet<Record> elts) {
    // A def in a list can be just be an element, or it may expand.
    if (expr instanceof Init.DefInit) {
      Init.DefInit df = (Init.DefInit) expr;
      ArrayList<Record> res = expand(df.getDef());
      if (res != null)
        elts.addAll(res);
      else
        elts.add(df.getDef());
      return;
    }

    // list simply expand.
    if (expr instanceof Init.ListInit) {
      Init.ListInit li = (Init.ListInit) expr;
      for (int i = 0, e = li.getSize(); i < e; i++)
        evaluate(li.getElement(i), elts);
      return;
    }

    Util.assertion(expr instanceof Init.DagInit, String.format("Invalid set element: %s", expr.toString()));
    Init.DagInit di = (Init.DagInit) expr;
    Util.assertion(di.getOperator() instanceof Init.DefInit, String.format("Bad set expression: %s", expr.toString()));
    Init.DefInit def = (Init.DefInit) di.getOperator();
    Util.assertion(operators.containsKey(def.getDef().getName()), String.format("Unknown set operation: %s", expr.toString()));
    Operator op = operators.get(def.getDef().getName());
    op.apply(this, di, elts);
  }

  public ArrayList<Record> expand(Record set) {
    // check existing entries for set and return early.
    if (expandedMap.containsKey(set))
      return expandedMap.get(set);

    // Create a new one.
    ArrayList<Record> scs = set.getSuperClasses();
    for (Record sc : scs) {
      if (expanders.containsKey(sc.getName())) {
        Expander exp = expanders.get(sc.getName());
        LinkedHashSet<Record> elts = new LinkedHashSet<>();
        exp.expand(this, set, elts);
        ArrayList<Record> eltVec = new ArrayList<>(elts);
        expandedMap.put(set, eltVec);
        return eltVec;
      }
    }
    // set is not expandable.
    return null;
  }
}
