package cfe.sema;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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

import cfe.support.Linkage;
import cfe.support.Visibility;
import tools.Pair;

import static cfe.support.Linkage.*;
import static cfe.support.Visibility.DefaultVisibility;
import static cfe.support.Visibility.minVisibility;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class LinkageInfo {
  private Linkage linkage;
  private Visibility visibility;
  private boolean explicit;

  public LinkageInfo() {
    linkage = Linkage.ExternalLinkage;
    visibility = DefaultVisibility;
    explicit = false;
  }

  public LinkageInfo(Linkage l, Visibility v, boolean e) {
    linkage = l;
    visibility = v;
    explicit = e;
  }

  public static LinkageInfo external() {
    return new LinkageInfo();
  }

  public static LinkageInfo internal() {
    return new LinkageInfo(InternalLinkage, DefaultVisibility, false);
  }

  public static LinkageInfo uniqueExternal() {
    return new LinkageInfo(UniqueExternalLinkage, DefaultVisibility, false);
  }

  public static LinkageInfo none() {
    return new LinkageInfo(NoLinkage, DefaultVisibility, false);
  }

  public Linkage getLinkage() {
    return linkage;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public boolean visibilityExplicit() {
    return explicit;
  }

  public void setLinkage(Linkage linkage) {
    this.linkage = linkage;
  }

  public void setVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  public void setVisibility(Visibility visibility, boolean e) {
    this.visibility = visibility;
    explicit = e;
  }

  public void setVisibility(LinkageInfo other) {
    setVisibility(other.visibility, other.explicit);
  }

  public void mergeLinkage(Linkage L) {
    setLinkage(minLinkage(getLinkage(), L));
  }

  public void mergeLinkage(LinkageInfo Other) {
    setLinkage(minLinkage(getLinkage(), Other.getLinkage()));
  }

  public void mergeVisibility(Visibility V) {
    setVisibility(minVisibility(getVisibility(), V));
  }

  public void mergeVisibility(Visibility V, boolean E) {
    setVisibility(minVisibility(getVisibility(), V), visibilityExplicit() || E);
  }

  public void mergeVisibility(LinkageInfo Other) {
    mergeVisibility(Other.getVisibility(), Other.visibilityExplicit());
  }

  public void merge(LinkageInfo Other) {
    mergeLinkage(Other);
    mergeVisibility(Other);
  }

  public void merge(Pair<Linkage, Visibility> LV) {
    mergeLinkage(LV.first);
    mergeVisibility(LV.second);
  }
}
