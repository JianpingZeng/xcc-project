package jlang.sema;
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

import jlang.sema.Decl.NamedDecl;
import tools.Util;

import java.util.ArrayList;

import static jlang.sema.StoredDeclsList.DataKind.*;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class StoredDeclsList {
  /**
   * The kind of data encoded in this list.
   */
  interface DataKind {
    /**
     * The data is a {@linkplain NamedDecl}.
     */
    int DK_Decl = 1;
    /**
     * The data is a declaration ID (an integral value).
     */
    int DK_DeclID = 2;
    /**
     * The data is a reference to list that contains declarations.
     */
    int DK_Decl_Vector = 4;
    /**
     * The data is a reference to list that contains the declaration ID.
     */
    int DK_ID_Vector = 8;
  }

  private Object data;
  private int kind;

  public StoredDeclsList() {
    kind = 0;
    data = null;
  }

  public boolean isNull() {
    return data == null;
  }

  public NamedDecl getAsDecl() {
    if (kind != DK_Decl)
      return null;
    return (NamedDecl) data;
  }

  public ArrayList<Object> getAsList() {
    if (kind != DK_ID_Vector && kind != DK_Decl_Vector)
      return null;
    return (ArrayList<Object>) data;
  }

  public void setOnlyValue(NamedDecl nd) {
    Util.assertion(getAsList() == null, "Not inline!");
    data = nd;
    kind = DK_Decl;
  }

  public void setFromDeclIDs(ArrayList<Integer> list) {
    if (list.size() > 1) {
      ArrayList<Object> vector = getAsList();
      if (vector == null) {
        vector = new ArrayList<>();
        data = vector;
        kind = DK_ID_Vector;
      }
      vector.addAll(list);
      return;
    }

    if (list.isEmpty()) {
      data = null;
      kind = 0;
    } else {
      data = list;
      kind = DK_ID_Vector;
    }
  }

  public boolean hasDeclarationIDs() {
    return kind == DK_DeclID || kind == DK_ID_Vector;
  }

  public void materializeDecls(ASTContext context) {
  }

  public NamedDecl[] getLookupResult(ASTContext context) {
    if (isNull())
      return new NamedDecl[0];

    if (hasDeclarationIDs())
      materializeDecls(context);

    // If we have a single NamedDecl, return it.
    if (getAsDecl() != null) {
      Util.assertion(!isNull(), "Empty list is not allowed!");
      NamedDecl[] res = new NamedDecl[1];
      res[0] = getAsDecl();
      return res;
    }

    Util.assertion(getAsList() != null, "Must have a vector at this point");
    ArrayList<Object> list = getAsList();
    NamedDecl[] res = new NamedDecl[list.size()];
    for (int i = 0; i < res.length; ++i)
      res[i] = (NamedDecl) list.get(i);
    return res;
  }

  /**
   * If this is a redeclaration of an existing decl,
   * replace the old one with D and return true. Otherwise return false.
   *
   * @param context
   * @param nd
   * @return
   */
  public boolean handleRedeclaration(ASTContext context, NamedDecl nd) {
    if (hasDeclarationIDs())
      materializeDecls(context);

    NamedDecl oldDecl = getAsDecl();
    if (oldDecl != null) {
      if (!nd.declarationReplaces(oldDecl))
        return false;
      setOnlyValue(nd);
      return true;
    }

    ArrayList<Object> list = getAsList();
    for (int i = 0, e = list.size(); i < e; ++i) {
      Object obj = list.get(i);
      NamedDecl oldD = (NamedDecl) obj;
      if (nd.declarationReplaces(oldD)) {
        list.set(i, nd);
        return true;
      }
    }
    return false;
  }

  /**
   * This is called on the second and later decl when it is
   * not a redeclaration to merge it into the appropriate place in our list.
   *
   * @param d
   */
  public void addSubsequentialDecl(NamedDecl d) {
    Util.assertion(!hasDeclarationIDs(), "Must materialize before adding decl!");

    // If this is the second decl added to the list, convert this to vector
    // form.
    NamedDecl oldDecl = getAsDecl();
    if (oldDecl != null) {
      ArrayList<Object> list = new ArrayList<>();
      list.add(oldDecl);
      data = list;
      kind = DK_Decl_Vector;
    }

    // At this point, this is the first declaration of the same name.
    ArrayList<Object> list = getAsList();
    if (d.getIdentifierNamespace() == IdentifierNamespace.IDNS_Tag)
      list.add(d);
    else if (((NamedDecl) list.get(list.size() - 1)).getIdentifierNamespace() == IdentifierNamespace.IDNS_Tag) {
      list.add(list.size() - 1, d);
    } else {
      list.add(d);
    }
  }
}
