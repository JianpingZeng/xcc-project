package backend.type;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

/**
 * <p>
 * The AbstractTypeUser class is an interface to be implemented by classes who
 * could possible use an abstract type.  Abstract types are denoted by the
 * isAbstract flag set to true in the Type class.  These are classes that
 * contain an Opaque type in their structure somehow.
 * </p>
 * <p>
 * Classes must implement this interface so that they may be notified when an
 * abstract type is resolved.  Abstract types may be resolved into more concrete
 * types through: linking, parsing, and bytecode reading.  When this happens,
 * all of the users of the type must be updated to reference the new, more
 * concrete type.  They are notified through the AbstractTypeUser interface.
 * </p>
 * <p>
 * In addition to this, AbstractTypeUsers must keep the use list of the
 * potentially abstract type that they reference up-to-date.  To do this in a
 * nice, transparent way, the PATypeHandle class is used to hold "Potentially
 * Abstract TypesParser", and keep the use list of the abstract types up-to-date.
 * </p>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface AbstractTypeUser {
  /**
   * The callback method invoked when an abstract type is
   * resolved to another type.  An object must override this method to update
   * its internal state to reference NewType instead of OldType.
   *
   * @param oldTy
   * @param newTy
   */
  void refineAbstractType(DerivedType oldTy, Type newTy);

  /**
   * The other case which AbstractTypeUsers must be aware of is when a type
   * makes the transition from being abstract (where it has clients on it's
   * AbstractTypeUsers list) to concrete (where it does not).  This method
   * notifies ATU's when this occurs for a type.
   *
   * @param absTy
   */
  void typeBecameConcrete(DerivedType absTy);

  /**
   * For debugging.
   */
  void dump();
}
