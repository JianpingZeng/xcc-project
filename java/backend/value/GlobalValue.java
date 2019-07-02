package backend.value;
/*
 * Extremely C language CompilerInstance
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

import backend.type.PointerType;
import backend.type.Type;
import tools.Util;

import static backend.value.GlobalValue.LinkageType.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class GlobalValue extends Constant {
  /**
   * An enumeration for the kinds of linkage for global variable and function.
   */
  public enum LinkageType {
    ExternalLinkage,///< Externally visible function
    AvailableExternallyLinkage, ///< Available for inspection, not emission.
    LinkOnceAnyLinkage, ///< Keep one copy of function when linking (inline)
    LinkOnceODRLinkage, ///< Same, but only replaced by something equivalent.
    WeakAnyLinkage,     ///< Keep one copy of named function when linking (weak)
    WeakODRLinkage,     ///< Same, but only replaced by something equivalent.
    AppendingLinkage,   ///< Special purpose, only applies to global arrays
    InternalLinkage,    ///< Rename collisions when linking (static functions).
    PrivateLinkage,     ///< Like Internal, but omit from symbol table.
    LinkerPrivateLinkage, ///< Like Private, but linker removes.
    LinkerPrivateWeakLinkage, ///< Like LinkerPrivate, but weak.
    LinkerPrivateWeakDefAutoLinkage, ///< Like LinkerPrivateWeak, but possibly
    ///  hidden.
    DLLImportLinkage,   ///< Function to be imported from DLL
    DLLExportLinkage,   ///< Function to be accessible from DLL.
    ExternalWeakLinkage,///< ExternalWeak linkage description.
    CommonLinkage,       ///< Tentative definitions.
  }

  protected Module parent;
  private LinkageType linkage;
  /**
   * The section on which this value will be printed.
   */
  private String section;

  private int alignment;

  /**
   * Constructs a new instruction representing the specified constant.
   *
   * @param
   */
  public GlobalValue(Type ty, int valueType, LinkageType linkage, String name) {
    super(ty, valueType);
    setName(name);
    this.linkage = linkage;
    section = "";
  }

  public boolean isDeclaration() {
    if (this instanceof GlobalVariable)
      return getNumOfOperands() == 0;

    if (this instanceof Function)
      return ((Function) this).empty();

    return false;
  }

  /**
   * This method unlinks 'this' from the containing module
   * and deletes it.
   */
  public abstract void eraseFromParent();

  public Module getParent() {
    return parent;
  }

  public void setParent(Module newParent) {
    parent = newParent;
  }

  @Override
  public boolean isNullValue() {
    return false;
  }

  /**
   * The type of all of global value musts be pointer.
   *
   * @return
   */
  @Override
  public PointerType getType() {
    return (PointerType) super.getType();
  }

  public boolean hasSection() {
    return section != null && !section.isEmpty();
  }

  public String getSection() {
    return section;
  }

  public void setSection(String newSection) {
    section = newSection;
  }

  public int getAlignment() {
    return alignment;
  }

  public void setAlignment(int align) {
    Util.assertion((align & (align - 1)) == 0, "Alignment must be power of 2!");
    alignment = align;
  }

  static private boolean doesConstantDead(Constant c) {
    if (c instanceof GlobalValue) return false;

    for (int i = 0; i < c.getNumUses(); i++) {
      User u = c.useAt(i).getUser();
      if (!(u instanceof Constant))
        return false;
      if (!doesConstantDead((Constant) u))
        return false;
      c.getUseList().remove(i);
    }
    return true;
  }

  /**
   * If there are any dead constant users dangling
   * off of this global value, remove them.  This method is useful for clients
   * that want to check to see if a global is unused, but don't want to deal
   * with potentially dead constants hanging off of the globals.
   */
  public void removeDeadConstantUsers() {
    for (int i = 0; i < getNumUses(); i++) {
      User u = useAt(i).getUser();
      if (!(u instanceof Constant))
        continue;

      Constant c = (Constant) u;
      if (doesConstantDead(c))
        usesList.remove(i);
    }
  }

  static LinkageType getLinkOnceLinkage(boolean ODR) {
    return ODR ? LinkOnceODRLinkage : LinkOnceAnyLinkage;
  }
  static LinkageType getWeakLinkage(boolean ODR) {
    return ODR ? WeakODRLinkage : WeakAnyLinkage;
  }

  static boolean isExternalLinkage(LinkageType Linkage) {
    return Linkage == ExternalLinkage;
  }
  static boolean isAvailableExternallyLinkage(LinkageType Linkage) {
    return Linkage == AvailableExternallyLinkage;
  }
  static boolean isLinkOnceLinkage(LinkageType Linkage) {
    return Linkage == LinkOnceAnyLinkage || Linkage == LinkOnceODRLinkage;
  }
  static boolean isWeakLinkage(LinkageType Linkage) {
    return Linkage == WeakAnyLinkage || Linkage == WeakODRLinkage;
  }
  static boolean isAppendingLinkage(LinkageType Linkage) {
    return Linkage == AppendingLinkage;
  }
  static boolean isInternalLinkage(LinkageType Linkage) {
    return Linkage == InternalLinkage;
  }
  static boolean isPrivateLinkage(LinkageType Linkage) {
    return Linkage == PrivateLinkage;
  }
  static boolean isLinkerPrivateLinkage(LinkageType Linkage) {
    return Linkage == LinkerPrivateLinkage;
  }
  static boolean isLinkerPrivateWeakLinkage(LinkageType Linkage) {
    return Linkage == LinkerPrivateWeakLinkage;
  }
  static boolean isLinkerPrivateWeakDefAutoLinkage(LinkageType Linkage) {
    return Linkage == LinkerPrivateWeakDefAutoLinkage;
  }
  static boolean isLocalLinkage(LinkageType Linkage) {
    return isInternalLinkage(Linkage) || isPrivateLinkage(Linkage) ||
        isLinkerPrivateLinkage(Linkage) || isLinkerPrivateWeakLinkage(Linkage) ||
        isLinkerPrivateWeakDefAutoLinkage(Linkage);
  }
  static boolean isDLLImportLinkage(LinkageType Linkage) {
    return Linkage == DLLImportLinkage;
  }
  static boolean isDLLExportLinkage(LinkageType Linkage) {
    return Linkage == DLLExportLinkage;
  }
  static boolean isExternalWeakLinkage(LinkageType Linkage) {
    return Linkage == ExternalWeakLinkage;
  }
  static boolean isCommonLinkage(LinkageType Linkage) {
    return Linkage == CommonLinkage;
  }
  
  private static boolean maybeOverridden(LinkageType linkage) {
    return linkage == WeakAnyLinkage ||
        linkage == LinkOnceAnyLinkage ||
        linkage == CommonLinkage ||
        linkage == ExternalWeakLinkage ||
        linkage == LinkerPrivateWeakLinkage ||
        linkage == LinkerPrivateWeakDefAutoLinkage;
  }

  private static boolean isWeakForLinker(LinkageType linkage) {
    return linkage == AvailableExternallyLinkage ||
        linkage == WeakAnyLinkage ||
        linkage == WeakODRLinkage ||
        linkage == LinkOnceAnyLinkage ||
        linkage == LinkOnceODRLinkage ||
        linkage == CommonLinkage ||
        linkage == ExternalWeakLinkage ||
        linkage == LinkerPrivateWeakLinkage ||
        linkage == LinkerPrivateWeakDefAutoLinkage;
  }

  public boolean hasExternalLinkage() { return isExternalLinkage(linkage); }
  public boolean hasAvailableExternallyLinkage() {
    return isAvailableExternallyLinkage(linkage);
  }
  public boolean hasLinkOnceLinkage() {
    return isLinkOnceLinkage(linkage);
  }
  public boolean hasWeakLinkage() {
    return isWeakLinkage(linkage);
  }
  public boolean hasAppendingLinkage() { return isAppendingLinkage(linkage); }
  public boolean hasInternalLinkage() { return isInternalLinkage(linkage); }
  public boolean hasPrivateLinkage() { return isPrivateLinkage(linkage); }
  public boolean hasLinkerPrivateLinkage() { return isLinkerPrivateLinkage(linkage); }
  public boolean hasLinkerPrivateWeakLinkage() {
    return isLinkerPrivateWeakLinkage(linkage);
  }
  public boolean hasLinkerPrivateWeakDefAutoLinkage() {
    return isLinkerPrivateWeakDefAutoLinkage(linkage);
  }
  public boolean hasLocalLinkage() { return isLocalLinkage(linkage); }
  public boolean hasDLLImportLinkage() { return isDLLImportLinkage(linkage); }
  public boolean hasDLLExportLinkage() { return isDLLExportLinkage(linkage); }
  public boolean hasExternalWeakLinkage() { return isExternalWeakLinkage(linkage); }
  public boolean hasCommonLinkage() { return isCommonLinkage(linkage); }

  public void setLinkage(LinkageType LT) { linkage = LT; }
  public LinkageType getLinkage() { return linkage; }


  public boolean isWeakForLinker() {
    return isWeakForLinker(linkage);
  }

  public boolean maybeOverridden() {
    return maybeOverridden(linkage);
  }
  
  /**
   * An enumeration for the kinds of visibility of global values.
   */
  public enum VisibilityTypes {
    DefaultVisibility,  ///< The GV is visible
    HiddenVisibility,       ///< The GV is hidden
    ProtectedVisibility     ///< The GV is protected
  }

  private VisibilityTypes visibility;

  public boolean hasDefaultVisibility() {
    return visibility == VisibilityTypes.DefaultVisibility;
  }

  public boolean hasHiddenVisibility() {
    return visibility == VisibilityTypes.HiddenVisibility;
  }

  public VisibilityTypes getVisibility() {
    return visibility;
  }

  public void setVisibility(VisibilityTypes v) {
    visibility = v;
  }
}
