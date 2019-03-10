package cfe.ast;

/**
 * storage and qualified flags for c-flat method and variable.
 *
 * @author Jianping Zeng
 */
public interface Flags {
  int STATIC = 1 << 0;
  int CONST = 1 << 1;

  int StandardFlags = 1 << CONST - 1;

  /**
   * Flag is set for a variable jlang.symbol if variable's definition
   * has initializer part.
   */
  int HASINIT = 1 << 13;

  /**
   * The flag set when this method jlang.symbol is unattributed before attributing phase.
   */
  int UNATTRIBUTED = 1 << 14;

  /**
   * A flag marks method parameter.
   */
  int PARAMETER = 1 << 17;
  int VarFlags = STATIC | CONST;
  int MethodFlags = STATIC;
}