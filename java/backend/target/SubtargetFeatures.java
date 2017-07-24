package backend.target;
/*
 * Xlous C language Compiler
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

import java.io.OutputStream;
import java.util.ArrayList;

/**
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
        split(features, initial);
    }

    /// features string accessors.
    public String getString()
    {
        return join(features);
    }

    public void setString(String Initial)
    {
    }

    /// Set the CPU string.  Replaces previous setting.  Setting to "" clears CPU.
    public void setCPU(String String)
    {
    }

    /// Setting CPU string only if no string is set.
    public void setCPUIfNone(String String)
    {
    }

    /// Returns current CPU string.
    public String getCPU()
    {
    }

    /// Adding features.
    public void AddFeature(String String)
    {
        AddFeature(String, true);
    }

    public void AddFeature(String String, boolean IsEnabled)
    {

    }

    /// Get feature bits.
    public int getBits(SubtargetFeatureKV[] CPUTable,
            SubtargetFeatureKV[] FeatureTable)
    {
    }

    /// Get info pointer
    public getInfo(SubtargetInfoKV Table, int TableSize)
    {
    }

    /// Print feature string.
    public void print(OutputStream os)
    {
        if (os == null)
            return;

    }

    // Dump feature info.
    public void dump()
    {
        print(System.err);
    }

    public void addFeature(String string)
    {
        addFeature(string, true);
    }

    public void addFeature(String string, boolean isEnabled)
    {
        if (!string.isEmpty())
            features.add(prependFlag(string.toLowerCase(), isEnabled));
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
