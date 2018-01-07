package backend.target;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

/**
 * Manages the enabling and disabling of subtarget 
 * specific features.  Features are encoded as a string of the form
 * <pre>
 *   "cpu,+attr1,+attr2,-attr3,...,+attrN"
 * </pre>
 * A comma separates each feature from the next (all lowercase.)
 * The first feature is always the CPU subtype (eg. pentiumm).  If the CPU
 * value is "generic" then the CPU subtype should be generic for the target.
 * Each of the remaining features is prefixed with + or - indicating whether
 * that feature should be enabled or disabled contrary to the cpu
 * specification.
 * @author Xlous.zeng
 * @version 0.1
 */
public class SubtargetFeatures
{
    ArrayList<String> features;    // Subtarget features as a vector

    public SubtargetFeatures()
    {
        this("");
    }
    public SubtargetFeatures(String initial)
    {
        features = new ArrayList<>();
        // Break up string into separate features
        split(features, initial);
    }

    /**
     * Splits a string of comma separated items in to a vector of strings.
     * @param features
     * @param init
     */
    private static void split(ArrayList<String> features, String init)
    {
        if (init == null || init.isEmpty())
            return;

        int pos = 0;
        while (true)
        {
            int comma = init.indexOf(',', pos);
            if (comma < 0)
            {
                features.add(init.substring(pos));
                break;
            }
            features.add(init.substring(pos, comma));
            pos = comma + 1;
        }
    }

    /**
     * Join a vector of strings to a string with a comma separating each element.
     * @param features
     * @return
     */
    private static String join(ArrayList<String> features)
    {
        StringBuilder buf = new StringBuilder();
        if (!features.isEmpty())
        {
            buf.append(features.get(0));
            for (int i = 1, e = features.size(); i < e; i++)
            {
                buf.append(",");
                buf.append(features.get(i));
            }
        }
        return buf.toString();
    }

    /** features string accessors.
     *
     * @return
     */
    public String getString()
    {
        return join(features);
    }

    public void setString(String initial)
    {
        features.clear();
        split(features, initial);
    }

    /** Set the CPU string.  Replaces previous setting.  Setting to "" clears CPU.
     *
     * @param str
     */
    public void setCPU(String str)
    {
        if (str == null || str.isEmpty())
            return;
        if (features.isEmpty())
            features.add(str.toLowerCase());
        else
            features.set(0, str.toLowerCase());
    }

    /** Setting CPU string only if no string is set.
     *
     * @param str
     */
    public void setCPUIfNone(String str)
    {
        if (features.get(0).isEmpty())
            features.set(0, str);
    }

    /** Returns current CPU string.
     *
     * @return
     */
    public String getCPU()
    {
        return features.get(0);
    }

    /** Adding features.
     *
     * @param str
     */
    public void addFeature(String str)
    {
        addFeature(str, true);
    }

    public void addFeature(String str, boolean isEnabled)
    {
        if (str != null && !str.isEmpty())
        {
            features.add(prependFlag(str.toLowerCase(), isEnabled));
        }
    }

    private static int getLongestEntryLength(SubtargetFeatureKV[] table)
    {
        int maxLen = 0;
        for (int i = 0; i < table.length; i++)
        {
            maxLen = Math.max(maxLen, table[i].key.length());
        }
        return maxLen;
    }

    private static void help(SubtargetFeatureKV[] cpuTable,
            SubtargetFeatureKV[] featureTable)
    {
        int maxCPULen = getLongestEntryLength(cpuTable);
        int maxFeatureLen = getLongestEntryLength(featureTable);

        // Print the CPU table.
        System.err.println("Available CPUs for this target:\n");
        for (SubtargetFeatureKV kv : cpuTable)
        {
            System.err.printf("  %s%s - %s.\n", kv.key,
                    Util.fixedLengthString(maxCPULen - kv.key.length(), ' '),
                    kv.desc);
        }
        System.err.println();

        // Print the Feature table.
        System.err.println("Available features for this target:\n");
        for (SubtargetFeatureKV kv : featureTable)
        {
            System.err.printf("  %s%s - %s.\n", kv.key,
                    Util.fixedLengthString(maxCPULen - kv.key.length(), ' '),
                    kv.desc);
        }
        System.err.println();
        System.err.println("Use +feature to enable a feature, or -feature to disable it.\n"
                + "For example, llc -mcpu=mycpu -mattr=+feature1,-feature2");
        System.exit(1);
    }

