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

package utils.tablegen;

import tools.Util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class JlangOptionsEmitter extends TableGenBackend {
  private RecordKeeper records;

  public JlangOptionsEmitter(RecordKeeper records) {
    this.records = records;
  }

  private static final Comparator<Record> OptionComparator = new Comparator<Record>() {
    @Override
    public int compare(Record r1, Record r2) {
      try {
        if (r1 == r2)
          return 0;

        boolean sen1 = r1.getValueAsDef("Kind").getValueAsBit("Sentinel");
        boolean sen2 = r2.getValueAsDef("Kind").getValueAsBit("Sentinel");
        if (sen1 != sen2)
          return sen1 ? -1 : 1;
        if (!sen1) {
          String name1 = r1.getValueAsString("Name");
          String name2 = r2.getValueAsString("Name");
          if (!name1.equals(name2))
            return name1.compareTo(name2);
        }
        long pred1 = r1.getValueAsDef("Kind").getValueAsInt("Precedence");
        long pred2 = r2.getValueAsDef("Kind").getValueAsInt("Precedence");
        if (pred1 == pred2) {
          r1.getValueAsDef("Kind").dump();
          r2.getValueAsDef("Kind").dump();
        }
        Util.assertion(pred1 != pred2, "Two equivalent options");
        return pred1 < pred2 ? -1 : 1;
      } catch (Exception e) {
        e.printStackTrace();
      }
      return 0;
    }
  };

  private static String getOptionName(Record option) {
    Init init = option.getValueInit("EnumName");
    if (init instanceof Init.UnsetInit)
      return option.getValueAsString("Name");
    return option.getValueAsString("EnumName");
  }

  /**
   * <pre>
   * The mangling scheme is to ignore the leading '-', and perform the
   * following substitutions:
   *   _ => __
   *   - => _
   *   < => _
   *   > => _
   *   # => _HASH
   *   , => _COMMA
   *   = => _EQ
   *   C++ => CXX
   * </pre>
   *
   * @param id
   * @return
   */
  private String manglingValidID(String id) {
    StringBuilder buf = new StringBuilder();
    int i = 0, e = id.length();
    for (; i < e; i++) {
      char ch = id.charAt(i);
      switch (ch) {
        case '_':
          buf.append("__");
          break;
        case '-':
        case '<':
        case '>':
        case ' ':
        case '(':
        case ')':
        case '/':
        case '.':
          buf.append('_');
          break;
        case '#':
          buf.append("_HASH");
          break;
        case ',':
          buf.append("_COMMA");
          break;
        case '=':
          buf.append("_EQ");
          break;
        case 'c':
        case 'C':
          if (i + 2 < e && id.substring(i + 1, i + 3).equals("++")) {
            buf.append("CXX");
            i += 2;
            break;
          }
          // fall through
        default:
          buf.append(ch);
          break;
      }
    }
    return buf.toString();
  }

  private static String escapeString(String str) {
    if (str == null || str.isEmpty()) return "";

    StringBuilder buf = new StringBuilder();
    buf.append(str);
    for (int i = 0; i < buf.length(); i++) {
      switch (buf.charAt(i)) {
        case '\n':
          buf.insert(i, '\\');
          ++i;
          buf.setCharAt(i, 'n');
          break;
        case '\t':
          buf.insert(i, ' '); // convert to two spaces.
          ++i;
          buf.setCharAt(i, ' ');
          break;
        case '"':
          buf.insert(i, '\\');
          ++i;
          break;
      }
    }
    return buf.toString();
  }

  /**
   * Emit OptionInfo ID for each option.
   *
   * @param options
   * @param os
   */
  private void emitOptionIDs(ArrayList<Record> options, PrintStream os) {
    emitSourceFileHeaderComment("OptionInfo ID definitions for JlangTool driver", os);

    os.printf("package xcc;%n%n");
    os.println("//This class defines some sort of static Constant for representing OptionInfo ID.");
    os.println("public interface OptionID");
    os.println("{");
    os.printf("\tint OPT_INVALID = -1;%n");
    os.printf("\tint OPT__unknown_ = 0;%n");
    os.printf("\tint OPT__input_ = 1;%n");
    int i = 2;
    TreeSet<String> ids = new TreeSet<>();
    for (Record opt : options) {
      String name = manglingValidID(opt.getValueAsString("Name"));
      if (name.equals("_unknown_") || name.equals("_input_"))
        continue;
      ids.add(name);
    }
    for (String id : ids)
      os.printf("\tint OPT_%s = %d;%n", id, i++);
    os.printf("\tint OPT_LastOption = %d;%n", i);
    os.println("}\n\n");
  }

  private void emitOptionKind(ArrayList<Record> options, PrintStream os) {
    emitSourceFileHeaderComment("OptionInfo Kinds definitions for JlangTool driver", os);

    os.printf("package xcc;%n%n");

    os.println("//This class defines some sort of static Constant for representing OptionInfo Kind.");
    os.println("public interface OptionKind");
    os.println("{");
    int i = 0;
    TreeSet<String> kinds = new TreeSet<>();
    for (Record opt : options) {
      kinds.add(opt.getValueAsDef("Kind").getValueAsString("Name"));
    }
    for (String kind : kinds) {
      os.printf("\tint KIND_%s = %d;%n", kind, i++);
    }
    os.println("}\n\n");
  }

  private void emitGroupID(ArrayList<Record> groups, PrintStream os) {
    os.printf("public interface GroupID%n\t{%n");
    os.println("\tint GRP_INVALID = -1;");
    int i = 0;
    TreeSet<String> ids = new TreeSet<>();

    for (Record r : groups)
      ids.add(manglingValidID(getOptionName(r)));
    for (String id : ids)
      os.printf("\tint GRP_%s = %d;%n", id, i++);
    os.printf("\tint GRP_Last = %d;%n", i);
    os.println("}");
  }

  private void emitGroup(ArrayList<Record> groups, PrintStream os) {
    emitSourceFileHeaderComment("Group ID definitions for JlangTool driver", os);

    os.printf("package xcc;%n%n");
    os.println("// Emission for Group ID.");
    emitGroupID(groups, os);
  }

  private void emitGroup2(ArrayList<Record> groups, PrintStream os) {
    emitSourceFileHeaderComment("Group definitions for JlangTool driver", os);

    os.printf("package xcc;%n%n");
    os.println("import static xcc.GroupID.*;");
    os.println("// Emission for Groups.");
    os.println("public enum Group \n{");
    int i = 0;
    int e = groups.size();
    for (Record r : groups) {
      String name = "INVALID";
      if (r.getValueInit("Group") instanceof Init.DefInit) {
        Init.DefInit di = (Init.DefInit) r.getValueInit("Group");
        name = getOptionName(di.getDef());
      }

      String helpText = null;
      if (!(r.getValueInit("HelpText") instanceof Init.UnsetInit)) {
        helpText = escapeString(r.getValueAsString("HelpText"));
      }

      os.printf("\tGROUP_%s(\"%s\", %s, %s, ",
          manglingValidID(r.getValueAsString("Name")),
          "GRP_" + manglingValidID(r.getValueAsString("Name")),
          "GRP_" + manglingValidID(getOptionName(r)),
          "GRP_" + manglingValidID(name));
      if (helpText == null)
        os.printf("null)");
      else
        os.printf("\"%s\")", helpText);
      if (i < e - 1)
        os.println(",");
      else
        os.println(";");
      ++i;
    }

    os.println();
    os.println("\tpublic String namespace;");
    os.println("\tpublic int id;");
    os.println("\tpublic int group;");
    os.println("\tpublic String helpText;");
    os.println("\tGroup(String namespace, int id, int group, String helpText)\n\t{");
    os.println("\t\tthis.namespace = namespace;");
    os.println("\t\tthis.id = id;");
    os.println("\t\tthis.group = group;");
    os.println("\t\tthis.helpText = helpText;");
    os.println("\t}");
    os.println("}");
  }

  public String getDirname(String path) {
    Util.assertion(path != null);
    int lastBlash = path.lastIndexOf('/');
    if (lastBlash < 0)
      lastBlash = path.length();
    return path.substring(0, lastBlash);
  }

  public String getBasename(String path) {
    Util.assertion(path != null);
    int lastBlash = path.lastIndexOf('/');
    Util.assertion(lastBlash >= 0, "No basename!");
    ++lastBlash;
    int lastDot = path.lastIndexOf('.');
    if (lastDot < 0)
      lastDot = path.length();
    Util.assertion(lastDot >= lastBlash, "Invalid OptionInfo filename!");
    return path.substring(lastBlash, lastDot);
  }

  private String computeOptionName(Record option) {
    String name = "OPTION_" + manglingValidID(option.getValueAsString("Name"));
    return name + option.getValueAsDef("Kind").getValueAsString("Name");
  }

  @Override
  public void run(String outputFile) throws FileNotFoundException {
    Util.assertion(outputFile != null && !outputFile.isEmpty(), "Invalid path to output file");


    ArrayList<Record> options = records.getAllDerivedDefinition("Option");
    ArrayList<Record> groups = records.getAllDerivedDefinition("OptionGroup");
    options.sort(OptionComparator);

    String dirname = getDirname(outputFile);
    String pathToOptionID = dirname + "/OptionID.java";

    try (PrintStream os = outputFile.equals("-") ?
        System.out : new PrintStream(new FileOutputStream(pathToOptionID))) {
      emitOptionIDs(options, os);
    }

    String pathToOptionKind = dirname + "/OptionKind.java";
    try (PrintStream os = outputFile.equals("-") ?
        System.out : new PrintStream(new FileOutputStream(pathToOptionKind))) {
      emitOptionKind(options, os);
    }

    String pathToGroupID = dirname + "/GroupID.java";
    try (PrintStream os = outputFile.equals("-") ?
        System.out : new PrintStream(new FileOutputStream(pathToGroupID))) {
      emitGroup(groups, os);
    }

    String pathToGroup = dirname + "/Group.java";
    try (PrintStream os = outputFile.equals("-") ?
        System.out : new PrintStream(new FileOutputStream(pathToGroup))) {
      emitGroup2(groups, os);
    }

    try (PrintStream os = outputFile.equals("-") ?
        System.out : new PrintStream(new FileOutputStream(outputFile))) {
      String className = getBasename(outputFile);
      emitSourceFileHeaderComment("Options definitions for JlangTool driver", os);

      os.printf("package xcc;%n%n");
      os.println("import static xcc.OptionKind.*;");
      os.println("import static xcc.OptionID.*;");
      os.println("import static xcc.GroupID.*;");
      os.println("import static xcc.JlangFlags.*;");

      os.printf("public enum %s\n", className);
      os.printf("{%n");

      os.println("\t// Emission for Options.\n");
      for (int i = 0, e = options.size(); i < e; i++) {
        Record opt = options.get(i);
        String groupName = "GRP_INVALID";

        if (opt.getValueInit("Group") instanceof Init.DefInit) {
          Init.DefInit di = (Init.DefInit) opt.getValueInit("Group");
          groupName = "GRP_" + manglingValidID(getOptionName(di.getDef()));
        }

        String aliasName = "OPT_INVALID";
        if (opt.getValueInit("Alias") instanceof Init.DefInit) {
          Init.DefInit li = (Init.DefInit) opt.getValueInit("Alias");
          aliasName = "OPT_" + manglingValidID(li.getDef().getValueAsString("Name"));
        }
        // OptionInfo flags.
        StringBuilder flags = new StringBuilder();
        Init.ListInit li = opt.getValueAsListInit("Flags");
        if (li.getSize() > 0) {
          for (int j = 0, sz = li.getSize(); j < sz; j++) {
            if (j != 0)
              flags.append("|");
            flags.append(((Init.DefInit) li.getElement(j)).getDef().getName());
          }
        } else
          flags.append('0');

        // Help information.
        String helpText = null;
        if (!(opt.getValueInit("HelpText") instanceof Init.UnsetInit)) {
          helpText = "        " + escapeString(opt.getValueAsString("HelpText"));
        }

        // Metavarname.
        String metaVarName = null;
        if (!(opt.getValueInit("MetaVarName") instanceof Init.UnsetInit)) {
          metaVarName = opt.getValueAsString("MetaVarName");
        }

        os.printf("\t%s(\"%s\", %s, %s, %s, %s, %s, %d",
            computeOptionName(opt),
            opt.getValueAsString("Name"),
            "OPT_" + manglingValidID(opt.getValueAsString("Name")),
            "KIND_" + opt.getValueAsDef("Kind").getValueAsString("Name"),
            groupName,
            aliasName,
            flags.toString(),
            opt.getValueAsInt("NumArgs"));
        if (helpText == null)
          os.print(", null");
        else
          os.printf(", \"%s\"", helpText);
        if (metaVarName == null)
          os.print(", null)");
        else
          os.printf(", \"%s\")", metaVarName);

        if (i < e - 1)
          os.println(",");
        else
          os.println(";");
      }

      os.println("\tpublic String optionName;");
      os.println("\tpublic int id;");
      os.println("\tpublic int kind;");
      os.println("\tpublic int group;");
      os.println("\tpublic int alias;");
      os.println("\tpublic int flags;");
      os.println("\tpublic int param;");
      os.println("\tpublic String helpText;");
      os.println("\tpublic String metaVarName;");
      os.println("\t");

      // Emission for constructor.
      os.println("\t// Constructor");
      os.printf("\tOptionInfo(String namespace, int optID, int kindID, %n\t\t\tint groupID, int aliasID, int flags, int param, %n\t\t\tString helpMsg, String metaVarName)%n\t{");
      os.println("\t\t\toptionName = namespace;");
      os.println("\t\t\tid = optID;");
      os.println("\t\t\tkind = kindID;");
      os.println("\t\t\tgroup = groupID;");
      os.println("\t\t\talias = aliasID;");
      os.println("\t\t\tthis.flags = flags;");
      os.println("\t\t\tthis.param = param;");
      os.println("\t\t\thelpText = helpMsg;");
      os.println("\t\t\tthis.metaVarName = metaVarName;");
      os.println("\t}");
      os.println("}");
    }
  }
}
