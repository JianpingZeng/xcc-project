package hir; 

import hir.type.Type;

/** 
 * This class that represents an operand in Quad,
 * which are two different variety of subclasses, including literal and variable.
 *   
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2015年12月23日 上午9:10:45 
 */
public abstract class Operand {

	protected Type t;
	public Operand(Type t) {this.t = t;}
	
	public abstract boolean isVar();			
	
	public static abstract class ConstOperand extends Operand {
		public ConstOperand(Type t) {
			super(t);
		}
	}
	
	public static abstract class Const4Operand extends Operand {

		public Const4Operand(Type t) {
			super(t);
		
		}
				
	}
	
	public static abstract class Const8Operand extends Operand {

		public Const8Operand(Type t) {
			super(t);
		
		}		
	}
	
	public static class IConstOperand extends Const4Operand {
		
		private int value;		
		public IConstOperand(int value, Type t) {
			super(t);
			this.value = value;			
		}
		@Override
		public boolean isVar() {
			return false;
		}
		
	}
	
	public static class LConstOperand extends Const8Operand {
		private Long value;
		private Type t;
		
		public LConstOperand(Long value, Type t) {
			super(t);
			this.value = value;		
		}
		
		@Override
		public boolean isVar() {
			return false;
		}
	}
	
	
	public static class Var extends Operand {
		private String name;
		private Type t;
		
		public Var(String name, Type t) {
			super(t);
			this.name = name; 		
		}
		
		@Override
		public boolean isVar() {
			return true;
		}
	}
}


