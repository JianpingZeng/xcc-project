package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the namespace of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import tools.Util;
import utils.tablegen.PatternToMatch.PatternSortingPredicate;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static utils.tablegen.DAGISelMatcherEmitter.emitMatcherTable;
import static utils.tablegen.DAGISelMatcherGen.convertPatternToMatcher;

/**
 * This is class definition used to generate a DFA-based instruction selector
 * which takes responsible for matching the given target-independent DAG node
 * with specified target-specific DAG node according to some metrics, like
 * matching cost, code size and enabled optimization level.
 *
 * @author Jianping Zeng.
 * @version 0.4
 */
public final class DAGISelEmitter extends TableGenBackend {
  private CodeGenDAGPatterns cgp;

  public DAGISelEmitter(RecordKeeper keeper) {
    cgp = new CodeGenDAGPatterns(keeper);
  }

  @Override
  public void run(String outputFile) throws FileNotFoundException {
    Util.assertion(outputFile != null && !outputFile.isEmpty());
    try (PrintStream os = !outputFile.equals("-") ?
        new PrintStream(new FileOutputStream(outputFile)) : System.out) {
      CodeGenTarget target = cgp.getTarget();
      String targetName = target.getName();

      emitSourceFileHeaderComment("Instruction Selection based on DAG " +
          "Covering for " + targetName + ".", os);

      // Add all the patterns to a temporary list so we can sort them.
      ArrayList<PatternToMatch> patterns = new ArrayList<>(cgp.getPatternsToMatch());

      // We want to process the matches in order of minimal cost.  Sort the patterns
      // so the least cost one is at the start.
      patterns.sort(new PatternSortingPredicate(cgp));

      if (Util.DEBUG) {
        patterns.forEach(tp ->
        {
          tp.dump();
          System.err.println();
        });
      }

      // Convert the each variant of each pattern into a Matcher.
      ArrayList<Matcher> patternMatchers = new ArrayList<>();
      for (PatternToMatch tp : patterns) {
        for (int variant = 0; ; ++variant) {
          Matcher m = convertPatternToMatcher(tp, variant, cgp);
          if (m != null)
            patternMatchers.add(m);
          else
            break;
        }
      }

      Matcher theMatcher = new Matcher.ScopeMatcher(patternMatchers);
      emitMatcherTable(theMatcher, cgp, os);
    }
  }
}