    /** Get feature bits.
     *
     * @param cpuTable
     * @param featureTable
     * @return
     */
    public int getBits(SubtargetFeatureKV[] cpuTable,
            SubtargetFeatureKV[] featureTable)
    {
        assert cpuTable != null :"missing CPU table";
        assert featureTable != null:"missing features table";

        int bit = 0;

        if (features.get(0).equals("help"))
        {
            help(cpuTable, featureTable);
        }

        SubtargetFeatureKV cpuEntry = null;
        for (SubtargetFeatureKV kv : cpuTable)
        {
            if (kv.key.equals(features.get(0)))
            {
                cpuEntry = kv;
                break;
            }
        }

        if (cpuEntry != null)
        {
            bit = cpuEntry.value;

            for (int i = 0; i < featureTable.length; i++)
            {
                SubtargetFeatureKV kv = featureTable[i];
                if ((cpuEntry.value & kv.value) != 0)
                    bit = setImpliedBits(bit, kv, featureTable);
            }
        }
        else
        {
            if (Util.DEBUG)
            {
                System.err.printf("'" + features.get(0)
                        + "' is not a recognized processor for this target"
                        + " (ignoring processor)"
                        + "\n");
            }
        }
        for (int i = 1; i < features.size(); i++)
        {
            String feature = features.get(i);

            if (feature.equals("help"))
                help(cpuTable, featureTable);

            SubtargetFeatureKV featureEntry = null;
            for (SubtargetFeatureKV kv : featureTable)
            {
                if (kv.key.equals(stripFlag(feature)))
                {
                    featureEntry = kv;
                    break;
                }
            }

            if(featureEntry != null)
            {
                if (isEnabled(feature))
                {
                    bit |= featureEntry.value;

                    bit = setImpliedBits(bit, featureEntry, featureTable);
                }
                else
                {
                    bit &= ~featureEntry.value;

                    bit = clearImpliedBits(bit, featureEntry, featureTable);
                }
            }
            else
            {
                if(Util.DEBUG)
                {
                    System.err.printf("'" + feature
                            + "' is not a recognized feature for this target"
                            + " (ignoring feature)"
                            + "\n");
                }
            }
        }
        return bit;
    }

    private static String stripFlag(String feature)
    {
        return hasFlag(feature) ? feature.substring(1) : feature;
    }

    /**
     * Return true if enable flag; '+'.
     * @param feature
     * @return
     */
    private static boolean isEnabled(String feature)
    {
        assert !feature.isEmpty():"Empty feature string";
        return feature.charAt(0) == '+';
    }

    private static int clearImpliedBits(int bits,
            SubtargetFeatureKV featureEntry,
            SubtargetFeatureKV[] featureTable)
    {
        for (SubtargetFeatureKV kv : featureTable)
        {
            if (featureEntry.value == kv.value)
                continue;
            if ((featureEntry.implies & kv.value) != 0)
            {
                bits &= ~kv.value;
                bits = clearImpliedBits(bits, kv, featureTable);
            }
        }
        return bits;
    }

    private static int setImpliedBits(int bits,
            SubtargetFeatureKV featureKV,
            SubtargetFeatureKV[] featureTable)
    {
        for (int i = 0; i < featureTable.length; i++)
        {
            SubtargetFeatureKV kv = featureTable[i];
            if (featureKV.value == kv.value)
                continue;
            if ((featureKV.implies & kv.value) != 0)
            {
                bits |= featureKV.value;
                bits = setImpliedBits(bits, kv, featureTable);
            }
        }
        return bits;
    }

    /** Get info pointer
     *
     * @param table
     */
    public Object getInfo(SubtargetInfoKV[] table)
    {
        SubtargetInfoKV entry = null;
        for (SubtargetInfoKV foundKV : table)
        {
            if (foundKV.key.equals(features.get(0)))
            {
                entry = foundKV;
                break;
            }
        }
        if (entry != null)
            return entry.value;
        else
        {
            if (Util.DEBUG)
            {
                System.err.printf("'%s' is not a recognized processor for this target"
                        + " (ignoring processor)\n", features.get(0));
            }
            return null;
        }
    }

    /** Print feature string.
     *
     * @param os
     */
    public void print(PrintStream os)
    {
        if (os == null)
            return;

        for (String feature : features)
            os.printf("%s ", feature);
        os.println();
    }

    // Dump feature info.
    public void dump()
    {
        print(System.err);
    }

    private static String prependFlag(String feature, boolean isEnabled)
    {
        if (hasFlag(feature))
            return feature;
        return (isEnabled ? "+": "-") + feature;
    }

    private static boolean hasFlag(String feature)
    {
        assert !feature.isEmpty() :"Empty string";

        char ch = feature.charAt(0);
        // Check if first character is '+' or '-' flag
        return ch == '+' || ch == '-';
    }
}
