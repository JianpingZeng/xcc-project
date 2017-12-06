/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import java.io.PrintStream;

/**
 * This file defines a class used for writing a specific kind graph
 * (like CFG, CallGraph, DomTree, Dominance Frontier) into dot file.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class GraphWriter
{
    public static PrintStream writeGraph(PrintStream out,
            DefaultDotGraphTrait dotTrait)
    {
        return writeGraph(out, dotTrait,false);
    }

    public static PrintStream writeGraph(PrintStream out,
            DefaultDotGraphTrait dotTrait,
            boolean shortName)
    {
        return writeGraph(out, dotTrait, shortName, "");
    }

    public static PrintStream writeGraph(PrintStream out,
            DefaultDotGraphTrait dotTrait,
            boolean shortName,
            String name)
    {
        return writeGraph(out, dotTrait, shortName, name, "");
    }

    public static PrintStream writeGraph(PrintStream out,
            DefaultDotGraphTrait dotTrait,
            boolean shortName,
            String name,
            String title)
    {
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
            boolean shortName)
    {
        this.out = out;
        this.dotTrait = dotTrait;
        this.shortName = shortName;
    }

    public PrintStream getOut()
    {
        return out;
    }

    static String escapeString(String str)
    {
        StringBuilder buf = new StringBuilder();
        buf.append(str);
        for (int i = 0; i < buf.length(); i++)
        {
            switch (buf.charAt(i))
            {
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
                case '\\':
                    if (i+1 != buf.length())
                    {
                        switch (buf.charAt(i+1))
                        {
                            case '1': continue;
                            case '|':
                            case '{':
                            case '}':
                                buf.deleteCharAt(i);
                                continue;
                            default:
                                break;
                        }
                    }
                case '{':
                case '}':
                case '<':
                case '>':
                case '|':
                case '"':
                    buf.insert(i, '\\');
                    ++i;
                    break;
            }
        }
        return buf.toString();
    }

    public void writeHeader(String name)
    {
        String graphName = dotTrait.getGraphName();
        if (graphName != null && !graphName.isEmpty())
        {
            out.printf("digraph \"%s\" { %n", escapeString(graphName));
        }
        else if (!graphName.isEmpty())
        {
            out.printf("digraph \"%s\" {%n", escapeString(graphName));
        }
        else
        {
            out.printf("digraph unamed {%n");
        }

        if (dotTrait.renderGraphFromBottomUp())
            out.println("\trankdir=\"BIT\";");

        if (!name.isEmpty())
        {
            out.printf("\tlabel=\"%s\";%n", escapeString(name));
        }
        else if (!graphName.isEmpty())
        {
            out.printf("\tlabel=\"%s\";%n", escapeString(graphName));
        }
        out.printf(dotTrait.getGraphProperties());
        out.println();
    }

    public void writeNodes()
    {
        dotTrait.writeNodes(this);
    }

    public void writeFooter()
    {
        dotTrait.writeFooter(this);
    }
}
