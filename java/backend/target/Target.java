package backend.target;
/*
 * Extremely C language Compiler
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

import backend.codegen.AsmPrinter;
import backend.codegen.AsmWriterFlavorTy;
import backend.mc.MCAsmInfo;
import backend.mc.MCInstPrinter;
import backend.mc.MCStreamer;
import backend.mc.MCSymbol;
import backend.support.Triple;
import backend.support.Triple.ArchType;
import tools.OutRef;
import tools.Util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;

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
 *
 * @author Jianping Zeng
 * @version 0.1
 */
public class Target {
  public interface AsmInfoCtor {
    MCAsmInfo create(Target t, String triple);
  }

  public interface TargetMachineCtor {
    TargetMachine create(Target t, String triple, String features);
  }

  public interface AsmPrinterCtor {
    AsmPrinter create(PrintStream os,
                      TargetMachine tm,
                      MCSymbol.MCContext ctx,
                      MCStreamer streamer,
                      MCAsmInfo mai);
  }

  public interface MCInstPrinterCtor {
    MCInstPrinter create(AsmWriterFlavorTy asmflavor,
                         PrintStream os,
                         MCAsmInfo mai);
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

  private MCInstPrinterCtor mcInstPrinterCtor;
  /**
   * create a MCAsmInfo implementation for the specified
   * target triple.
   *
   * @param triple This argument is used to determine the target machine
   *               feature set; it should always be provided. Generally this should be
   *               either the target triple from the module, or the target triple of the
   *               host if that does not exist.
   * @return
   */
  public MCAsmInfo createAsmInfo(String triple) {
    if (asmInfoCtor == null)
      return null;
    return asmInfoCtor.create(this, triple);
  }

  /**
   * create a target specific machine implementation for the specified {@code triple}
   *
   * @param triple   This argument is used to determine the target machine
   *                 feature set; it should always be provided. Generally this should be
   *                 either the target triple from the module, or the target triple of the
   *                 host if that does not exist.
   * @param features
   * @return
   */
  public TargetMachine createTargetMachine(String triple, String features) {
    if (targetMachineCtor == null)
      return null;
    return targetMachineCtor.create(this, triple, features);
  }

  /**
   * create a target specific assembly printer pass.
   *
   * @param os
   * @param tm
   * @param mai
   * @return
   */
  public AsmPrinter createAsmPrinter(PrintStream os,
                                     TargetMachine tm,
                                     MCSymbol.MCContext ctx,
                                     MCStreamer streamer,
                                     MCAsmInfo mai) {
    if (asmPrinterCtor == null)
      return null;
    return asmPrinterCtor.create(os, tm, ctx, streamer, mai);
  }

  public MCInstPrinter createMCInstPrinter(AsmWriterFlavorTy asmflavor,
                                           PrintStream os,
                                           MCAsmInfo mai) {
    if (mcInstPrinterCtor == null)
      return null;
    return mcInstPrinterCtor.create(asmflavor, os, mai);
  }

  public String getName() {
    return name;
  }

  public String getShortDescription() {
    return shortDesc;
  }

  /**
   * A interface to define what is the best quality matcher to select a best
   * suitable target for specified triple string.
   */
  public interface TripleMatcher {
    int apply(String triple);
  }

  /**
   * A target registry factory.
   *
   * @author Jianping Zeng
   * @version 0.1
   */
  public static class TargetRegistry {
    /**
     * A refernce to current walked target.
     * This used to walk through the target registered chain.
     */
    private static Target currentTarget = null;

    public static void registerTargetMachine(Target target, TargetMachineCtor ctor) {
      if (target.targetMachineCtor != null)
        return;
      target.targetMachineCtor = ctor;
    }

    public static void registerAsmInfo(Target target, AsmInfoCtor ctor) {
      if (target.asmInfoCtor != null)
        return;
      target.asmInfoCtor = ctor;
    }

    public static void registerAsmPrinter(Target target, AsmPrinterCtor ctor) {
      if (target.asmPrinterCtor != null)
        return;
      target.asmPrinterCtor = ctor;
    }

    public static void registerMCInstPrinter(Target target, MCInstPrinterCtor ctor) {
      if (target.mcInstPrinterCtor != null)
        return;
      target.mcInstPrinterCtor = ctor;
    }

    private static void registerTarget(Target t,
                                       String name,
                                       String shortDesc,
                                       TripleMatcher matcher,
                                       boolean hasJIT) {
      Util.assertion(name != null && !name.isEmpty() && matcher != null, "Missing required target information!");


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
                                      boolean hasJIT) {
      registerTarget(t, name, shortDesc, new GetTripleMatchQuality(archType), hasJIT);
    }

    public static Target lookupTarget(String triple,
                                      OutRef<String> error) {
      if (firstTarget == null) {
        error.set("Unable to find target for this triple (no targets are registered)");
        return null;
      }

      Target best = null, equallyBest = null;
      int bestQuality = 0;
      for (Target itr = firstTarget; itr != null; itr = itr.next) {
        int qual = itr.tripleMatchQualityFn.apply(triple);
        if (best == null || qual > bestQuality) {
          best = itr;
          equallyBest = null;
          bestQuality = qual;
        } else if (qual == bestQuality) {
          equallyBest = itr;
        }
      }

      if (best == null) {
        error.set("No available targets are compatible with this triple");
        return null;
      }

      if (equallyBest != null) {
        error.set("Cannot choose between targets \"" +
            best.name + "\" and \"" + equallyBest.name + "\"");
        return null;
      }
      return best;
    }

    public static Iterator<Target> iterator() {
      return new TargetIterator(firstTarget);
    }

    /**
     * A Iterator to walk through all of registered target.
     */
    public static class TargetIterator implements Iterator<Target> {
      private Target cur;

      public TargetIterator(Target t) {
        cur = t;
        Util.assertion(cur != null);
      }

      @Override
      public boolean hasNext() {
        return cur != null;
      }

      @Override
      public Target next() {
        Util.assertion(hasNext());
        Target temp = cur;
        cur = cur.next;
        return temp;
      }
    }

    public static class GetTripleMatchQuality implements TripleMatcher {
      private ArchType archType;

      public GetTripleMatchQuality(ArchType ty) {
        archType = ty;
      }

      @Override
      public int apply(String tt) {
        if (new Triple(tt).getArch() == archType)
          return 20;
        return 0;
      }
    }
  }
}
