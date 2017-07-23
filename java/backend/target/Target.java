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

import backend.codegen.AsmPrinter;
import jlang.system.Triple;
import jlang.system.Triple.ArchType;

import java.io.OutputStream;

/**
 * Wrapper for Target specific information.
 * <p>
 * For registration purposes, this is a POD type so that targets can be
 * registered without the use of static constructors.
 * </p>
 * <p>>
 * Targets should implement a single global instance of this class (which
 * will be zero initialized), and pass that instance to the TargetRegistry as
 * part of their initialization.
 * </p
 * @author Xlous.zeng
 * @version 0.1
 */
public class Target
{
    public interface AsmInfoCtor
    {
        TargetAsmInfo create(Target t, String triple);
    }

    public interface TargetMachineCtor
    {
        TargetMachine create(Target t, String triple, String features);
    }

    public interface AsmPrinterCtor
    {
        AsmPrinter create(OutputStream os,
                TargetMachine tm,
                TargetAsmInfo asmInfo,
                boolean verbose);
    }

    private Target next;

    public static Target firstTarget;

    private TripleMatcher tripleMatchQualityFn;

    private String name;

    /**
     * A short desciption.
     */
    private String shortDesc;

    private AsmInfoCtor asmInfoCtor;

    /**
     * Construction function for this target's TargetMachine, if registered.
     */
    private TargetMachineCtor targetMachineCtor;
    /**
     * Construction function for this target's Assembly Printer, if registered.
     */
    private AsmPrinterCtor asmPrinterCtor;

    /**
     * Create a TargetAsmInfo implementation for the specified
     * target triple.
     *
     * @param triple  This argument is used to determine the target machine
     * feature set; it should always be provided. Generally this should be
     * either the target triple from the module, or the target triple of the
     * host if that does not exist.
     * @return
     */
    public TargetAsmInfo createAsmInfo(String triple)
    {
        if (asmInfoCtor == null)
            return null;
        return asmInfoCtor.create(this, triple);
    }

    /**
     * Create a target specific machine implementation for the specified {@code triple}
     * @param triple    This argument is used to determine the target machine
     * feature set; it should always be provided. Generally this should be
     * either the target triple from the module, or the target triple of the
     * host if that does not exist.
     * @param features
     * @return
     */
    public TargetMachine createTargetMachine(String triple, String features)
    {
        if (targetMachineCtor == null)
            return null;
        return targetMachineCtor.create(this, triple, features);
    }

    /**
     * Create a target specific assembly printer pass.
     * @param os
     * @param tm
     * @param asmInfo
     * @param verbose
     * @return
     */
    public AsmPrinter createAsmPrinter(OutputStream os,
            TargetMachine tm,
            TargetAsmInfo asmInfo,
            boolean verbose)
    {
        if (asmPrinterCtor == null)
            return null;
        return asmPrinterCtor.create(os, tm, asmInfo, verbose);
    }

    /**
     * A interface to define what is the best quality matcher to select a best
     * suitable target for specified triple string.
     */
    public interface TripleMatcher
    {
        int apply(String triple);
    }

    /**
     * A target registry factory.
     * @author Xlous.zeng
     * @version 0.1
     */
    public static class TargetRegistry
    {
        public static void registerTargetMachine(Target target, TargetMachineCtor ctor)
        {
            if (target.targetMachineCtor != null)
                return;
            target.targetMachineCtor = ctor;
        }

        public static void registerAsmInfo(Target target, AsmInfoCtor ctor)
        {
            if (target.asmInfoCtor != null)
                return;
            target.asmInfoCtor = ctor;
        }

        public static void registerAsmPrinter(Target target, AsmPrinterCtor ctor)
        {
            if (target.asmPrinterCtor != null)
                return;
            target.asmPrinterCtor = ctor;
        }

        private static void registerTarget(Target t,
                String name,
                String shortDesc,
                TripleMatcher matcher,
                boolean hasJIT)
        {
            assert name != null && !name.isEmpty() && matcher != null:
                    "Missing required target information!";

            // Check if this target has already been initialized, we allow this as a
            // convenience to some clients.
            if (t.name != null)
                return;

            t.next = firstTarget;
            firstTarget = t;
            t.name = name;
            t.shortDesc = shortDesc;
            t.tripleMatchQualityFn = matcher;
        }

        public static void registerTarget(Target t,
                String name,
                String shortDesc,
                ArchType archType,
                boolean hasJIT)
        {
            registerTarget(t, name, shortDesc, new GetTripleMatchQuality(archType), hasJIT);
        }

        public static class GetTripleMatchQuality implements TripleMatcher
        {
            private ArchType archType;

            public GetTripleMatchQuality(ArchType ty)
            {
                archType = ty;
            }

            @Override
            public int apply(String tt)
            {
                if (new Triple(tt).getArch() == archType)
                    return 20;
                return 0;
            }
        }
    }
}
