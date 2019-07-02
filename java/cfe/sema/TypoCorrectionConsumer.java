/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

package cfe.sema;

import cfe.clex.IdentifierInfo;
import cfe.sema.Decl.NamedDecl;
import tools.Util;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class TypoCorrectionConsumer {
  private class Record implements Comparable<Record> {
    int edit;
    NamedDecl decl;

    public Record(int d, NamedDecl nd) {
      edit = d;
      decl = nd;
    }

    @Override
    public int compareTo(Record o) {
      return edit - o.edit;
    }
  }

  private String typo;
  private TreeSet<Record> bestResults;
  private int bestEditDistance;


  public TypoCorrectionConsumer(IdentifierInfo typo) {
    this.typo = typo.getName();
    bestResults = new TreeSet<>();
    bestEditDistance = 0;
  }

  public void foundDecl(NamedDecl nd) {
    IdentifierInfo name = nd.getIdentifier();
    if (name == null)
      return;

    int ed = Util.getEditDistance(typo, name.getName());
    if (bestResults.isEmpty()) {
      bestResults.add(new Record(ed, nd));
      bestEditDistance = ed;
    } else {
      if (ed < bestEditDistance) {
        bestEditDistance = ed;
        bestResults.add(new Record(ed, nd));
      }
    }
  }

  public ArrayList<NamedDecl> getBestResults() {
    ArrayList<NamedDecl> res = new ArrayList<>();
    res.addAll(bestResults.stream()
        .map(node -> node.decl)
        .collect(Collectors.toList()));
    return res;
  }

  public int getBestEditDistance() {
    return bestEditDistance;
  }

  public boolean isEmpty() {
    return bestResults.isEmpty();
  }
}
