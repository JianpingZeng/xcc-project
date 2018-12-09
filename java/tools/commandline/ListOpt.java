package tools.commandline;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Jianping Zeng.
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
import tools.OutRef;
import tools.Util;

import java.util.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class ListOpt<T> extends Option<T> implements List<T> {
  private TIntArrayList positions;

  private LinkedList<T> values;

  public ListOpt(Parser<T> parser, Modifier... mods) {
    super(parser, NumOccurrences.ZeroOrMore.value);
    positions = new TIntArrayList();
    values = new LinkedList<T>();
    for (Modifier mod : mods)
      mod.apply(this);

    done();
  }

  @Override
  public void setValue(T val) {
    add(val);
  }

  @Override
  protected boolean handleOccurrence(int pos, String optionName, String arg) {
    T val;
    OutRef<T> x = new OutRef<>();
    if (parser.parse(this, optionName, arg, x))
      return true;
    val = x.get();
    setValue(val);
    setPosition(pos);
    positions.add(pos);
    return false;
  }

  @Override
  protected ValueExpected getValueExpectedDefault() {
    return parser.getValueExpectedFlagDefault();
  }

  @Override
  public void getExtraOptionNames(ArrayList<String> names) {
    parser.getExtraOptionNames(names);
  }

  @Override
  public int getOptionWidth() {
    return parser.getOptionWidth(this);
  }

  @Override
  public void printOptionInfo(int globalWidth) {
    parser.printOptionInfo(this, globalWidth);
  }

  @Override
  public void setInitializer(T val) {
    setValue(val);
  }

  private void done() {
    addArgument();
    parser.initialize(this);
  }

  public Parser<T> getParser() {
    return parser;
  }

  public int getPosition(int index) {
    Util.assertion(index >= 0 && index < size());
    return positions.get(index);
  }

  @Override
  protected void setNumAdditionalVals(int val) {
    super.setNumAdditionalVals(val);
  }

  @Override
  public int size() {
    return values.size();
  }

  @Override
  public boolean isEmpty() {
    return values.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return values.contains(o);
  }

  @Override
  public Iterator<T> iterator() {
    return values.iterator();
  }

  @Override
  public Object[] toArray() {
    return values.toArray();
  }

  @Override
  public <T1> T1[] toArray(T1[] a) {
    return values.toArray(a);
  }

  @Override
  public boolean add(T t) {
    return values.add(t);
  }

  @Override
  public boolean remove(Object o) {
    return values.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return values.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    return values.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    return values.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return values.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return values.retainAll(c);
  }

  @Override
  public void clear() {
    values.clear();
  }

  @Override
  public T get(int index) {
    return values.get(index);
  }

  @Override
  public T set(int index, T element) {
    return values.set(index, element);
  }

  @Override
  public void add(int index, T element) {
    values.add(index, element);
  }

  @Override
  public T remove(int index) {
    return values.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return values.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return values.lastIndexOf(o);
  }

  @Override
  public ListIterator<T> listIterator() {
    return values.listIterator();
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    return values.listIterator(index);
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return values.subList(fromIndex, toIndex);
  }
}
