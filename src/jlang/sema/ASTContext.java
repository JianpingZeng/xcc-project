package jlang.sema;
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

import jlang.basic.LangOption;
import jlang.basic.TargetInfo;
import jlang.cparser.DeclContext;
import jlang.sema.Decl.*;
import jlang.type.*;

import java.util.LinkedList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class ASTContext
{
	public LangOption langOptions;
	public TargetInfo target;
	public LinkedList<Type> types = new LinkedList<>();

	public LangOption getLangOptions()
	{
		return langOptions;
	}

	/**
	 * Return the unique reference to the type for the specified
	 * TagDecl (struct/union/enum) decl.
	 * @param decl
	 * @return
	 */
	public QualType getTagDeclType(TagDecl decl)
	{
		assert decl != null;
		return getTypeDeclType(decl, null);
	}

	/**
	 * Return the unique reference to the type for the
	 * specified typename decl.
	 * @param decl
	 * @return
	 */
	public QualType getTypedefType(TypeDefDecl decl)
	{
		if (decl.getTypeForDecl() != null)
			return new QualType(decl.getTypeForDecl());

		QualType cannonical = decl.getUnderlyingType();
		decl.setTyepForDecl(new TypeDefType(TypeClass.TypeDef, decl, cannonical));
		types.addLast(decl.getTypeForDecl());
		return new QualType(decl.getTypeForDecl());
	}

	/**
	 * Return the unique reference to the type for the
	 * specified type declaration.
	 * @param decl
	 * @param prevDecl
	 * @return
	 */
	public QualType getTypeDeclType(TypeDecl decl, TypeDecl prevDecl)
	{
		assert decl != null:"Passed null for decl param";
		if (decl.getTypeForDecl() != null)
			return new QualType(decl.getTypeForDecl());

		if (decl instanceof TypeDefDecl)
		{
			TypeDefDecl typedef = (TypeDefDecl)decl;
			return getTypedefType(typedef);
		}
		if (decl instanceof RecordDecl)
		{
			RecordDecl record = (RecordDecl)decl;
			if (prevDecl != null)
				decl.setTyepForDecl(prevDecl.getTypeForDecl());
			else
				decl.setTyepForDecl(new RecordType(record));
		}
		else if (decl instanceof EnumDecl)
		{
			EnumDecl enumDecl = (EnumDecl)decl;
			if (prevDecl != null)
				decl.setTyepForDecl(prevDecl.getTypeForDecl());
			else
				decl.setTyepForDecl(new EnumType(enumDecl));
		}
		else
		{
			assert false:"TypeDecl without a type?";
		}
		if (prevDecl == null)
			types.addLast(decl.getTypeForDecl());
		return new QualType(decl.getTypeForDecl());
	}

	public QualType mergeType(QualType newOneDeclType, QualType two)
	{
		return null;
	}

	private DeclContext translateUnitDecl;

	public DeclContext getTranslateUnitDecl()
	{
		return translateUnitDecl;
	}
}
