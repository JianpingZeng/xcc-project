package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import gnu.trove.list.array.TIntArrayList;
import tools.SourceMgr;
import tools.Util;
import utils.tablegen.Init.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class Record implements Cloneable {
  /**
   * This is a global, static, and final object for keeping the map from
   * class or def name to its def.
   */
  public static final RecordKeeper records = new RecordKeeper();
  /**
   * Sorting predicate to sort record by name.
   */
  public static final Comparator<Record> LessRecord = (o1, o2) -> o1.getName().compareTo(o2.getName());
  /**
   * Sorting predicate to sort the record by theire name field.
   */
  private static final Comparator<Record> LessRecordFieldName = (Record o1, Record o2) ->
        o1.getValueAsString("Name").compareTo(o2.getValueAsString("Name"));

  private String name;
  private ArrayList<String> templateArgs;
  private ArrayList<RecordVal> values;
  private ArrayList<Record> superClasses;
  private SourceMgr.SMLoc loc;

  /**
   * An unique ID.
   */
  private int id;
  private static int lastID = 0;

  public Record(String name, SourceMgr.SMLoc loc) {
    this.name = name;
    templateArgs = new ArrayList<>();
    values = new ArrayList<>();
    superClasses = new ArrayList<>();
    this.loc = loc;
    id = lastID++;
  }

  public SourceMgr.SMLoc getLoc() {
    return loc;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    // Also updates RecordKeeper.
    if (equals(records.getDef(getName()))) {
      records.removeDef(getName());
      this.name = name;
      records.addDef(this);
    } else {
      records.removeClass(getName());
      this.name = name;
      records.addClass(this);
    }
  }

  public ArrayList<String> getTemplateArgs() {
    return templateArgs;
  }

  public ArrayList<RecordVal> getValues() {
    return values;
  }

  public ArrayList<Record> getSuperClasses() {
    return superClasses;
  }

  public boolean isTemplateArg(String name) {
    return templateArgs.contains(name);
  }

  public RecordVal getValue(String name) {
    for (RecordVal rv : values)
      if (rv.getName().equals(name)) return rv;
    return null;
  }

  public void addTemplateArg(String name) {
    Util.assertion(!isTemplateArg(name), "Template arg already defined!");
    templateArgs.add(name);
  }

  public void addValue(RecordVal rv) {
    Util.assertion(getValue(rv.getName()) == null, "Value already defined!");
    values.add(rv.clone());
  }

  public void removeValue(String name) {
    Util.assertion(getValue(name) != null, "Cannot remove a no existing value");
    for (Iterator<RecordVal> itr = values.iterator(); itr.hasNext(); ) {
      RecordVal rv = itr.next();
      if (rv.getName().equals(name)) {
        itr.remove();
        return;
      }
    }
    Util.assertion(false, "Name does not exist in record!");
  }

  public boolean isSubClassOf(Record r) {
    for (Record R : superClasses)
      if (r == R) return true;
    return false;
  }

  public boolean isSubClassOf(String name) {
    for (Record R : superClasses)
      if (name.equals(R.getName())) return true;
    return false;
  }

  public void addSuperClass(Record r) {
    Util.assertion(!isSubClassOf(r), "Already subclass record!");
    superClasses.add(r);
  }

  public void resolveReferences() {
    resolveReferencesTo(null);
  }

  public void resolveReferencesTo(RecordVal rv) {
    for (int i = 0, e = values.size(); i < e; i++) {
      RecordVal val = values.get(i);
      Init v = val.getValue();
      if (v != null) {
        Init res = v.resolveReferences(this, rv);
        val.setValue(res);
      }
    }
  }

  public void dump() {
    print(System.err, this);
  }

  public static void print(PrintStream os, Record r) {
    os.print(r.getName());

    ArrayList<String> templateArgs = r.getTemplateArgs();
    if (!templateArgs.isEmpty()) {
      os.print("<");
      for (int i = 0, e = templateArgs.size(); i < e; i++) {
        if (i != 0) os.print(", ");
        RecordVal rv = r.getValue(templateArgs.get(i));
        Util.assertion(rv != null, "Template argument record not found!");
        rv.print(os, false);
      }
      os.print(">");
    }

    os.print(" {");
    ArrayList<Record> sc = r.getSuperClasses();
    if (!sc.isEmpty()) {
      os.print("  //");
      for (int i = 0, e = sc.size(); i < e; i++)
        os.printf(" %s", sc.get(i).getName());
    }
    os.println();

    ArrayList<RecordVal> vals = r.getValues();
    for (int i = 0, e = vals.size(); i < e; i++) {
      if (vals.get(i).getPrefix() != 0 && !r.isTemplateArg(vals.get(i).getName())) {
        os.printf("  ");
        vals.get(i).print(os, true);
      }
    }
    for (int i = 0, e = vals.size(); i < e; i++) {
      if (vals.get(i).getPrefix() == 0 && !r.isTemplateArg(vals.get(i).getName())) {
        os.printf("  ");
        vals.get(i).print(os);
      }
    }
    os.println("}");
  }

  //===--------------------------------------------------------------------===//
  // High-level methods useful to tablegen back-ends
  //

  /**
   * Return the initializer for a value with the specified name,
   * or throw an exception if the field does not exist.
   *
   * @param fieldName
   * @return
   */
  public Init getValueInit(String fieldName) {
    RecordVal rv = getValue(fieldName);
    Util.assertion(rv != null && rv.getValue() != null,
        "Reord '" + getName() + "' does not have a field"
            + " named '" + fieldName + "'!\n");
    return rv.getValue();
  }

  /**
   * This method looks up the specified field and returns
   * its value as a string, throwing an exception if the field does not exist
   * or if the value is not a string.
   */
  public String getValueAsString(String fieldName) {
    RecordVal rv = getValue(fieldName);
    if (rv == null || rv.getValue() == null)
      Util.assertion("Reord '" + getName() + "' does not have a field"
          + " named '" + fieldName + "'!\n");
    if (rv.getValue() instanceof StringInit)
      return ((StringInit) rv.getValue()).getValue();
    Util.assertion("Record `" + getName() + "', field `" + fieldName +
        "' does not have a string initializer!");
    return null;
  }

  /**
   * This method looks up the specified field and returns
   * its value as a BitsInit, throwing an exception if the field does not exist
   * or if the value is not the right type.
   *
   * @param fieldName
   * @return
   * @throws Exception
   */
  public BitsInit getValueAsBitsInit(String fieldName) {
    RecordVal rv = getValue(fieldName);
    Util.assertion(rv != null && rv.getValue() != null,
        "Reord '" + getName() + "' does not have a field"
            + " named '" + fieldName + "'!\n");
    Util.assertion(rv.getValue() instanceof BitsInit,
        "Record `" + getName() + "', field `" + fieldName +
        "' does not have a BitsInit initializer!");
    return ((BitsInit) rv.getValue());
  }

  /**
   * This method looks up the specified field and returns
   * its value as a ListInit, throwing an exception if the field does not exist
   * or if the value is not the right type.
   *
   * @param fieldName
   * @return
   * @throws Exception
   */
  public ListInit getValueAsListInit(String fieldName) {
    RecordVal rv = getValue(fieldName);
    Util.assertion(rv != null && rv.getValue() != null,
        "Reord '" + getName() + "' does not have a field"
            + " named '" + fieldName + "'!\n");
    if (rv.getValue() instanceof ListInit)
      return ((ListInit) rv.getValue());
    Util.assertion("Record `" + getName() + "', field `" + fieldName +
        "' does not have a ListInit initializer!");
    return null;
  }

  /**
   * This method looks up the specified field and
   * returnsits value as a vector of records, throwing an exception if the
   * field does not exist or if the value is not the right type.
   *
   * @param fieldName
   * @return
   * @throws Exception
   */
  public ArrayList<Record> getValueAsListOfDefs(String fieldName) {
    ListInit list = getValueAsListInit(fieldName);
    ArrayList<Record> defs = new ArrayList<>();
    for (int i = 0; i < list.getSize(); i++) {
      Init ii = list.getElement(i);
      Util.assertion(ii instanceof DefInit,
          "Record `" + getName() + "', field `" +
              fieldName + "' list is not entirely DefInit!");
      defs.add(((DefInit) ii).getDef());
    }
    return defs;
  }

  /**
   * This method looks up the specified field and returns its
   * value as a Record, throwing an exception if the field does not exist or if
   * the value is not the right type.
   *
   * @param fieldName
   * @return
   * @throws Exception
   */
  public Record getValueAsDef(String fieldName) {
    RecordVal rv = getValue(fieldName);
    Util.assertion(rv != null && rv.getValue() != null,
        "Reord '" + getName() + "' does not have a field" +
            " named '" + fieldName + "'!\n");
    Util.assertion(rv.getValue() instanceof DefInit,
        "Record `" + getName() + "', field `" + fieldName +
            "' does not have a DefInit initializer!");
    return ((DefInit) rv.getValue()).getDef();
  }

  /**
   * This method looks up the specified field and returns its
   * value as a bit, throwing an exception if the field does not exist or if
   * the value is not the right type.
   *
   * @param fieldName
   * @return
   * @throws Exception
   */
  public boolean getValueAsBit(String fieldName) {
    RecordVal rv = getValue(fieldName);
    Util.assertion(rv != null && rv.getValue() != null,
        "Reord '" + getName() + "' does not have a field" +
            " named '" + fieldName + "'!\n");
    Util.assertion (rv.getValue() instanceof BitInit,
        "Record `" + getName() + "', field `" + fieldName +
            "' does not have a BitInit initializer!");
    return ((BitInit) rv.getValue()).getValue();
  }

  /**
   * This method looks up the specified field and returns its
   * value as an int, throwing an exception if the field does not exist or if
   * the value is not the right type.
   *
   * @param fieldName
   * @return
   * @throws Exception
   */
  public long getValueAsInt(String fieldName) {
    RecordVal rv = getValue(fieldName);
    Util.assertion(rv != null && rv.getValue() != null,
        "Record '" + getName() + "' does not have a field" +
            " named '" + fieldName + "'!\n");
    Util.assertion(rv.getValue() instanceof IntInit,
        "Record `" + getName() + "', field `" + fieldName +
            "' does not have a IntInit initializer!");
    return ((IntInit) rv.getValue()).getValue();
  }

  /**
   * This method looks up the specified field and returns its
   * value as an Dag, throwing an exception if the field does not exist or if
   * the value is not the right type.
   *
   * @param fieldName
   * @return
   * @throws Exception
   */
  public Init.DagInit getValueAsDag(String fieldName) {
    RecordVal rv = getValue(fieldName);
    Util.assertion(rv != null && rv.getValue() != null,
        "Reord '" + getName() + "' does not have a field"
            + " named '" + fieldName + "'!\n");
    Util.assertion (rv.getValue() instanceof DagInit,
        "Record `" + getName() + "', field `" + fieldName +
        "' does not have a DagInit initializer!");
    return ((DagInit) rv.getValue());
  }

  /**
   * his method looks up the specified field and returns
   * its value as the string data in a CodeInit, throwing an exception if the
   * field does not exist or if the value is not a code object.
   *
   * @param fieldName
   * @return
   * @throws Exception
   */
  public String getValueAsCode(String fieldName) {
    RecordVal rv = getValue(fieldName);
    Util.assertion(rv != null && rv.getValue() != null,
        "Reord '" + getName() + "' does not have a field"
            + " named '" + fieldName + "'!\n");
    Util.assertion (rv.getValue() instanceof CodeInit,
        "Record `" + getName() + "', field `" + fieldName +
        "' does not have a CodeInit initializer!");
    return ((CodeInit) rv.getValue()).getValue();
  }

  public TIntArrayList getValueAsListOfInts(String fieldName) {
    ListInit list = getValueAsListInit(fieldName);
    Util.assertion(list != null);
    TIntArrayList res = new TIntArrayList();
    for (int i = 0; i < list.getSize(); i++) {
      IntInit ii = list.getElement(i) instanceof IntInit ?
          (IntInit) list.getElement(i) : null;
      Util.assertion(ii != null, "Record '" + getName() + "', field '" +
          fieldName + "' does not have a list of ints initializer!");
      res.add((int) ii.getValue());
    }
    return res;
  }

  @Override
  public String toString() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    print(new PrintStream(os), this);
    return os.toString();
  }

  @Override
  public Record clone() {
    try {
      Object obj = super.clone();

      Record r = (Record) obj;
      r.name = name;
      r.loc = loc;
      ArrayList<String> t = new ArrayList<>(templateArgs);
      ArrayList<RecordVal> t2 = new ArrayList<>(values);
      ArrayList<Record> s = new ArrayList<>(superClasses);
      r.templateArgs = t;
      r.values = t2;
      r.superClasses = s;
      return r;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  /**
   * Determines whether this Record is a Declaration or not.
   * <p>
   * Return {@code true} if the record values belongs to this Record is empty
   * and no super classes which this record inherits and no template arguments
   * declared.
   * </p>
   *
   * @return
   */
  public boolean isDeclaration() {
    return getValues().isEmpty()
        && getSuperClasses().isEmpty()
        && getTemplateArgs().isEmpty();
  }

  public int getID() {
    return id;
  }
}
