package ast;

import utils.Context;
import utils.Name;
import ast.Tree;
import ast.Tree.Ident;
import ast.Tree.Parens;
import ast.Tree.Select;

public class TreeInfo
{
	/**
	 * Operator precedences values.
	 */
	public static final int notExpression = -1;

	/**
	 * Operator precedences values.
	 */
	public static final int noPrec = 0;

	/**
	 * Operator precedences values.
	 */
	public static final int assignPrec = 1;

	/**
	 * Operator precedences values.
	 */
	public static final int assignopPrec = 2;

	/**
	 * Operator precedences values.
	 */
	public static final int condPrec = 3;

	/**
	 * Operator precedences values.
	 */
	public static final int orPrec = 4;

	/**
	 * Operator precedences values.
	 */
	public static final int andPrec = 5;

	/**
	 * Operator precedences values.
	 */
	public static final int bitorPrec = 6;

	/**
	 * Operator precedences values.
	 */
	public static final int bitxorPrec = 7;

	/**
	 * Operator precedences values.
	 */
	public static final int bitandPrec = 8;

	/**
	 * Operator precedences values.
	 */
	public static final int eqPrec = 9;

	/**
	 * Operator precedences values.
	 */
	public static final int ordPrec = 10;

	/**
	 * Operator precedences values.
	 */
	public static final int shiftPrec = 11;

	/**
	 * Operator precedences values.
	 */
	public static final int addPrec = 12;

	/**
	 * Operator precedences values.
	 */
	public static final int mulPrec = 13;

	/**
	 * Operator precedences values.
	 */
	public static final int prefixPrec = 14;

	/**
	 * Operator precedences values.
	 */
	public static final int postfixPrec = 15;

	/**
	 * Operator precedences values.
	 */
	public static final int precCount = 16;

	private static final Context.Key treeinfoKey = new Context.Key();
	
	public static TreeInfo instance(Context context)
	{
		TreeInfo instance = (TreeInfo)context.get(treeinfoKey);
		if (instance == null)
			instance = new TreeInfo(context);
		return instance;
	}
	
	public TreeInfo(Context context)
	{
		context.put(treeinfoKey, this);
        Name.Table names = Name.Table.instance(context);
        opname[Tree.POS - Tree.POS] = names.fromString("+");
        opname[Tree.NEG - Tree.POS] = names.hyphen;
        opname[Tree.NOT - Tree.POS] = names.fromString("!");
        opname[Tree.COMPL - Tree.POS] = names.fromString("~");
        opname[Tree.PREINC - Tree.POS] = names.fromString("++");
        opname[Tree.PREDEC - Tree.POS] = names.fromString("--");
        opname[Tree.POSTINC - Tree.POS] = names.fromString("++");
        opname[Tree.POSTDEC - Tree.POS] = names.fromString("--");
        opname[Tree.OR - Tree.POS] = names.fromString("||");
        opname[Tree.AND - Tree.POS] = names.fromString("&&");
        opname[Tree.EQ - Tree.POS] = names.fromString("==");
        opname[Tree.NE - Tree.POS] = names.fromString("!=");
        opname[Tree.LT - Tree.POS] = names.fromString("<");
        opname[Tree.GT - Tree.POS] = names.fromString(">");
        opname[Tree.LE - Tree.POS] = names.fromString("<=");
        opname[Tree.GE - Tree.POS] = names.fromString(">=");
        opname[Tree.BITOR - Tree.POS] = names.fromString("|");
        opname[Tree.BITXOR - Tree.POS] = names.fromString("^");
        opname[Tree.BITAND - Tree.POS] = names.fromString("&");
        opname[Tree.SL - Tree.POS] = names.fromString("<<");
        opname[Tree.SR - Tree.POS] = names.fromString(">>");
        opname[Tree.PLUS - Tree.POS] = names.fromString("+");
        opname[Tree.MINUS - Tree.POS] = names.hyphen;
        opname[Tree.MUL - Tree.POS] = names.asterisk;
        opname[Tree.DIV - Tree.POS] = names.slash;
        opname[Tree.MOD - Tree.POS] = names.fromString("%");
	}
	
	/**
	 * Map operators to their precedence levels.
	 */
	public static int opPrec(int op)
	{
		switch (op)
		{
			case Tree.POS:

			case Tree.NEG:

			case Tree.NOT:

			case Tree.COMPL:

			case Tree.PREINC:

			case Tree.PREDEC:
				return prefixPrec;

			case Tree.POSTINC:

			case Tree.POSTDEC:

				return postfixPrec;

			case Tree.ASSIGN:
				return assignPrec;

			case Tree.BITOR_ASG:

			case Tree.BITXOR_ASG:

			case Tree.BITAND_ASG:

			case Tree.SL_ASG:

			case Tree.SR_ASG:

			case Tree.PLUS_ASG:

			case Tree.MINUS_ASG:

			case Tree.MUL_ASG:

			case Tree.DIV_ASG:

			case Tree.MOD_ASG:
				return assignopPrec;

			case Tree.OR:
				return orPrec;

			case Tree.AND:
				return andPrec;

			case Tree.EQ:

			case Tree.NE:
				return eqPrec;

			case Tree.LT:

			case Tree.GT:

			case Tree.LE:

			case Tree.GE:
				return ordPrec;

			case Tree.BITOR:
				return bitorPrec;

			case Tree.BITXOR:
				return bitxorPrec;

			case Tree.BITAND:
				return bitandPrec;

			case Tree.SL:

			case Tree.SR:

				return shiftPrec;

			case Tree.PLUS:

			case Tree.MINUS:
				return addPrec;

			case Tree.MUL:

			case Tree.DIV:

			case Tree.MOD:
				return mulPrec;

			default:
				throw new AssertionError();

		}
	}

	/**
     * Return flags as a string, separated by " ".
     */
   public static String flagNames(long flags) 
   {
	   StringBuilder list = new StringBuilder();
	   long f = flags | Flags.StandardFlags;
	   int i  = 0;
	   while (f != 0)
	   {
		   if ((f & 1) != 0)
			{
			   if (list.length() != 0)
				   list.append(" ");
			   list.append(flagName[i]);
			}
		   i++;
		   f >>= 1;
	   }
	   return list.toString();
   }
   
   /**
    * If this tree is an identifier or a field or a parameterized type,
    *  return its name, otherwise return null.
    */
  public static Name name(Tree tree) {
      switch (tree.tag) {
      case Tree.IDENT:
          return ((Ident) tree).name;

      case Tree.SELECT:
          return ((Select) tree).name;

      default:
          return null;

      }
  }
   
  /**
   * Skip parens and return the enclosed expression
   */
 public static Tree skipParens(Tree tree) {
     while (tree.tag == Tree.PARENS) {
         tree = ((Parens) tree).expr;
     }
     return tree;
 }
  
  /**
   * Return name of operator with given tree tag.
   */
 public Name operatorName(int tag) {
     return opname[tag - Tree.POS];
 }
  
 /**
  * The names of all operators.
  */
private Name[] opname = new Name[Tree.MOD - Tree.POS + 1];
   private static final String[] flagName = {"static", "const"};
}
