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

import tools.Util;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class RecordKeeper
{
    private HashMap<String, Record> classes, defs;

    public RecordKeeper()
    {
        classes = new HashMap<>();
        defs = new HashMap<>();
    }

    public HashMap<String, Record> getClasses()
    {
        return classes;
    }

    public HashMap<String, Record> getDefs()
    {
        return defs;
    }

    public Record getClass(String name)
    {
        if (classes.containsKey(name))
            return classes.get(name);
        return null;
    }

    public Record getDef(String name)
    {
        if (defs.containsKey(name))
            return defs.get(name);
        return null;
    }

    public void addClass(Record r)
    {
        Util.assertion(getClass(r.getName()) == null, "Class already exist!");
        classes.put(r.getName(), r);
    }

    public void addDef(Record r)
    {
        Util.assertion(getDef(r.getName()) == null, "Def already exist!");
        defs.put(r.getName(), r);
    }

    public void removeClass(String name)
    {
        Util.assertion(classes.containsKey(name), "Class does not exist!");
        classes.remove(name);
    }

    public void removeDef(String name)
    {
        Util.assertion(defs.containsKey(name), "Def does not exist!");
        defs.remove(name);
    }

    //===--------------------------------------------------------------------===//
    // High-level helper methods, useful for tablegen backends.

    /**
     * This method returns all concrete definitions that derive from the
     * specified class name.  If a class with the specified name does not
     * exist, an exception is thrown.
     * @param className
     * @return
     */
    public ArrayList<Record> getAllDerivedDefinition(String className)
            throws Exception
    {
        Record klass = getClass(className);
        if (klass == null)
            throw new Exception("UNKNOWN: Couldn't find the `" + className
                    + "' class!\n");

        ArrayList<Record> defs = new ArrayList<>();
        List<Record> res = getDefs().values().stream().filter(val->
            val.isSubClassOf(klass)
        ).collect(Collectors.toList());

        defs.addAll(res);
        return defs;
    }

    public void dump()
    {
        print(System.err, this);
    }

    public static void print(PrintStream os, RecordKeeper rk)
    {
        os.println("------------- Classes -----------------");
        HashMap<String, Record> classes = rk.getClasses();
        classes.values().forEach(r->{
            os.print("class ");
            Record.print(os, r);
        });

        os.println("------------- defs -----------------");
        HashMap<String, Record> defs = rk.getDefs();
        defs.values().forEach(def->
        {
            os.print("def ");
            Record.print(os, def);
        });
    }
}
