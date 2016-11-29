package backend.value;
/*
 * Xlous C language CompilerInstance
 * Copyright (c) 2015-2016, Xlous
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

import backend.hir.Module;
import backend.type.PointerType;
import backend.type.Type;

import static backend.value.GlobalValue.LinkageType.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class GlobalValue extends Constant
{

    /**
     * An enumeration for the kinds of linkage for global variable and function.
     */
    public enum LinkageType
    {
	    /**
         * Externally visible function
         */
        ExternalLinkage,
	    /**
	     * Rename collisions when linking (static functions).
         */
        InteralLinkage,
	    /**
         * Like Internal, but omit from symbol table.
         */
        PrivateLinkage,
	    /**
         * Like Private, but linker removes.
         */
        LinkerPrivateLinkage,

        CommonLinkage,
    }

    protected Module parent;
    private LinkageType linkageType;
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
    public GlobalValue(Type ty, int valueType, LinkageType linkage, String name)
    {
        super(ty, valueType);
        this.name = name;
        linkageType = linkage;
    }

    public boolean isDeclaration()
    {
        if (this instanceof GlobalVariable)
            return ((GlobalVariable)this).getNumOfOperands() == 0;

        if (this instanceof Function)
            return ((Function)this).empty();

        return false;
    }

    /**
     * This method unlinks 'this' from the containing module
     * and deletes it.
     */
    public abstract void eraseFromParent();

    public Module getParent() {return parent;}
    public void setParent(Module newParent) {parent = newParent;}

    public boolean hasExternalLinkage() {return linkageType == LinkageType.ExternalLinkage;}

    public boolean hasInternalLinkage() {return linkageType == LinkageType.InteralLinkage;}

    public boolean hasPrivateLinkage() {return linkageType == LinkageType.PrivateLinkage;}
    public boolean hasLinkerPrivateLinkage() {return linkageType == LinkerPrivateLinkage;}
    public boolean hasLocalLinkage()
    {
        return hasInternalLinkage() || hasPrivateLinkage()
                || hasLinkerPrivateLinkage();
    }

    public void setLinkage(LinkageType newLinkage) {linkageType = newLinkage;}

    public LinkageType getLinkage() {return linkageType;}

    @Override
    public boolean isNullValue() {return false;}

    @Override
    public PointerType getType() { return (PointerType) super.getType();}

    public boolean hasSection() {return section != null;}

    public String getSection() {return section;}

    public void setSection(String newSection) {section = newSection;}

    /**
     * Return true if the primary definition of this global value is
     * outside of the current translation unit.
     * @return
     */
    public abstract boolean isExternal();

    public int getAlignment() {return alignment; }

    public void setAlignment(int align)
    {
        assert (align & (align - 1)) == 0:"Alignment must be power of 2!";
        alignment = align;
    }
}
