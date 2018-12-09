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

package backend.support;

import config.Config;
import tools.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This file defines a class used for writing a specific kind graph
 * (like CFG, CallGraph, DomTree, Dominance Frontier) into dot file.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class GraphWriter {
  public static PrintStream writeGraph(PrintStream out,
                                       DefaultDotGraphTrait dotTrait) {
    return writeGraph(out, dotTrait, false);
  }

  public static PrintStream writeGraph(PrintStream out,
                                       DefaultDotGraphTrait dotTrait,
                                       boolean shortName) {
    return writeGraph(out, dotTrait, shortName, "");
  }

  public static PrintStream writeGraph(PrintStream out,
                                       DefaultDotGraphTrait dotTrait,
                                       boolean shortName,
                                       String title) {
    GraphWriter writer = new GraphWriter(out, dotTrait, shortName);
    writer.writeHeader(title);
    writer.writeNodes();
    dotTrait.addCustomGraphFeatures();
    writer.writeFooter();
    return out;
  }

  private PrintStream out;
  private DefaultDotGraphTrait dotTrait;
  boolean shortName;

  public GraphWriter(PrintStream out, DefaultDotGraphTrait dotTrait,
                     boolean shortName) {
    this.out = out;
    this.dotTrait = dotTrait;
    this.shortName = shortName;
  }

  public PrintStream getOut() {
    return out;
  }

  public void writeHeader(String name) {
    String graphName = dotTrait.getGraphName();
    if (graphName != null && !graphName.isEmpty()) {
      out.printf("digraph \"%s\" { %n", Util.escapeString(graphName));
    } else if (!graphName.isEmpty()) {
      out.printf("digraph \"%s\" {%n", Util.escapeString(graphName));
    } else {
      out.printf("digraph unamed {%n");
    }

    if (dotTrait.renderGraphFromBottomUp())
      out.println("\trankdir=\"BT\";");

    if (!name.isEmpty()) {
      out.printf("\tlabel=\"%s\";%n", Util.escapeString(name));
    } else if (!graphName.isEmpty()) {
      out.printf("\tlabel=\"%s\";%n", Util.escapeString(graphName));
    }
    out.printf(dotTrait.getGraphProperties(dotTrait.getGraphType()));
    out.println();
  }

  public void writeNodes() {
    dotTrait.writeNodes(this);
  }

  public void writeFooter() {
    dotTrait.writeFooter(this);
  }

  public static void viewGraph(String title, String filename, DefaultDotGraphTrait dotTrait) {
    PrintStream out = null;
    File temp = null;
    try {
      Path path = Files.createTempFile(null, filename);
      temp = path.toFile();
      out = new PrintStream(temp);
      System.err.printf("Writing '%s'...%n", temp.toString());
      writeGraph(out, dotTrait, false, title);
      displayGraph(temp);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    } finally {
      if (out != null) out.close();
      if (temp != null) temp.delete();
    }
  }

  public static void displayGraph(File filename)
      throws IOException, InterruptedException {
    if (!Config.XDOT_PATH.isEmpty()) {
      System.err.println("Running 'xdot' program... ");
      Process p = Runtime.getRuntime().exec("xdot " + filename.toString());
      int res = p.waitFor();
      if (res != 0) {
        System.err.printf("Error viewing graph %s.\n", filename.getName());
      } else {
        filename.delete();
      }
    } else if (!Config.DOT_PATH.isEmpty()) {
      StringBuilder cmd = new StringBuilder();
      String pdfFilename = filename + ".pdf";
      cmd.append("dot");
      cmd.append(" -Tpdf");
      cmd.append(" -Nfontname=Courier");
      cmd.append(" -Gsize=7.5,10");
      cmd.append(" ").append(filename);
      cmd.append(" -o");
      cmd.append(" ").append(pdfFilename);
      System.err.println("Running 'dot' program... ");
      Process p = Runtime.getRuntime().exec(cmd.toString());
      int res = p.waitFor();
      if (res != 0)
        System.err.printf("Error viewing graph %s.\n", filename.getName());
      else {
        System.err.println("Running 'open' program... ");
        p = Runtime.getRuntime().exec("open " + pdfFilename);
        res = p.waitFor();
        if (res != 0)
          System.err.printf("Error viewing graph %s.\n", pdfFilename);
        else {
          filename.delete();
          Files.delete(Paths.get(pdfFilename));
        }
      }
    }
  }
}
