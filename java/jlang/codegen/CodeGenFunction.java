package jlang.codegen;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous
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

import backend.hir.HIRBuilder;
import backend.support.CallSite;
import backend.type.IntegerType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.GlobalValue.LinkageType;
import backend.value.Instruction.BranchInst;
import backend.value.Instruction.TerminatorInst;
import jlang.ast.Tree;
import jlang.basic.TargetInfo;
import jlang.codegen.CodeGenTypes.CGFunctionInfo;
import jlang.codegen.CodeGenTypes.CGFunctionInfo.ArgInfo;
import jlang.sema.ASTContext;
import jlang.sema.BinaryOperatorKind;
import jlang.sema.Decl;
import jlang.sema.Decl.*;
import jlang.sema.UnaryOperatorKind;
import jlang.support.SourceLocation;
import jlang.type.*;
import tools.APInt;
import tools.APSInt;
import tools.Pair;

import java.util.*;

import static jlang.ast.Tree.*;
import static jlang.codegen.RValue.*;

/**
 * This class responsible for generating LLVM code from AST.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CodeGenFunction
{
	private HIRModuleGenerator generator;
	private TargetInfo target;
	private FunctionDecl curFnDecl;
	private Function curFn;
	private QualType fnRetTy;
	private int id;

	/**
	 * Unified return block.
	 */
	private BasicBlock returnBlock;

	/**
	 * The temporary alloca to hold the return value.
	 * This is null iff the function has no return value.
	 */
	private Value returnValue;

	/**
	 * This is an instruction before which we prefer to insert allocas.
	 */
	private Instruction allocaInstPtr;

	private int nextCleanupDestIndex;

	HIRBuilder builder;

	/**
	 * Indicates if code generation of this function has finished.
	 */
	private boolean isFinished;

	/**
	 * A hashmap for keeping track of local variable declaration in C.
	 */
	private HashMap<Decl, Value> localVarMaps;

	/**
	 * Keeps track of the Basic block for each C label.
	 */
	private HashMap<LabelStmt, BasicBlock> labelMap;

    /**
     * This keeps track of the associated size for each VLA type.
     */
	private HashMap<Tree.Expr, Value> vlaSizeMap;

    /**
     * The CGFunctionInfo of the current function being compiled.
     */
	private CGFunctionInfo curFnInfo;

	public int pointerWidth;
	public Type BACKEND_INTTy;

	static class BreakContinue
	{
		BasicBlock breakBlock;
		BasicBlock continueBlock;

		BreakContinue(BasicBlock bb, BasicBlock cBB)
		{
			breakBlock = bb;
			continueBlock = cBB;
		}
	}

	private Stack<BreakContinue> breakContinueStack;

	/**
	 * This is nearest current switch instruction. It is null if if
	 * current context is not in a switch.
	 */
	private Instruction.SwitchInst switchInst;

	/**
	 * This block holds if condition check for last case
	 * statement range in current switch instruction.
	 */
	private BasicBlock caseRangeBlock;

	public CodeGenFunction(HIRModuleGenerator generator)
	{
		this.generator = generator;
		target = generator.getASTContext().target;
		builder = new HIRBuilder();
		localVarMaps = new HashMap<>();
		labelMap = new HashMap<>();
		vlaSizeMap = new HashMap<>();
		breakContinueStack = new Stack<>();
		pointerWidth = target.getPointerWidth(0);
		BACKEND_INTTy = convertType(getContext().IntTy);
	}

	public ASTContext getContext()
	{
		return generator.getASTContext();
	}

	/**
	 * This method is the entry for emitting HIR code for a function definition.
     * All emited LLVM IR code will be stored in the Function object {@code fn}.
	 * <p>
	 * It responsible for several tasks, emitting code for standard function
	 * prologue, function body statement, and function epilogue.
	 * </p>
	 * @param fd
	 * @param fn
	 */
	public void generateCode(FunctionDecl fd, Function fn)
	{
		QualType resTy = fd.getReturnType();

		// Obtains the a pair of VarDecl and QualType for each formal parameter.
		ArrayList<Pair<VarDecl, QualType>> functionArgList = new ArrayList<>(16);
		if (fd.getNumParams() > 0)
		{
            FunctionProtoType fproto = fd.getType().getAsFunctionProtoType();
            assert fproto != null :"Function def must have prototype!";

			for (int i = 0, e = fd.getNumParams(); i < e; i++)
				functionArgList.add(new Pair<>(fd.getParamDecl(i), fproto.getArgType(i)));
		}

		CompoundStmt body = fd.getCompoundBody();
		if (body != null)
		{
			// emit the standard function prologue.
			// Note that, this prologue and epilogue differs from calling convention
			// in Assembly code, we just determines what type of argument or
			// return value is passed into callee or get from callee with what
			// Calling convention, e.g. Indirect(by pointer), or direct,extend,
			// or expand, etc.
			startFunction(fd, resTy, fn, functionArgList, body.getLBraceLoc());
			// Generates code for function body.
			emitStmt(body);

			// emit standard function epilogue.
			finishFunction(body.getRBraceLoc());
		}
	}

    /**
     * Emit function prologue code and compute how to pass function argument and
     * the result is returned.
     * @param fd
     * @param resTy
     * @param fn
     * @param args
     * @param startLoc
     */
	private void startFunction(FunctionDecl fd,
                               QualType resTy,
                               Function fn,
                               ArrayList<Pair<VarDecl, QualType>> args,
                               SourceLocation startLoc)
	{
		curFn = fn;
		curFnDecl = fd;
		fnRetTy = resTy;

		// Before emit LLVM IR code for this function, it must be declaration.
		assert fn.isDeclaration() : "FunctionProto already has body.";

		BasicBlock entryBB = createBasicBlock("entry", curFn);

		// Create a marker to make it easy to insert allocas into the entryblock
		// later.  Don't create this with the builder, because we don't want it
		// folded.
		Value undef = Value.UndefValue.get(Type.Int32Ty);
		allocaInstPtr = new Instruction.BitCastInst(undef, Type.Int32Ty, "allocapt");

		returnBlock = createBasicBlock("return");
		builder.setInsertPoint(entryBB);

		if (!resTy.isVoidType())
			returnValue = createAllocaTemp(resTy, "retval");

		curFnInfo = generator.getCodeGenTypes().getFunctionInfo(fnRetTy, args);
		emitFunctionPrologue(curFn, curFnInfo, args);

		// If any of the arguments have a variably modified type,
		// make sure to emit type getNumOfSubLoop.
		args.forEach((pair)->
		{
			VarDecl vd = pair.first;
			QualType ty = vd.getType();
			if (ty.isVariablyModifiedType())
				emitVLASize(ty);
		});
	}

	/**
	 * Emits standard prologue code for function definition.
	 *
	 * @param fn
	 * @param args
	 */
	private void emitFunctionPrologue(Function fn, CGFunctionInfo fi,
			ArrayList<Pair<VarDecl, QualType>> args)
	{
		if (curFnDecl.hasImplicitReturnZero())
		{
			QualType retType = curFnDecl.getReturnType().getUnQualifiedType();
			Type backendTy = generator.getCodeGenTypes().convertType(retType);
			Constant zero = Constant.getNullValue(backendTy);
			builder.createStore(zero, returnValue);
		}

		assert fn.getNumOfArgs() == args.size()
                : "Mismatch between function signature and argumens";

        // emit alloca inst for param decls.  Give the HIR argument nodes names.
		int ai = 0, aiEnd = fn.getNumOfArgs();

		// obtains the ABIArgInfo list (contains the type and arg AI) of formal
		// type enclosing in FunctionType.
		int infoItr = 0;
		fi.getNumOfArgs();

		for (Pair<VarDecl, QualType> pair : args)
		{
			VarDecl arg = pair.first;
			QualType ty = fi.getArgInfoAt(infoItr).type;
			ABIArgInfo argAI = fi.getArgInfoAt(infoItr).info;

			boolean isPromoted = arg instanceof ParamVarDecl;
			assert ai != aiEnd: "Argument mismatch!";

			switch (argAI.getKind())
			{
				case Indirect:
				{
					Value v = fn.argAt(ai);
					if (hasAggregateBackendType(ty))
					{
						// Do nothing, aggregates and complex variables are accessed by
						// reference.
					}
					else
					{
						// Load a scalar value from indirect argument.
						//int align = QualType.getTypeAlignInBytes(ty);
						v = emitLoadOfScalar(v, false, ty);
						if (!getContext().isCompatible(ty, arg.getType()))
						{
							// This must be a promotion, for something like
							// "void a(x) short x; {..."
							v = emitScalarConversion(v, ty, arg.getType());
						}
						//if (isPromoted)
						//	v = emitArgumentDemotion(arg, v);
					}
					emitParamDecl(arg, v);
					break;
				}
				case Direct:
                case Extend:
				{
					assert ai != aiEnd : "Argument mismatch!";
					Value v = fn.argAt(ai);
					if (hasAggregateBackendType(ty))
					{
						// Create a temporary alloca to hold the argument; the rest of
						// codegen expects to access aggregates & complex values by
						// reference.
						v = createTempAlloc(convertTypeForMem(ty));
						builder.createStore(v, v);
					}
					else
					{
						if (!getContext().isCompatible(ty, arg.getType()))
						{
							// This must be a promotion, for something like
							// "void a(x) short x; {..."
							v = emitScalarConversion(v, ty, arg.getType());
						}
					}
					emitParamDecl(arg, v);
					break;
				}
                case Expand:
                {
                    String name = arg.getNameAsString();
                    Value temp = createTempAlloc(convertTypeForMem(ty), name + ".addr");

                    // Emit each argument for each expanded structure field.
                    int end = expandTypeFromArgs(ty, LValue.makeAddr(temp, 0), fn, ai);

                    int index = 0;
                    for (; ai < end; ++ai, ++index)
                    {
                        fn.argAt(index).setName(name + "." + index);
                    }

                    continue;
                }
				case Ignore:
				{
					// Initialize the local variable appropriately.
					if (hasAggregateBackendType(ty))
						emitParamDecl(arg, createTempAlloc(convertTypeForMem(ty)));
					else
						emitParamDecl(arg, Value.UndefValue.get(convertType(arg.getType())));

					// Skip increment, no matching parameter.
					continue;
				}
                case Coerce:
                {
                    assert ai != aiEnd :"Argument mismatch!";

                    Value temp = createTempAlloc(convertTypeForMem(ty), "coerce");
                    createCoercedStore(fn.argAt(ai), temp, this);
                    if (!hasAggregateBackendType(ty))
                    {
                        temp = emitLoadOfScalar(temp, false, ty);
                        if (!getContext().isCompatible(ty, arg.getType()))
                        {
                            // This must be a promotion, for assumpting like
                            // "void a(x)  short x; {...}"
                            temp = emitScalarConversion(temp, ty, arg.getType());
                        }
                    }
                    emitParamDecl(arg, temp);
                    break;
                }
			}
			++ai;
		}
		assert ai == aiEnd :"Argument mismatch!";
	}

    /**
     * Create a store to {@code destPtr} from {@code src}, where the source
     * and destination may have different type.
     *
     * This saftly handle the case when src type is larger than the type of
     * destPtr type; the Upper bits of src will be lost.
     * @param src
     * @param destPtr
     * @param cgf
     */
    private static void createCoercedStore(
            Value src,
            Value destPtr,
            CodeGenFunction cgf)
    {
        Type srcTy = src.getType();
        // The destination must be pointer to a value of type Element type.
        Type destTy = ((PointerType)destPtr.getType()).getElementType();

        long srcSize = cgf.generator.getTargetData().getTypeAllocSize(srcTy);
        long destSize = cgf.generator.getTargetData().getTypeAllocSize(destTy);;

        // If the size of source type is less than or equal to the destination size.
        // It is safe to directly write.
        if (srcSize <= destSize)
        {
            Value casted = cgf.builder.createBitCast(destPtr, PointerType.getUnqual(srcTy));
            cgf.builder.createStore(src, casted).setAlignment(1);
        }
        else
        {
            // Otherwise, do coercion through memory. This is stupid, but simple.

            Value tmp = cgf.createTempAlloc(srcTy);
            cgf.builder.createStore(src, tmp);
            Value casted = cgf.builder.createBitCast(tmp, PointerType.getUnqual(destTy));
            Instruction.LoadInst li = cgf.builder.createLoad(casted);
            li.setAlignment(1);
            cgf.builder.createStore(li, destPtr);
        }
    }

    private int expandTypeFromArgs(QualType ty, LValue lValue, Function fn,
            int ai)
    {
        RecordType rt = ty.getAsStructureType();
        assert rt != null :"Can only expand argument for structure type";

        RecordDecl rd = rt.getDecl();
        assert lValue.isSimple() :"Can not expand for structure type whose field"
                + "is bitfield";
        Value addr = lValue.getAddress();

        for (int i = 0, e = rd.getNumFields(); i < e; i++)
        {
            FieldDecl fd = rd.getDeclAt(i);
            QualType ft = fd.getType();

            LValue lv = emitLValueForField(addr, fd, false, 0);
            if (hasAggregateBackendType(ft))
                ai = expandTypeFromArgs(ft, lv, fn, ai);
            else
            {
                emitStoreThroughLValue(get(fn.argAt(ai)), lv, ft);
                ++ai;
            }
        }
        return ai;
    }

    /**
	 * Emits conversion instruction from the specified jlang type to another frotnend tyep.
	 * <p>
	 * Both type must be no Aggregate type.
	 *
	 * @param v
	 * @param srcTy
	 * @param destTy
	 * @return
	 */
	private Value emitScalarConversion(Value v, QualType srcTy, QualType destTy)
	{
		assert !hasAggregateBackendType(srcTy) && !hasAggregateBackendType(
				destTy) : "Invalid scalar expression to emit!";
		return new ScalarExprEmitter(this)
				.emitScalarConversion(v, srcTy, destTy);
	}

	/**
	 * emit a alloca for realistic argument and fills locaVarMaps.
	 * @param param
	 * @param arg
	 */
	private void emitParamDecl(VarDecl param, Value arg)
	{
		assert param instanceof ParamVarDecl
                : "Invalid argument to emitParamDecl()";

		QualType ty = param.getType();
		Value destPtr = null;
		if (!ty.isConstantSizeType())
		{
			// Variable sized values always passed by reference.
			destPtr = arg;
		}
		else
		{
			// A fixed getNumOfSubLoop single-value varibale becomes an alloca in the entry block.
			Type lty = convertTypeForMem(ty);
			if (lty.isSingleValueType())
			{
				String name = param.getDeclName() + ".addr";
				destPtr = createTempAlloc(lty);
				destPtr.setName(name);

				// Store the initial value into the alloca just created.
				emitStoreOfScalar(arg, destPtr, ty);
			}
			else
			{
				// Otherwise, if this is an aggregate type, just use the input pointer.
				destPtr = arg;
			}
			arg.setName(param.getNameAsString());
		}

		Value entry = localVarMaps.get(param);
		assert entry == null :"Decl already existing in localVarMaps";

		entry = destPtr;
		localVarMaps.put(param, entry);
	}

	private backend.type.Type convertTypeForMem(QualType ty)
	{
		return generator.getCodeGenTypes().convertTypeForMem(ty);
	}

	public static boolean hasAggregateBackendType(QualType ty)
	{
		return !ty.isPointerType() && !ty.isRealType() && !ty.isVoidType()
				&& !ty.isFunctionType();
	}

	private Value createAllocaTemp(QualType ty, String name)
	{
		Instruction.AllocaInst alloc = createTempAlloc(convertType(ty), name);
		return alloc;
	}

	/**
	 * This creates a alloca and inserts it into the entry block.
	 *
	 * @param ty
	 * @param name
	 * @return
	 */
	public Instruction.AllocaInst createTempAlloc(Type ty, String name)
	{
		return new Instruction.AllocaInst(ty, null, name, allocaInstPtr);
	}

	private Instruction.AllocaInst createTempAlloc(Type ty)
	{
		return new Instruction.AllocaInst(ty, null, "temp", allocaInstPtr);
	}

	public Type convertType(QualType t)
	{
		return generator.getCodeGenTypes().convertType(t);
	}

	public void emitStmt(Tree.Stmt stmt)
	{
		assert stmt != null : "Null Statement!";

		// Check if we can handle this without bother to generate an
		// insert point.
		if (emitSimpleStmt(stmt))
			return;

		// Checks if there is a insertion point where emitted code will be reside.
		if (!hasInsertPoint())
		{
			// If so, and the statement doesn't contain a label, then we do not need to
			// generate actual code. This is safe because (1) the current point is
			// unreachable, so we don't need to execute the code, and (2) we've already
			// handled the statements which update internal data structures (like the
			// local variable map) which could be used by subsequent statements.
			if (!containsLabel(stmt, false))
			{
				// Verify that any decl statements were handled as simple, they may be in
				// scope of subsequent reachable statements.
				assert !(stmt instanceof DeclStmt) : "Unexpected DeclStmt!";
				return;
			}

			// Otherwise, make a new block to hold the code.
			ensureInsertPoint();
		}
		switch (stmt.getStmtClass())
		{
			default:
				break;
			case IfStmtClass:
				emitIfStmt((IfStmt) stmt);
				break;
			case WhileStmtClass:
				emitWhileStmt((WhileStmt) stmt);
				break;
			case DoStmtClass:
				emitDoStmt((DoStmt) stmt);
				break;
			case ForStmtClass:
				emitForStmt((ForStmt) stmt);
				break;
			case ReturnStmtClass:
				emitReturnStmt((ReturnStmt) stmt);
				break;
			case SwitchStmtClass:
				emitSwitchStmt((SwitchStmt) stmt);
				break;
		}
	}

	/**
	 * C99 6.8.4.1: The first substatement is executed if the expression compares
	 * unequal to 0.  The condition must be a scalar type.
	 *
	 * @param s
	 */
	private void emitIfStmt(IfStmt s)
	{
		// If the condition constant folds and can be elided, try to avoid emitting
		// the condition and the dead arm of the if/else.
		int cond = constantFoldsToSimpleInteger(s.getCond());
		if (cond != 0)
		{
			// Figure out which block (then or else) is executed.
			Stmt executed = s.getThenPart(), skipped = s.getElsePart();
			if (cond == -1) // condition is false.
			{
				Stmt temp = executed;
				executed = skipped;
				skipped = temp;
			}

			// if skipped block has no labels within it, just emit code
			// for executed stmt. This avoids emitting dead code.
			if (!containsLabel(skipped, false))
			{
				if (executed != null)
					emitStmt(executed);
				return;
			}
		}

		// Otherwise, the condition can not folded or the condition would be folded
		// but there is a label contained in this conditional expression. In the
		// both cases, just emit thenBB first and then falseBB.
		BasicBlock thenBB = createBasicBlock("if.then");
		BasicBlock endBB = createBasicBlock("if.end");
		BasicBlock elseBB = endBB;
		if (s.getElsePart() != null)
			elseBB = createBasicBlock("if.else");

		emitBranchOnBoolExpr(s.getCond(), thenBB, elseBB);

		// emit the 'then' code.
		emitBlock(thenBB);
		emitStmt(s.getThenPart());
		emitBranch(endBB);

		// emit the 'else' cdoe if present.
		if (s.getElsePart() != null)
		{
			emitBlock(elseBB);
			emitStmt(s.getElsePart());
			emitBranch(endBB);
		}

		// emit the continuation block for code after the if stmt.
		emitBlock(endBB, true);
	}

	/**
	 * emit a branch instruction on the specified condition is evaluated as boolean.
	 * When condition is false, branch to {@code trueBB}, othewise to {@code falseBB}.
	 * <p>Based on condition, it might be taken to simply the codegen of the branch.
	 * </p>
	 *
	 * @param cond
	 * @param trueBB
	 * @param falseBB
	 */
	public void emitBranchOnBoolExpr(
			Expr cond,
			BasicBlock trueBB,
			BasicBlock falseBB)
	{
		// Strips the parenthesis outsides sub-expression.
		if (cond instanceof ParenExpr)
			cond = ((ParenExpr) cond).getSubExpr();

		if (cond instanceof BinaryExpr)
		{
			BinaryExpr condBOp = (BinaryExpr) cond;
			// Handle x&&y in a condition.
			if (condBOp.getOpcode() == BinaryOperatorKind.BO_LAnd)
			{
				// If we have "1 && X", simplify the code.  "0 && X" would have constant
				// folded if the case was simple enough.
				if (constantFoldsToSimpleInteger(condBOp.getLHS()) == 1)
				{
					// br (1&&x) -> br x.
					emitBranchOnBoolExpr(condBOp.getRHS(), trueBB, falseBB);
					return;
				}

				// If we have "X && 1", simplify the code to use an uncond branch.
				// "X && 0" would have been constant folded to 0.
				if (constantFoldsToSimpleInteger(condBOp.getRHS()) == 1)
				{
					// br (x&&1) -> br x.
					emitBranchOnBoolExpr(condBOp.getLHS(), trueBB, falseBB);
					return;
				}

				// emit the LHS as a conditional.  If the LHS conditional is false, we
				// want to jump to the FalseBlock.
				BasicBlock lhsTrue = createBasicBlock("land.lhs.true");
				emitBranchOnBoolExpr(condBOp.getLHS(), lhsTrue, falseBB);
				emitBlock(lhsTrue);

				emitBranchOnBoolExpr(condBOp.getRHS(), trueBB, falseBB);
				return;
			}
			else if (condBOp.getOpcode() == BinaryOperatorKind.BO_LOr)
			{
				// 0 || X or X || 0.
				if (constantFoldsToSimpleInteger(condBOp.getLHS()) == -1)
				{
					// br (0||X) -> br (x).
					emitBranchOnBoolExpr(condBOp.getRHS(), trueBB, falseBB);
					return;
				}

				if (constantFoldsToSimpleInteger(condBOp.getRHS()) == -1)
				{
					// br (X||0) -> br (x).
					emitBranchOnBoolExpr(condBOp.getLHS(), trueBB, falseBB);
					return;
				}

				// emit the branch as regural conditional.
				BasicBlock lhsFalse = createBasicBlock("lor.lhs.false");
				emitBranchOnBoolExpr(condBOp.getLHS(), trueBB, lhsFalse);
				emitBlock(lhsFalse);

				emitBranchOnBoolExpr(condBOp.getRHS(), trueBB, falseBB);
				return;
			}
		}

		if (cond instanceof UnaryExpr)
		{
			UnaryExpr condUOp = (UnaryExpr) cond;
			// br(!x, t, f) -> br(x, f, t).
			if (condUOp.getOpCode() == UnaryOperatorKind.UO_LNot)
			{
				emitBranchOnBoolExpr(condUOp.getSubExpr(), trueBB, falseBB);
				return;
			}
		}

		if (cond instanceof ConditionalExpr)
		{
			ConditionalExpr condOp = (ConditionalExpr) cond;
			// handles ?: operator.

			// br (c ? x: y, t, f) -> br (c, br (x, t, f), br(y, t, f)).
			BasicBlock lhsBlock = createBasicBlock("cond.true");
			BasicBlock rhsBlock = createBasicBlock("cond.false");
			emitBranchOnBoolExpr(condOp.getCond(), lhsBlock, rhsBlock);

			emitBlock(lhsBlock);
			emitBranchOnBoolExpr(condOp.getTrueExpr(), trueBB, falseBB);

			emitBlock(rhsBlock);
			emitBranchOnBoolExpr(condOp.getFalseExpr(), trueBB, falseBB);
			return;
		}

		// emit code for the general cases.
		Value condVal = evaluateExprAsBool(cond);
		builder.createCondBr(condVal, trueBB, falseBB);
	}

	/**
	 * Evaluates the specified conditional expression as a boolean value.
	 *
	 * @param cond
	 * @return
	 */
	public Value evaluateExprAsBool(Expr cond)
	{
		QualType boolTy =  getContext().BoolTy;
		assert !cond.getType().isComplexType()
                : "Current complex type not be supported.";

		return emitScalarConversion(emitScalarExpr(cond), cond.getType(),
				boolTy);
	}

	/**
	 * Emits code for while stmt.
	 *
	 * @param s
	 */
	private void emitWhileStmt(WhileStmt s)
	{
		// create a basic block for condition when continue encountered in while.
		// emit the header for the loop, insert it, which will create an uncond br to
		// it.
		BasicBlock condBB = createBasicBlock("while.cond");
		emitBlock(condBB);

		BasicBlock exitBB = createBasicBlock("while.end");
		BasicBlock loopBody = createBasicBlock("while.body");

		// stores the blocks to use for break/continue stmt.
		breakContinueStack.push(new BreakContinue(exitBB, loopBody));

		Value condVal = evaluateExprAsBool(s.getCond());

		// while(1) is common, avoid extra exit blocks.  Be sure
		// to correctly handle break/continue though.
		boolean emitBoolCondBranch = true;
		if (condVal instanceof ConstantInt)
		{
			ConstantInt c = (ConstantInt) condVal;
			if (c.isOne())
				emitBoolCondBranch = false;
		}

		// As long as the condition is true, go to the loop body.
		if (emitBoolCondBranch)
			builder.createCondBr(condVal, loopBody, exitBB);

		// emit the loop body.
		emitBlock(loopBody);
		emitStmt(s.getBody());

		breakContinueStack.pop();

		// goto the condition.
		emitBlock(condBB);

		// emit the exit block.
		emitBlock(exitBB, true);

		// the loop header typically is just a branch if we skipping emitting
		// a branch, try to erase it.
		if (!emitBoolCondBranch)
			simplifyForwardingBlocks(condBB);
	}

	private void simplifyForwardingBlocks(BasicBlock bb)
	{
		TerminatorInst ti = bb.getTerminator();
		if (!(ti instanceof BranchInst))
			return;
		BranchInst inst = (BranchInst)ti;
		// Can only simplify direct branch.
		if (inst == null || !inst.isUnconditional())
			return;

		bb.replaceAllUsesWith(inst.getSuccessor(0));
		inst.eraseFromParent();
		bb.eraseFromParent();
	}

	private void emitDoStmt(DoStmt s)
	{
		// emit the body of the loop, insert it into blocks list.
		BasicBlock loopBody = createBasicBlock("do.body");
		BasicBlock loopEnd = createBasicBlock("do.end");

		// creates this block for continue stmt.
		BasicBlock doCond = createBasicBlock("do.cond");

		emitBlock(loopBody);

		// Store the blocks to use for break and continue.
		breakContinueStack.push(new BreakContinue(loopEnd, doCond));

		emitStmt(s.getBody());

		breakContinueStack.pop();

		emitBlock(doCond);

		// C99 6.8.5.2: "The evaluation of the controlling expression takes place
		// after each execution of the loop body."
		Value boolCondVal = evaluateExprAsBool(s.getCond());

		boolean shouldEmitBranch = true;

		// "do {...}while(0)" is common case in macros, avoid extra blocks.
		if (boolCondVal instanceof ConstantInt)
		{
			ConstantInt c = (ConstantInt) boolCondVal;
			if (c.isZero())
				shouldEmitBranch = false;
		}

		// As long as the condition is true, iterate the loop.
		if (shouldEmitBranch)
			builder.createCondBr(boolCondVal, loopBody, loopEnd);

		// emit the loop exit block.
		emitBlock(loopEnd);

		if (!shouldEmitBranch)
			simplifyForwardingBlocks(doCond);
	}

	private void emitForStmt(ForStmt s)
	{
		// emit the code for the first part.
		if (s.getInit() != null)
			emitStmt(s.getInit());

		// The condition and end block are absolutely needed.
		BasicBlock condBlock = createBasicBlock("for.cond");
		BasicBlock forEnd = createBasicBlock("for.end");

		// emit the condition block.
		emitBlock(condBlock);

		// emits the code for condition if present. If not,
		// treat it as a non-zero constant according to C99 6.8.5.3p2.
		if (s.getCond() != null)
		{
			BasicBlock forBody = createBasicBlock("for.body");

			// C99 6.8.5p2/p4: The first sub-statement is executed if the expression
			// compares unequal to 0.  The condition must be a scalar type.
			emitBranchOnBoolExpr(s.getCond(), forBody, forEnd);
			emitBlock(forBody);
		}
		else
		{
			// Treat it as a non-zero constant.  Don't even create a new block for the
			// body, just fall into it.
		}

		BasicBlock continueBlock;
		if (s.getStep() != null)
			continueBlock = createBasicBlock("for.inc");
		else
			continueBlock = condBlock;

		// Stores the blocks for break/continue stmt.
		breakContinueStack.push(new BreakContinue(forEnd, continueBlock));

		emitStmt(s.getBody());

		breakContinueStack.pop();

		// If there is an increment, emit it next.
		if (s.getStep() != null)
		{
			emitBlock(continueBlock);
			emitStmt(s.getStep());
		}

		// Finally, branch back up to the condition for the next iteration.
		emitBranch(condBlock);

		// emit the loop exit block.
		emitBlock(forEnd, true);
	}

	private void emitReturnStmt(ReturnStmt s)
	{
		// emit the result value, even if unused, to evalute the side effects.
		final Expr resultVal = s.getRetValue();

		// if this function have no return value.
		if (returnValue == null)
		{
			// Make sure not to return any value, but it is needed to evaluate
			// return expression due to side effect.
			if (resultVal != null)
				emitAnyExpr(resultVal);
		}
		else if (resultVal == null)
		{
			// Do nothing.
		}
		else if (!hasAggregateBackendType(resultVal.getType()))
		{
			// The type of return stmt is not aggregate type.
			builder.createStore(emitScalarExpr(resultVal), returnValue);
		}
		else if (resultVal.getType().isComplexType())
		{
			// TODO.
			assert false:"ComplexType is not supported!";
		}
		else
		{
			emitAggExpr(resultVal, returnValue);
		}
	}

	public RValue emitAnyExpr(Expr e)
	{
		return emitAnyExpr(e, null, false, false, false);
	}

	/**
	 * emit code to compute the specified expression with any type.
	 *
	 * @param e
	 */
	public RValue emitAnyExpr(Expr e, Value aggLoc, boolean isAggLocVolatile,
			boolean ignoreResult, boolean isInitializer)
	{
		if (!hasAggregateBackendType(e.getType()))
			return get(emitScalarExpr(e));
		else if (e.getType().isComplexType())
		{
			// TODO.
		}
		emitAggExpr(e, aggLoc, ignoreResult, isInitializer);
		return getAggregate(aggLoc, isAggLocVolatile);
	}

	private void emitSwitchStmt(SwitchStmt s)
	{
		Value condV = emitScalarExpr(s.getCond());

		// Handle nested switch statement.
		Instruction.SwitchInst savedSwitchInst = switchInst;
		BasicBlock savedCRBlock = caseRangeBlock;

		// Create basic block to hold stuff that comes after switch
		// statement. We also need to create a default block now so that
		// explicit case ranges tests can have a place to jump to on
		// failure.
		BasicBlock nextBB = createBasicBlock("sw.epilog");
		BasicBlock defaultBB = createBasicBlock("sw.default");
		switchInst = builder.createSwitch(condV, defaultBB);
		caseRangeBlock = defaultBB;

		// clears the insertion point to indicate we are in unreachable point.
		builder.clearInsertPoint();

		// All break statements jump to NextBlock. If BreakContinueStack is non empty
		// then reuse last ContinueBlock.
		BasicBlock continueBB = null;
		if (!breakContinueStack.isEmpty())
			continueBB = breakContinueStack.peek().continueBlock;

		// Ensure any vlas created between there and here, are undone
		breakContinueStack.add(new BreakContinue(nextBB, continueBB));

		// Emits the switch body.
		emitStmt(s.getBody());

		breakContinueStack.pop();

		// Update the default block in case explicit case range tests have
		// been chained on top.
		switchInst.setSuxAt(0, caseRangeBlock);

		if (defaultBB.getParent() == null)
		{
			defaultBB.replaceAllUsesWith(nextBB);
			defaultBB = null;
		}

		// emit continuation.
		emitBlock(nextBB, true);

		switchInst = savedSwitchInst;
		caseRangeBlock = savedCRBlock;
	}

	/**
	 * If the specified expression does not fold to a constant, or if it does
	 * but contains a label, return 0.  If it constant folds to 'true' and does
	 * not contain a label, return 1, if it constant folds to 'false' and does
	 * not contain a label, return -1.
	 *
	 * @param expr
	 * @return
	 */
	public int constantFoldsToSimpleInteger(Expr expr)
	{
		Expr.EvalResult result = new Expr.EvalResult();
		if (!expr.evaluate(result, getContext()) || !result.getValue().isInt() || result
				.hasSideEffects())
			return 0;   // can not foldable not integral or not fully evaluatable.

		if (containsLabel(expr, false))
			return 0;

		return result.getValue().getInt().getBoolValue() ? 1 : -1;
	}

	private boolean emitSimpleStmt(Tree.Stmt s)
	{
		switch (s.getStmtClass())
		{
			default:
				return false;
			case NullStmtClass:
			case CompoundStmtClass:
				emitCompoundStmt((Tree.CompoundStmt)s);
				return true;
			case DeclStmtClass:
				emitDeclStmt((Tree.DeclStmt) s);
				return true;
			case LabelledStmtClass:
				emitLabelStmt((LabelStmt) s);
				return true;
			case GotoStmtClass:
				emitGotoStmt((Tree.GotoStmt) s);
				return true;
			case BreakStmtClass:
				emitBreakStmt((Tree.BreakStmt) s);
				return true;
			case ContinueStmtClass:
				emitContinueStmt((Tree.ContinueStmt) s);
				return true;
			case DefaultStmtClass:
				emitDefaultStmt((Tree.DefaultStmt) s);
				return true;
			case CaseStmtClass:
				emitCaseStmt((Tree.CaseStmt) s);
				return true;
		}
	}

	private void emitLabelStmt(LabelStmt stmt)
	{
		emitLabel(stmt);
		emitStmt(stmt.body);
	}

	private void emitLabel(LabelStmt s)
	{
		emitBlock(getBasicBlockForLabel(s));
	}

	private BasicBlock getBasicBlockForLabel(LabelStmt s)
	{
		BasicBlock BB = labelMap.get(s);
		if (BB != null)
			return BB;

		BB = createBasicBlock(s.getName());
		labelMap.put(s, BB);
		return BB;
	}

	/**
	 * Emits code for the compound statement, like {...}.
	 *
	 * @param s
	 */
	public void emitCompoundStmt(Tree.CompoundStmt s)
	{
	    Stmt[] stmts = s.getBody();
	    for (Stmt stmt : stmts)
		{
			emitStmt(stmt);
		}
	}

	public void emitDeclStmt(Tree.DeclStmt s)
	{
		for (Iterator<Decl> itr = s.iterator(); itr.hasNext(); )
		{
			emitDecl(itr.next());
		}
	}

	public void emitDecl(Decl decl)
	{
		switch (decl.getKind())
		{
			default:
				assert false : "Undefined decl type.";
				break;
			case ParamVarDecl:
				assert false : "ParamDecls should not be handled in emitDecl().";
				break;
			case FunctionDecl: // void foo();
			case RecordDecl:   // struct/union X;
			case EnumDecl:     // enum X;
			case EnumConstant: // enum ? {X =?,};
				// none of those decls required codegen support.
				return;
			case VarDecl:
			{
				VarDecl vd = (VarDecl)decl;
				assert vd.isBlockVarDecl()
						: "Should not see file-scope variable declaration.";
				emitBlockVarDecl(vd);
				return;
			}
			case TypedefDecl: // typedef int x;
			{
				Decl.TypeDefDecl tf = (Decl.TypeDefDecl) decl;
				QualType ty = tf.getUnderlyingType();

				if (ty.isVariableArrayType())
				    emitVLASize(ty);
			}
		}
	}

	/**
	 * This method handles any variable function inside a function.
	 *
	 * @param vd
	 * @return
	 */
	private void emitBlockVarDecl(VarDecl vd)
	{
		switch (vd.getStorageClass())
		{
			case SC_none:
			case SC_auto:
			case SC_register:
				emitLocalBlockVarDecl(vd);
				return;
			case SC_static:
				emitStaticBlockVarDecl(vd);
				return;
			case SC_extern:
				return;
		}
		assert false : "Undefined storage class.";
	}

	/**
	 * <p>Emits code and set up an entry in LocalDeclMap for a variable declaration
	 * with auto, register, or on storage class specifier.
	 * </p>
	 * <p>These turn into simple stack objects, or {@linkplain GlobalValue}
	 * depending on TargetData.
	 * </p>
	 *
	 * @param vd
	 */
	public void emitLocalBlockVarDecl(VarDecl vd)
	{
		QualType ty = vd.getType();
		backend.value.Value declPtr;
		if (ty.isConstantSizeType())
		{
			// A normal fixed sized variable becomes an alloca in the entry block.
			backend.type.Type lty = convertTypeForMem(ty);
			Instruction.AllocaInst alloca = createTempAlloc(lty);
			alloca.setName(vd.getNameAsString());

			declPtr = alloca;
		}
		else
		{
			ensureInsertPoint();

			Type elemTy = convertTypeForMem(ty);
			Type elemPtrTy = PointerType.getUnqual(elemTy);

			Value vlaSize = emitVLASize(ty);

			// downcast the VLA getNumOfSubLoop expression.
			vlaSize = builder.createIntCast(vlaSize, Type.Int32Ty, false, "");
			// allocate an array with variable getNumOfSubLoop.
			Value vla = builder.createAlloca(Type.Int8Ty, vlaSize, "vla");

			// convert the pointer to array into regular pointer.
			declPtr = builder.createBitCast(vla, elemPtrTy, "temp");
		}

		assert !localVarMaps.containsKey(vd) : "Decl already exits in LocalVarMaps";

        localVarMaps.put(vd, declPtr);

		// if this local var has initializer, emit it.
		Tree.Expr init = vd.getInit();

		// If we are at an unreachable point, we don't need to emit the initializer
		// unless it contains a label.
		if (!hasInsertPoint())
		{
			if (!containsLabel(init, false))
				init = null;
			else
				ensureInsertPoint();
		}

		if (init != null)
		{
            if (!hasAggregateBackendType(init.getType()))
			{
				Value v = emitScalarExpr(init);
				emitStoreOfScalar(v, declPtr, vd.getType());
			}
			else if (init.getType().isComplexType())
			{
				// todo handle var declaration of typed complex type.
			}
			else
			{
				emitAggExpr(init, declPtr);
			}
		}
	}

	public Value emitVLASize(QualType type)
	{
		assert type.isVariablyModifiedType()
                : "Must pass variably modified type to EmitVLASizes!";

		ensureInsertPoint();
		ArrayType.VariableArrayType vat = getContext().getAsVariableArrayType(type);

		if (vat != null)
		{
			Value sizeEntry = null;
			if (!vlaSizeMap.containsKey(vat.getSizeExpr()))
			{
				Type sizeTy = convertType(vat.getSizeExpr().getType());

				// get the element size.
				QualType elemTy = vat.getElementType();
				Value elemSize;
				if (elemTy.isVariableArrayType())
					elemSize = emitVLASize(elemTy);
				else
					elemSize = ConstantInt.get(sizeTy, getContext().getTypeSize(elemTy) / 8);

				Value numElements = emitScalarExpr(vat.getSizeExpr());
				numElements = builder.createIntCast(numElements, sizeTy, false, "tmp");
				sizeEntry = builder.createMul(elemSize, numElements, "");

                vlaSizeMap.put(vat.getSizeExpr(), sizeEntry);
			}

			sizeEntry = vlaSizeMap.get(vat.getSizeExpr());
			return sizeEntry;
		}

		ArrayType at = getContext().getAsArrayType(type);
		if (at != null)
		{
			emitVLASize(at.getElementType());
			return null;
		}

        assert type.getType() instanceof jlang.type.PointerType : "unknown VM type!";
		jlang.type.PointerType ptr = (jlang.type.PointerType)type.getType();
		emitVLASize(ptr.getPointeeType());
		return null;
	}

	public Value emitScalarExpr(Tree.Expr expr)
	{
		assert expr != null && !hasAggregateBackendType(
				expr.getType()) : "Invalid scalar expression to emit";
		return new ScalarExprEmitter(this).visit(expr);
	}

	public void emitAggExpr(Tree.Expr expr, Value destPtr)
	{
		emitAggExpr(expr, destPtr, false, false);
	}

	/**
	 * emit the computation of the specified expression of aggregate
	 * type.  The result is computed into {@code destPtr}.
	 * <p>
	 * Note that if {@code destPtr} is null, the value of the aggregate
	 * expression is not needed.
	 *
	 * @param expr
	 * @param destPtr
	 */
	public void emitAggExpr(Tree.Expr expr, Value destPtr, boolean ignoreResult,
			boolean isInitializer)
	{
		assert expr != null && hasAggregateBackendType(expr.getType())
				: "Invalid aggregate expression to emit";
		if (destPtr == null)
			return;

		new AggExprEmitter(this, destPtr, ignoreResult, isInitializer)
				.visit(expr);
	}

	public void emitAggregateCopy(
			Value destPtr,
			Value srcPtr,
			QualType ty,
			boolean isVolatile)
	{
		// Aggregate assignment turns into backend.memcpy function calling.
		// struct {int i;}a, b;
		//
		// int main()
		// {
		//     a = b;     // convert to memcpy calling in LLVM IR.
		// }
		//
		Type bp = PointerType.getUnqual(Type.Int8Ty);
		if (destPtr.getType() != bp)
			destPtr = builder.createBitCast(destPtr, bp, "tmp");
		if (srcPtr.getType() != bp)
			srcPtr = builder.createBitCast(srcPtr, bp, "tmp");

		// Get the size and alignment info for this aggregate.
		Pair<Long, Integer> typeInfo = getContext().getTypeInfo(ty);

		// Handle variable sized types.
		Type intPtr = IntegerType.get(pointerWidth);

		// TODO we need to use a differnt call here.  We use isVolatile to
		// indicate when either the source or the destination is volatile.
		builder.createCall4(generator.getMemCpyFn(), destPtr, srcPtr,
				ConstantInt.get(intPtr, typeInfo.first/8),
				ConstantInt.get(Type.Int32Ty, typeInfo.second/8));
	}



	public void emitStoreOfScalar(Value val, Value addr
			/** boolean isVolatile*/, QualType ty)
	{
		if (ty.isBooleanType())
		{
			// Bool can have different representation in memory than in registers.
			Type srcTy = val.getType();
			PointerType destPtr = (PointerType) addr.getType();
			if (destPtr.getElementType() != srcTy)
			{
				Type memTy = PointerType.getUnqual(srcTy);
				addr = builder.createBitCast(addr, memTy, "storetmp");
			}
		}
		builder.createStore(val, addr);
	}

	public boolean containsLabel(Tree.Stmt s, boolean ignoreCaseStmts)
	{
		// Null statement, not a label.
		if (s == null)
			return false;

		// If this is a labelled statement, we want to emit code for it.
		// like this: if (0) {... foo: bar(); } goto foo;
		if (s instanceof LabelStmt)
			return true;

		// If this is a case/default statement, and we haven't seen a switch, we have
		// to emit the code.
		if (s instanceof Tree.SwitchCase && !ignoreCaseStmts)
			return true;

		// If this is a switch statement, we want to ignore cases below it.
		if (s instanceof Tree.SwitchStmt)
			ignoreCaseStmts = true;

		if (s instanceof Tree.CompoundStmt)
		{
			Tree.CompoundStmt cs = (Tree.CompoundStmt) s;
			for (Tree.Stmt sub : cs.getBody())
				if (containsLabel(sub, ignoreCaseStmts))
					return true;
		}
		return false;
	}

	/**
	 * Ensure the insert point has been defined as yet before emit IR.
	 */
	public void ensureInsertPoint()
	{
		if (!hasInsertPoint())
			emitBlock(createBasicBlock());
	}

	private boolean hasInsertPoint()
	{
		return builder.getInsertBlock() != null;
	}

	private void emitBlock(BasicBlock bb, boolean isFinished)
	{
		// fall out of the current block if necessary.
		emitBranch(bb);

		if (isFinished && bb.isUseEmpty())
			return;
		curFn.getBasicBlockList().add(bb);
		builder.setInsertPoint(bb);
	}

	public void emitBlock(BasicBlock bb)
	{
		emitBlock(bb, false);
	}

	public void emitBranch(BasicBlock targetBB)
	{
		// emit a branch instruction from the current block to the
		// TargetData block if this is a real one. If this is just a fall-through
		// block after a terminator, don't emit it.
		BasicBlock curBB = builder.getInsertBlock();

		if (curBB == null || curBB.getTerminator() != null)
		{
			// If there is no insert point or the previous block is already
			// terminated, don't touch it.
		}
		else
		{
			// Otherwise, create a fall-through branch.
			builder.createBr(targetBB);
		}
		builder.clearInsertPoint();
	}

	/**
	 * Emits code for static variable declared in block scope.
	 *
	 * @param vd
	 */
	private void emitStaticBlockVarDecl(VarDecl vd)
	{
		assert !localVarMaps.containsKey(vd) : "Decl already exists in localdeclmap!";

		GlobalVariable gv = createStaticBlockVarDecl(vd, ".");

		// Store into localVarMaps before generating initializer to handle
		// circular reference.
		localVarMaps.put(vd, gv);

		if (vd.getType().isVariablyModifiedType())
			emitVLASize(vd.getType());

		if (vd.hasInit())
		{
			Constant init = generator.emitConstantExpr(vd.getInit(), vd.getType(), this);

			// If constant emission failed, then this should be a C++ static
			// initializer.
			if (init == null)
			{
				errorUnsupported(vd.getInit(), "constant l-value expression");
			}
			else
			{
				if (gv.getType() != init.getType())
				{
					GlobalVariable oldGV = gv.clone();

					gv = new GlobalVariable(generator.getModule(),
							init.getType(), oldGV.isConstant(),
							LinkageType.InteralLinkage,
							init, "", null,vd.getType().getAddressSpace());

					gv.setName(oldGV.getName());
					// Replace all uses of the old global with the new global
					Constant newPtrForOldDecl = ConstantExpr
							.getBitCast(gv, oldGV.getType());
					oldGV.replaceAllUsesWith(newPtrForOldDecl);

					// Erase the old global, since it is no longer used.
					oldGV.eraseFromParent();
				}

				gv.setInitializer(init);
			}
		}

		Type lty = convertTypeForMem(vd.getType());
		Type ptrTy = PointerType.get(lty, vd.getType().getAddressSpace());
		localVarMaps.put(vd, ConstantExpr.getBitCast(gv, ptrTy));
	}

	private GlobalVariable createStaticBlockVarDecl(VarDecl vd,
			String separator)
	{
		QualType ty = vd.getType();
		assert ty.isConstantSizeType() : "VLAs cann't be static";

		String contextName = "";
		if (curFnDecl != null)
			contextName = curFnDecl.getNameAsString();
		else
			assert false : "Undefined context for block var decl";

		String name = contextName + separator + vd.getDeclName();
		Type lty = generator.getCodeGenTypes().convertTypeForMem(ty);
		return new GlobalVariable(generator.getModule(),
				lty, ty.isConstant(getContext()),
				LinkageType.InteralLinkage,
				generator.emitNullConstant(vd.getType()), name, null, 0);
	}

	private void emitGotoStmt(Tree.GotoStmt s)
	{
		// If this code is reachable then emit a stop point (if generating
		// debug info). We have to do this ourselves because we are on the
		// "simple" statement path.
		if (hasInsertPoint())
			emitStopPoint(s);
	}

	private void emitStopPoint(Tree.Stmt s)
	{

	}

	private void emitBreakStmt(Tree.BreakStmt s)
	{
		assert !breakContinueStack.isEmpty()
                : "break stmt not in a loop or switch!";
		BasicBlock breakBB = breakContinueStack.peek().breakBlock;
		emitBlock(breakBB);
	}

	/**
	 * Complete HIR generation for current function, then it is legal to call
	 * this function.
	 */
	public void finishFunction(SourceLocation endLoc)
	{
		assert breakContinueStack.isEmpty()
				:"mismatched push/pop in break/continue stack!";

		// emit function epilogue (to return).
		emitReturnBlock();

		emitFunctionEpilogue(curFnInfo, returnValue);

		// Remove the AllocaInstrPtr instruction, which is just a conversience for use.
		Instruction ptr = allocaInstPtr;
		allocaInstPtr = null;
		ptr.eraseFromParent();
	}

	private void emitReturnBlock()
	{
		BasicBlock curBB = builder.getInsertBlock();

		if (curBB != null)
		{
			assert curBB.getTerminator()==null:"Unexpected terminated block.";

			if (curBB.isEmpty() || returnBlock.isUseEmpty())
			{
				returnBlock.replaceAllUsesWith(curBB);
				// for GC.
				returnBlock = null;
			}
			else
			{
				emitBlock(returnBlock);
			}
			return;
		}
		// Otherwise, if the return block is the backend.target of a single direct
		// branch then we can just put the code in that block instead.
		if (returnBlock.hasOneUses())
		{
			Use use = returnBlock.useAt(0);
			BranchInst bi = null;
			if (use.getValue() instanceof BranchInst &&
					(bi = (BranchInst)use.getValue()).isUnconditional()
					&& bi.getSuccessor(0) == returnBlock)
			{
				// Reset the insertion point and delete the branch.
				builder.setInsertPoint(bi.getParent());
				bi.eraseFromParent();
				returnBlock = null;
			}
		}
	}

	private void emitFunctionEpilogue(CGFunctionInfo fnInfo, Value returnValue)
	{
		Value rv = null;

		// Functin with non result always return void.
		if (returnValue != null)
		{
			QualType retTy = fnInfo.getReturnType();
			ABIArgInfo retAI = fnInfo.getReturnInfo();

			switch(retAI.getKind())
			{
				case Ignore:
					break;
				case Direct:
                case Extend:
					// The internal return value temp always will have pointer
					// to return-type types.
					rv = builder.createLoad(returnValue, false, "ret");
				case Indirect:
                {
                    if (retTy.isComplexType())
                    {
                        // TODO complex type.
                    }
                    else if (hasAggregateBackendType(retTy))
                    {
                        emitAggregateCopy(curFn.getArgumentList().get(0),
                                returnValue, retTy, false);
                    }
                    else
                    {
                        emitStoreOfScalar(
                                builder.createLoad(returnValue, false, "ret"),
                                curFn.getArgumentList().get(0), false, retTy);
                    }
                }
                case Coerce:
                    rv = createCoercedLoad(returnValue, retAI.getCoerceType(), this);
                    break;
                case Expand:
                    assert false :"Expand abi can not used for return argument!";
                    break;
			}
		}

		if (rv != null)
			builder.createRet(rv);
		else
			builder.createRetVoid();
	}

    /**
     * Create a load from {@code srcPtr} which interpreted as a pointer to
     * an object of type {@code ty}.
     * <p>
     * This safely handles the case when the src type is smaller than destination
     * type; in this situation the values of bit which not present in the src and
     * undefined.
     * </p>
     * @param srcPtr
     * @param ty
     * @param cgf
     * @return
     */
    private static Value createCoercedLoad(
            Value srcPtr,
            Type ty,
            CodeGenFunction cgf)
    {
        Type srcTy = ((PointerType)srcPtr.getType()).getElementType();
        long srcSize = cgf.generator.getTargetData().getTypeAllocSize(srcTy);
        long destSize = cgf.generator.getTargetData().getTypeAllocSize(ty);

        if (srcSize >= destSize)
        {
            //
            Value casted = cgf.builder.createBitCast(srcPtr, PointerType.getUnqual(ty));
            Instruction.LoadInst load = cgf.builder.createLoad(casted);
            load.setAlignment(1);
            return load;
        }
        else
        {
            Value tmp = cgf.createTempAlloc(ty);
            Value casted = cgf.builder.createBitCast(tmp, PointerType.getUnqual(srcTy));
            Instruction.StoreInst store = cgf.builder.createStore(cgf.builder.createLoad(srcPtr), casted);
            store.setAlignment(1);
            return cgf.builder.createLoad(tmp);
        }
    }

    private void emitContinueStmt(Tree.ContinueStmt s)
	{
		assert !breakContinueStack
				.isEmpty() : "break stmt not in a loop or switch!";

		BasicBlock continueBB = breakContinueStack.peek().continueBlock;
		emitBlock(continueBB);
	}

	private void emitDefaultStmt(Tree.DefaultStmt s)
	{
		BasicBlock defaultBlock = switchInst.getDefaultBlock();
		assert defaultBlock.isEmpty()
                : "EmitDefaultStmt: Default block already defined?";

		emitBlock(defaultBlock);
		emitStmt(s.getSubStmt());
	}

	private void emitCaseStmt(Tree.CaseStmt s)
	{
		emitBlock(createBasicBlock("sw.bb"));
		BasicBlock caseDest = builder.getInsertBlock();

		APSInt caseVal = s.getCondExpr().evaluateAsInt(getContext());
		switchInst.addCase(ConstantInt.get(caseVal), caseDest);

		// Recursively emitting the statement is acceptable, but is not wonderful for
		// code where we have many case statements nested together, i.e.:
		//  case 1:
		//    case 2:
		//      case 3: etc.
		// Handling this recursively will create a new block for each case statement
		// that falls through to the next case which is IR intensive.  It also causes
		// deep recursion which can run into stack depth limitations.  Handle
		// sequential non-range case statements specially.
		CaseStmt curCase = s;
		CaseStmt nextCase = (CaseStmt) s.getSubStmt();
		while (nextCase != null)
		{
			curCase = nextCase;
			caseVal = curCase.getCondExpr().evaluateKnownConstInt();
			switchInst.addCase(ConstantInt.get(caseVal), caseDest);
			nextCase = (CaseStmt) curCase.getSubStmt();
		}

		// Normal default recursion for non-cases.
		emitStmt(curCase.getSubStmt());
	}

	private BasicBlock createBasicBlock(String name, Function parent)
	{
		return createBasicBlock(name, parent, null);
	}

	public BasicBlock createBasicBlock()
	{
		return createBasicBlock("", curFn, null);
	}

	public BasicBlock createBasicBlock(String name)
	{
		return createBasicBlock(name, curFn, null);
	}

	private BasicBlock createBasicBlock(String name, Function parent,
			BasicBlock before)
	{
		return BasicBlock.createBasicBlock(name, parent, before);
	}

	public LValue emitUnSupportedLValue(Expr expr, String msg)
	{
		errorUnsupported(expr, msg);
		Type ty = PointerType.getUnqual(convertType(expr.getType()));
		return LValue.makeAddr(Value.UndefValue.get(ty),
				expr.getType().getCVRQualifiers());
	}

	/**
	 * emit code to compute a designator that specifies the location of the
	 * expression.
	 * <p>This can return one of things: a simple address or a bitfield.
	 * </p>
	 * <p>
	 * <p>If this return a bitfield reference, nothing about the pointee type
	 * of the HIR value is known: for example, it may not be a pointer to an
	 * integer.
	 * </p>
	 * <p>
	 * <p>If this returns a normal address, and if the lvalue's C type
	 * is fixed getNumOfSubLoop, this method guarantees that the returned pointer type
	 * will point to an HIR type of the same getNumOfSubLoop of lvalue's type.
	 * </p>
	 *
	 * @param expr
	 * @return
	 */
	public LValue emitLValue(Expr expr)
	{
		switch (expr.getStmtClass())
		{
			default:
				return emitUnSupportedLValue(expr, "l-value expression");

			case BinaryOperatorClass:
				return emitBinaryOperatorLValue((BinaryExpr) expr);
			case DeclRefExprClass:
				return emitDeclRefLValue((DeclRefExpr) expr);
			case ParenExprClass:
				return emitLValue(((ParenExpr)expr).getSubExpr());
			case StringLiteralClass:
				return emitStringLiteralLValue((StringLiteral) expr);
			case UnaryOperatorClass:
				return emitUnaryOpLValue((UnaryExpr) expr);
			case ArraySubscriptExprClass:
				return emitArraySubscriptExpr((ArraySubscriptExpr) expr);
			case MemberExprClass:
				return emitMemberExpr((MemberExpr) expr);
			case CompoundLiteralExprClass:
				return emitCompoundLiteralLValue((CompoundLiteralExpr) expr);
		}
	}

	/**
	 * Creates a load instruction for loading a value from specified address.
	 * <p>A worthwhile noted point is that performing truncating to i1 when
	 * {@code ty} is boolean type while the type of given {@code addr} is not
	 * boolean.</p>
	 *
	 * @param addr
	 * @param isVolatile
	 * @param ty
	 * @return
	 */
	public Value emitLoadOfScalar(Value addr, boolean isVolatile, QualType ty)
	{
		Value v = builder.createLoad(addr, isVolatile, "tmp");
		// Bool can have different representation in memory than in registers.
		if (ty.isBooleanType())
			if (v.getType() != Type.Int1Ty)
				v = builder.createTrunc(v, Type.Int1Ty, "tobool");
		return v;
	}

	/**
	 * Emits an instruction to write data of type {@code ty} into specified address.
	 *
	 * @param value
	 * @param addr
	 * @param isVolatile
	 * @param ty
	 */
	public void emitStoreOfScalar(Value value, Value addr, boolean isVolatile,
			QualType ty)
	{
		if (ty.isBooleanType())
		{
			// Bool can have different representation in memory than in registers.
			final Type srcTy = value.getType();
			final PointerType desPtr = (PointerType) addr.getType();
			if (desPtr.getElementType() != srcTy)
			{
				Type memTy = PointerType.getUnqual(srcTy);
				addr = builder.createBitCast(addr, memTy, "storetmp");
			}
		}
		builder.createStore(value, addr);
	}

	/**
	 * Given an expression that represents a lvalue, this method emits the address
	 * of the lvalue, then loads like result as an rvalue, returning the rvalue.
	 *
	 * @param lv
	 * @param exprType
	 * @return
	 */
	public RValue emitLoadOfLValue(LValue lv, QualType exprType)
	{
		if (lv.isSimple())
		{
			Value ptr = lv.getAddress();
			Type eltType = ((PointerType) ptr.getType()).getElementType();

			// Simple scalar lvalue.
			if (eltType.isFirstClassType())
				return get(emitLoadOfScalar(ptr, lv.isVolatileQualified(),
								exprType));

			assert exprType.isFunctionType() : "Undefined scalar type.";
			return get(ptr);
		}

		assert lv.isBitField() : "Undefined LValue exprType.";
		return emitLoadOfBitfieldLValue(lv, exprType);
	}

	public RValue emitLoadOfBitfieldLValue(LValue lv, QualType exprType)
	{
		// TODO complete emitLoadOfBitfield();
		int startBit = lv.getBitfieldStartBits();
		int bitfieldSize = lv.getBitfieldSize();
		Value ptr = lv.getBitFieldAddr();

		Type eltType = ((PointerType) ptr.getType()).getElementType();
		int eltTySize = (int)generator.getTargetData().getTypeSizeInBits(eltType);

		int lowBits = Math.min(bitfieldSize, eltTySize - startBit);
		Value val = builder.createLoad(ptr, lv.isVolatileQualified(), "tmp");

		// Shift to proper location.
		if (startBit != 0)
			val = builder.createLShr(val, ConstantInt.get(eltType, startBit),
					"bf.to");

		// Mask off unused bits.
		Constant lowMask = ConstantInt
				.get(APInt.getLowBitsSet(eltTySize, lowBits));

		val = builder.createAnd(val, lowMask, "bf.to.cleared");

		// Fetch the high bits if necessary.
		if (lowBits < bitfieldSize)
		{
			int highBits = bitfieldSize - lowBits;
			// TODO
		}
		return null;
	}

	public RValue emitCallExpr(CallExpr expr)
	{
		Decl targetDecl = null;
		if (expr.getCallee() instanceof ImplicitCastExpr)
		{
			ImplicitCastExpr ce = (ImplicitCastExpr) expr.getCallee();
			if (ce.getSubExpr() instanceof DeclRefExpr)
			{
				targetDecl = ((DeclRefExpr) ce.getSubExpr()).getDecl();
			}
		}
		Value callee = emitScalarExpr(expr.getCallee());
		return emitCall(callee, expr.getCallee().getType(), expr.getArgs(),
				targetDecl);
	}

	public RValue emitCall(Value callee, QualType calleeType,
			ArrayList<Expr> args, Decl fnDecl)
	{
		// Get the actual function type. The callee type will always be a
		// pointer to function type or a block pointer type.
		assert calleeType.isFunctionPointerType()
                : "Call must have function pointer type!";

		QualType fnType = calleeType.getAsPointerType().getPointeeType();
		QualType resultType = fnType.getAsFunctionType().getResultType();

		ArrayList<Pair<RValue, QualType>> callArgs = new ArrayList<>();
		emitCallArgs(callArgs, fnType.getAsFunctionProtoType(), args);

		return emitCall(generator.getCodeGenTypes().
						getFunctionInfo3(resultType, callArgs),
                callee, callArgs, fnDecl);
	}

	/**
	 * Generate a call of the given function, expecting the given
	 * result type, and using the given argument list which
	 * specifies both the backend argument and types there wre derived from.
	 * @param callInfo
	 * @param callee
	 * @param callArgs
	 * @param targetDecl If given, the decl of the function in a direct call;
	 *                   used to set attributes on the call (noreturn, etc.).
	 * @return
	 */
	public RValue emitCall(
	        CGFunctionInfo callInfo,
            Value callee,
			ArrayList<Pair<RValue, QualType>> callArgs,
            Decl targetDecl)
	{
		// A list for holding argument type.
		LinkedList<Value> args = new LinkedList<>();

		QualType retType = callInfo.getReturnType();
		ABIArgInfo retAI = callInfo.getReturnInfo();

		// If the call returns a temporary with struct return, create a temporary
		// alloca to hold the result.
		if (generator.returnTypeUseSret(callInfo))
			args.add(createTempAlloc(convertTypeForMem(retType)));

		assert callInfo.getNumOfArgs() == callArgs.size()
                : "Mismatch between function signature & arguments.";
		int i = 0;
		for (Pair<RValue, QualType> ptr : callArgs)
		{
			ArgInfo infoItr = callInfo
					.getArgInfoAt(i);
			ABIArgInfo argInfo = infoItr.info;
			RValue rv = ptr.first;

			switch (argInfo.getKind())
            {
                case Indirect:
                {
                    if (rv.isScalar() || rv.isComplex())
                    {
                        // Make a temporary alloca to pass the argument.
                        args.add(createTempAlloc(convertTypeForMem(ptr.second)));
                        if (rv.isScalar())
                        {
                            emitStoreOfScalar(rv.getScalarVal(), args.getLast(),
                                    false, ptr.second);
                        }
                        else
                        {
                            // TODO complex type.
                        }
                    }
                    else
                    {
                        args.add(rv.getAggregateAddr());
                    }
                    break;
                }
                case Direct:
                case Extend:
                {
                    if (rv.isScalar())
                        args.add(rv.getScalarVal());
                    else if (rv.isComplex())
                    {
                        //
                    }
                    else
                    {
                        args.add(builder.createLoad(rv.getAggregateAddr(), false,
                                "load"));
                    }
                    break;
                }
                case Ignore:
                    break;
                case Coerce:
                {
                    Value srcPtr = null;
                    if (rv.isScalar())
                    {
                        srcPtr = createTempAlloc(convertTypeForMem(ptr.second), "coerce");
                        emitStoreOfScalar(rv.getScalarVal(), srcPtr, false, ptr.second);;
                    }
                    else if (rv.isComplex())
                    {
                        assert false:"Current complex type is not supported!";
                    }
                    else
                    {
                        srcPtr = rv.getAggregateAddr();
                    }
                    args.addLast(createCoercedLoad(srcPtr, argInfo.getCoerceType(), this));
                    break;
                }
                case Expand:
                {
                    expandTypeToArgs(ptr.second, rv, args);
                    break;
                }
			}
		}

		CallSite cs = new CallSite(builder.createCall(callee, args));

		Instruction.CallInst ci = cs.getInstruction();
		if (ci.getType() != Type.VoidTy)
			ci.setName("call");

		switch (retAI.getKind())
		{
			case Indirect:
				if (retType.isComplexType())
				{
					// TODO complex type.
					return null;
				}
				if (hasAggregateBackendType(retType))
				{
					// Handles return value as aggregate type.
					return getAggregate(args.get(0));
				}
				return get(emitLoadOfScalar(args.get(0), false, retType));
			case Direct:
            case Extend:
				if (retType.isComplexType())
				{
					// TODO complex type.
					return null;
				}
				if (hasAggregateBackendType(retType))
				{
					// emit the returned value of type aggregate type.
					Value v = createTempAlloc(convertTypeForMem(retType),
							"agg.tmp");
					builder.createStore(ci, v);
					return getAggregate(v);
				}
				return get(ci);
			case Ignore:
				// If we are ignoreing an argument that haa d result, make sure
				// to construct a appropriate return value for caller.
				return getUndefRValue(retType);
            case Coerce:
            {
                Value v = createTempAlloc(convertTypeForMem(retType), "coerce");
                createCoercedStore(ci, v, this);
                if (retType.isAnyComplexType())
                {
                    assert false:"Complex type is unsupported!";
                }
                if (hasAggregateBackendType(retType))
                {
                    return RValue.getAggregate(v);
                }
                return RValue.get(emitLoadOfScalar(v, false, retType));
            }
            case Expand:
                assert false:"Expand ABI is not allowed for return argument";
		}

		assert false : "Unhandled ABIArgInfo.Kind";
		return get(null);
	}

    private void expandTypeToArgs(
            QualType type,
            RValue rv,
            LinkedList<Value> args)
    {
        RecordType rt = type.getAsStructureType();
        assert rt != null :"Can only expand struture types!";

        RecordDecl rd = rt.getDecl();
        assert rv.isAggregate() :"Unexpected rvalue during struct expansion";
        Value addr = rv.getAggregateAddr();
        for (int i = 0, e = rd.getNumFields(); i < e; i++)
        {
            FieldDecl fd = rd.getDeclAt(i);
            QualType ft = fd.getType();

            LValue lv = emitLValueForField(addr, fd, false, 0);
            if (hasAggregateBackendType(ft))
            {
                expandTypeToArgs(ft, getAggregate(lv.getAddress()), args);
            }
            else
            {
                RValue rvalue = emitLoadOfLValue(lv, ft);
                assert rvalue.isScalar()
                        :"Unexpected non-scalar rvlaue during struct expansion";
                args.addLast(rv.getScalarVal());
            }
        }
    }

    private RValue getUndefRValue(QualType type)
	{
		ComplexType cty = null;
		if (type.isVoidType())
			return get(null);
		else if ((cty = type.getAsComplexType()) != null)
		{
			Type eltType = convertType(cty.getElementType());
			Value u = Value.UndefValue.get(eltType);
			return getComplex(u, u);
		}
		else if (hasAggregateBackendType(type))
		{
			Type lty = PointerType.getUnqual(convertType(type));
			return getAggregate(Value.UndefValue.get(lty));
		}
		else
		{
			return get(Value.UndefValue.get(convertType(type)));
		}
	}

	/**
	 * emit function calling arguments.
	 *
	 * @param callArgs
	 * @param fnType   The function type of called function definition is used for
	 *                 iterating over known argument types of the function being called.
	 * @param args     The passed in real argument list.
	 */
	public void emitCallArgs(
	        ArrayList<Pair<RValue, QualType>> callArgs,
			jlang.type.FunctionProtoType fnType,
            ArrayList<Expr> args)
	{
		int idx = 0;

		if (fnType != null)
		{
			for (int i = 0, size = fnType.getNumArgs(); i < size; i++)
			{
				QualType argType = fnType.getArgType(i);
				Expr arg = args.get(idx);
				assert getContext().getCanonicalType(argType) == getContext().getCanonicalType(arg
						.getType()) : "Type mismatch in call argument!";

				callArgs.add(new Pair<>(emitCallExpr(arg), argType));
				idx++;
			}

			assert idx == args.size() || fnType
					.isVariadic() : "Extra arguments in non-varidadic function!";
		}

		// If we still have any arguments, emit them using the type of the argument.
		for (int size = fnType.getNumArgs(); idx < size; idx++)
		{
			Expr arg = args.get(idx);
			QualType argType = arg.getType();
			callArgs.add(new Pair<>(emitCallExpr(arg), argType));
		}
	}

	public RValue emitCallExpr(Expr arg)
	{
		return emitAnyExprToTemp(arg);
	}

	public LValue emitBinaryOperatorLValue(BinaryExpr expr)
	{
		// Comma expression just emit their lhs and then rhs as l-value.
		if (expr.getOpcode() == BinaryOperatorKind.BO_Comma)
		{
			emitAnyExpr(expr.getLHS());
			return emitLValue(expr.getRHS());
		}

		// Only assignment expression can be l-value.
		if (expr.getOpcode() != BinaryOperatorKind.BO_Assign)
			return emitUnSupportedLValue(expr, "binary l-value");

		Value temp = createTempAlloc(convertType(expr.getType()));
		emitAggExpr(expr, temp, false, false);
		return LValue.makeAddr(temp, expr.getType().getCVRQualifiers());
	}

	public LValue emitDeclRefLValue(DeclRefExpr expr)
	{
		NamedDecl decl = expr.getDecl();
		VarDecl vd = decl instanceof VarDecl ? (VarDecl)decl : null;
		if (vd!=null && (vd.isBlockVarDecl() || vd instanceof ParamVarDecl))
		{
			LValue lv = new LValue();
			if (vd.hasExternalStorage())
			{
				// extern qualified or implicit extern.
				Value v = generator.getAddrOfGlobalVar(vd);
				lv = LValue.makeAddr(v, expr.getType().getCVRQualifiers());
			}
			else
			{
				Value v = localVarMaps.get(vd);
				assert v !=null:"local variable is not enterred localVarMpas?";
				// local static.
				lv = LValue.makeAddr(v, expr.getType().getCVRQualifiers());
			}
			return lv;
		}
		else if (vd != null && vd.isFileVarDecl())
		{
			Value v = generator.getAddrOfGlobalVar(vd);
			LValue lv = LValue.makeAddr(v, expr.getType().getCVRQualifiers());
			return lv;
		}
		else if (expr.getDecl() instanceof FunctionDecl)
		{
			FunctionDecl fn = (FunctionDecl)expr.getDecl();
			Value v = generator.getAddrOfFunction(fn);
			return LValue.makeAddr(v, expr.getType().getCVRQualifiers());
		}
		assert false:"Illegal DeclRefExpr.";
		return new LValue();
	}

	public LValue emitStringLiteralLValue(StringLiteral expr)
	{
		return LValue.makeAddr(generator.getAddrOfConstantStringFromLiteral(expr), 0);
	}

	public LValue emitUnaryOpLValue(UnaryExpr expr)
	{
		QualType exprType = getContext().getCanonicalType(expr.getType());
		switch (expr.getOpCode())
		{
			default:
				assert false:"Undefined unary operator lvalue!";
			case UO_Deref:
			{
				QualType t = expr.getSubExpr().getType().getPointeeType();
				assert !t.isNull():"CodeGenFunction.emitUnaryOpLValue: illegal type";

				return LValue.makeAddr(emitScalarExpr(expr.getSubExpr()),
						expr.getType().getCVRQualifiers());
			}
		}
	}

	public LValue emitArraySubscriptExpr(ArraySubscriptExpr expr)
	{
		// The index must always be an integer
		Value idx = emitScalarExpr(expr.getIdx());
		QualType idxType = expr.getIdx().getType();
		boolean idxSigned = idxType.isSignedIntegerType();

		// The base must be a pointer, which is not an aggregate.  emit it.
		Value base = emitScalarExpr(expr.getBase());

		// Extend or truncate the index type to 32 or 64 bits.
		int idxBitwidth = ((IntegerType)idx.getType()).getBitWidth();
		if (idxBitwidth < pointerWidth)
		{
			if (idxSigned)
				idx = builder.createSExt(idx, IntegerType.get(pointerWidth), "sext");
			else
				idx = builder.createZExt(idx, IntegerType.get(pointerWidth), "zext");
		}
		else if (idxBitwidth > pointerWidth)
		{
			idx = builder.createTrunc(idx, IntegerType.get(pointerWidth), "trunc");
		}

		// We know that the pointer points to a type of the correct getNumOfSubLoop,
		// unless the getNumOfSubLoop is a VLA
		ArrayType.VariableArrayType vat = getContext().getAsVariableArrayType(expr.getType());
		Value address;
		if (vat != null)
		{
			// TODO.
			Value vlaSize = getVLASize(vat);
			idx = builder.createMul(idx, vlaSize, "idxmul");
			QualType baseType = getContext().getBaseElementType(vat);

			long baseTypeSize = getContext().getTypeSize(baseType) / 8;
			idx = builder.createUDiv(idx, ConstantInt.get(idx.getType(), baseTypeSize), "udiv");

			address = builder.createInBoundsGEP(base, idx, "arrayidx");
		}
		else
		{
			address = builder.createInBoundsGEP(base, idx, "arrayidx");
		}

		QualType t = expr.getBase().getType().getPointeeType();
		assert !t.isNull():"CodeGenFunction.emitArraySubscriptExpr():Illegal base type";

		LValue lv = LValue.makeAddr(address, t.getCVRQualifiers());
		return lv;
	}

	private Value getVLASize(ArrayType.VariableArrayType vat)
	{
		Value size = vlaSizeMap.get(vat.getSizeExpr());
		assert size!= null:"Did not emti getNumOfSubLoop of type";
		return size;
	}

	public LValue emitMemberExpr(MemberExpr expr)
	{
		boolean isUnion = false;
		Expr baseExpr = expr.getBase();
		Value baseValue = null;
		int cvrQualifiers = 0;
		// If this is s.x, emit s as an lvalue.  If it is s->x, emit s as a scalar.
		if (expr.isArrow())
		{
			baseValue = emitScalarExpr(baseExpr);
			jlang.type.PointerType pty = baseExpr.getType().getAsPointerType();
			if (pty.getPointeeType().isUnionType())
				isUnion = true;
			cvrQualifiers = pty.getPointeeType().getCVRQualifiers();
		}
		else
		{
			LValue baseLV = emitLValue(baseExpr);
			baseValue = baseLV.getAddress();
			QualType baseType = baseExpr.getType();
			if (baseType.isUnionType())
				isUnion = true;
			cvrQualifiers = baseType.getCVRQualifiers();
		}

		assert expr.getMemberDecl() instanceof FieldDecl
				:"No code generation for non-field member expression";
		FieldDecl field = (FieldDecl) expr.getMemberDecl();
		LValue memExprLV = emitLValueForField(baseValue, field, isUnion, cvrQualifiers);
		return memExprLV;
	}

	public LValue emitLValueForField(Value baseValue,
			FieldDecl field,
			boolean isUnion,
			int cvrQualifiers
			)
	{
		if (field.isBitField())
			return emitLValueForBitField(baseValue, field, cvrQualifiers);

		int idx = generator.getCodeGenTypes().getFieldNo(field);
		Value v = builder.createStructGEP(baseValue, idx, "tmp");

		// Match union field type.
		if (isUnion)
		{
			Type fieldType = generator.getCodeGenTypes().
					convertTypeForMem(field.getType());

			PointerType baseType = (PointerType)baseValue.getType();
			v = builder.createBitCast(v, PointerType.getUnqual(fieldType),
					"tmp");
		}

		LValue lv = LValue.makeAddr(v,
				field.getType().getCVRQualifiers()
				| cvrQualifiers);
		return lv;
	}

	public LValue emitLValueForBitField(Value baseValue,
			FieldDecl field,
			int cvrQualifiers)
	{
		CodeGenTypes.BitFieldInfo info = generator.getCodeGenTypes().getBitFieldInfo(field);

		Type fieldType = generator.getCodeGenTypes().convertTypeForMem(field.getType());
		PointerType baseType = (PointerType) baseValue.getType();
		baseValue = builder.createBitCast(baseValue, PointerType.getUnqual(fieldType), "tmp");

		Value idx = ConstantInt.get(Type.Int32Ty, info.fieldNo);
		Value v = builder.createGEP(baseValue, idx, "tmp");

		return LValue.makeBitfield(v, info.start, info.size,
				field.getType().isSignedIntegerType(),
				field.getType().getCVRQualifiers() | cvrQualifiers);
	}

	public LValue emitCompoundLiteralLValue(CompoundLiteralExpr expr)
	{
		Type ty = convertType(expr.getType());
		Value declPtr = createTempAlloc(ty, ".compoundliteral");

		Expr initExpr = expr.getInitializer();

		LValue result = LValue.makeAddr(declPtr, expr.getType().getCVRQualifiers());
		if (expr.getType().isComplexType())
		{
			// TODO complex type.
		}
		else if (hasAggregateBackendType(expr.getType()))
			emitAnyExpr(initExpr, declPtr, false, false,false);
		else
			emitStoreThroughLValue(emitAnyExpr(initExpr), result, expr.getType());;

		return result;
	}

	/**
	 * Similar to {@linkplain #emitAnyExpr(Expr)}
	 *
	 * @param expr
	 * @return
	 */
	public RValue emitAnyExprToTemp(Expr expr)
	{
		return emitAnyExprToTemp(expr, false, false);
	}

	public RValue emitAnyExprToTemp(Expr expr, boolean isAggLocVolatile,
			boolean isInitializer)
	{
		Value aggLoc = null;
		if (hasAggregateBackendType(expr.getType()) && !expr.getType()
				.isComplexType())
		{
			aggLoc = createTempAlloc(convertType(expr.getType()));
		}
		return emitAnyExpr(expr, aggLoc, isAggLocVolatile,
	            /*ignore result*/false, isInitializer);
	}

	public void errorUnsupported(Stmt s, String msg)
	{
		generator.errorUnsupported(s, msg);
	}

	/**
	 * Store the specified RValue into the specified LValue.
	 * Also, it is guaranteed that both have same type is {@code type}.
	 */
	public void emitStoreThroughLValue(RValue src, LValue dest, QualType type)
	{
		if (dest.isBitField())
		{
			// todo emit store for bitfield.
		}

		assert src.isScalar() : "Can't emit an agg store with this method!";
		emitStoreOfScalar(src.getScalarVal(), dest.getAddress(), type);
	}

	public void emitMemSetToZero(Value address, QualType ty)
	{
		Type bp = PointerType.getUnqual(Type.Int8Ty);
		if (address.getType() != bp)
			address = builder.createBitCast(address, bp, "bitcast.tmp");

		// Get the getNumOfSubLoop and alignment info for this aggregate.
		Pair<Long, Integer> typeInfo = getContext().getTypeInfo(ty);

		// don't emit code for zero getNumOfSubLoop type.
		if (typeInfo.first == 0)
			return;

		// handle variable sized type.
		Type intPtr = IntegerType.get(pointerWidth);

		builder.createCall4(generator.getMemSetFn(),
				address,
				Constant.getNullValue(Type.Int8Ty),
				ConstantInt.get(intPtr, typeInfo.first/8),
				ConstantInt.get(Type.Int32Ty, typeInfo.second/8),
				"intrinsic.memset");
	}
}
