package jlang.system;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous Zeng.
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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class Process
{
    public static int getStandardErrColumns()
    {
        String os = System.getProperty("os.asmName").toLowerCase();
        switch (os)
        {
            case "linux":
            {
                String val = System.getenv("COLUMNS");
                if (val != null)
                    return Integer.parseInt(val);
                return 143;
            }
            default:return 80;
        }
    }

    public static boolean isDigit(char ch)
    {
        return ch >= '0' && ch <= '9';
    }

    public static String getHostTriple()
    {
        String hostTripleString = "i686-linux-gnu";
        String[] archSplit = hostTripleString.split("-");
        String arch = archSplit[0];

        String triple = arch;
        triple += "-";
        triple += archSplit[1];
        char[] temp = triple.toCharArray();

        if (temp[0] == 'i' && isDigit(temp[1]) && temp[2] == '8' && temp[3] == '6')
                temp[1] = '3';
        triple = String.valueOf(temp);

        return triple;
    }

    public static Boolean getStandardErrHasColors()
    {
        return false;
    }
}
