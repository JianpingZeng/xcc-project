package frontend.codegen;
/*
 * Xlous C language Compiler
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

import backend.hir.BasicBlock;
import backend.hir.HIRBuilder;
import backend.hir.JumpDest;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.BranchInst;
import frontend.ast.Tree;
import frontend.sema.APSInt;
import frontend.sema.BinaryOperatorKind;
import frontend.sema.Decl;
import frontend.sema.Decl.FunctionDecl;
import frontend.sema.Decl.VarDecl;
import frontend.sema.UnaryOperatorKind;
import frontend.type.ArrayType;
import frontend.type.QualType;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import static frontend.ast.Tree.*;

/**
 * This class responsible for generating HIR code.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class CodeGenFunction
{
    private HIRGenModule generator;
    private FunctionDecl curFnDecl;
    private Function curFn;
    private QualType fnRetTy;
    private int id;

    /**
     * Unified return block.
     */
    private JumpDest returnBlock;

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
    private HashMap<Tree.LabelledStmt, BasicBlock> labelMap;

    private HashMap<Tree.Expr, Type> vlaSizeMap;

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

    public CodeGenFunction(HIRGenModule generator)
    {
        this.generator = generator;
        builder = new HIRBuilder();
        localVarMaps = new HashMap<>();
        labelMap = new HashMap<>();
        vlaSizeMap = new HashMap<>();
        breakContinueStack = new Stack<>();
    }

    public void generateCode(FunctionDecl fd, Function fn)
    {
        QualType resTy = fd.getReturnType();

        ArrayList<VarDecl> functionArgList = new ArrayList<>(16);
        if (fd.getNumParams()>0)
        {
            for (int i= 0, e = fd.getNumParams(); i < e; i++)
                functionArgList.add(fd.getParamDecl(i));
        }

        // Emit the standard function prologue.
        startFunction(fd, resTy, fn, functionArgList);
        // Generates code for function body.
        emitFunctionBody();

        // emit standard function eliplogue.
        finishFunction();
    }

    private void startFunction(FunctionDecl fd, QualType resTy,
            Function fn,
            ArrayList<VarDecl> args)
    {
        curFn = fn;
        curFnDecl = fd;
        fnRetTy = resTy;

        assert fn.isDeclaration():"Function already has body.";

        BasicBlock entryBB = createBasicBlock("entry", curFn);

        // Create a marker to make it easy to insert allocas into the entryblock
        // later.  Don't create this with the builder, because we don't want it
        // folded.
        Value undef = Value.UndefValue.get(Type.Int32Ty);

        returnBlock = getJumpDestInCurrentScope("return");
        builder.setInsertPoint(entryBB);

        if (resTy.isVoidType())
            returnValue = null;
        else
        {
            returnValue = createIRTemp(resTy, "retval");
        }

        emitFunctionPrologue(curFn, curFn.getType(), args);
        // If any of the arguments have a variably modified type,
        // make sure to emit type size.
        for (VarDecl vd : args)
        {
            QualType ty = vd.getDeclType();
            // TODO handle variable size type introduced in C99.
        }
    }

    /**
     * Emits standard prologue code for function definition.
     * @param fn
     * @param args
     */
    private void emitFunctionPrologue(Function fn,
            FunctionType fnType,
            ArrayList<VarDecl> args)
    {
        if (curFnDecl.hasImplicitReturnZero())
        {
            QualType retType = curFnDecl.getReturnType().getUnQualifiedType();
            Type backendTy = generator.getCodeGenTypes().convertType(retType);
            Constant zero = Constant.getNullValue(backendTy);
            builder.createStore(zero, returnValue);
        }

        assert fn.getNumOfArgs() == args.size()
                :"Mismatch between function signature and argumens";

        // walking through variable declaration.
        int argNo = 1;
        // Emit allocs for param decls.  Give the HIR argument nodes names.
        Iterator<Argument> argItr = fn.getArgumentList().iterator();

        // obtains the type list of formal type enclosing in FunctionType.
        Iterator<CodeGenTypes.ArgTypeInfo> infoItr = fnType.getParamTypes().iterator();
        for (VarDecl vd : args)
        {
            QualType ty = vd.getDeclType();
            Value v = argItr.next();
            final CodeGenTypes.ArgTypeInfo ArgInfo = infoItr.next();

            assert !argItr.hasNext():"Argument mismatch!";

            // struct/union, array type.
            if (hasAggregateBackendType(ty))
            {
                // Create a temporary alloca to hold the argument; the rest of
                // codegen expects to access aggregates & complex values by
                // reference.
                Value ptr = createTempAlloc(convertTypeForMem(ty));
                builder.createStore(v, ptr);
                v = ptr;
            }
            else
            {
                // if argument type is compatible with parameter type.
                // issue conversion instruction.
                // v = emitScalarConversion(v, argument type, formal type);
                if (!ty.isCompatible(ArgInfo.frontendType))
                {
                    // This must be a promotion, for something like
                    // "void a(x) short x; {..."
                    v = emitScalarConversion(v, ty, ArgInfo.frontendType);
                }
            }
            emitParamDecl(vd, v);
        }
        assert !argItr.hasNext():"Argument mismatch!";
    }

    /**
     * Emits conversion instruction from the specified frontend type to another frotnend tyep.
     *
     * Both type must be no Aggregate type.
     * @param v
     * @param srcTy
     * @param destTy
     * @return
     */
    private Value emitScalarConversion(Value v, QualType srcTy, QualType destTy)
    {
        assert !hasAggregateBackendType(srcTy) && !hasAggregateBackendType(destTy)
                :"Invalid scalar expression to emit!";
        return new ScalarExprEmitter(this).emitScalarConversion(v, srcTy, destTy);
    }

    private void emitParamDecl(VarDecl param, Value v)
    {

    }

    private backend.type.Type convertTypeForMem(QualType ty)
    {
        return generator.getCodeGenTypes().convertTypeForMem(ty);
    }

    private boolean hasAggregateBackendType(QualType ty)
    {
        return !ty.isPointerType() && !ty.isRealType()
                && !ty.isVoidType() && !ty.isFunctionType();
    }

    private Value createIRTemp(QualType ty, String name)
    {
        Instruction.AllocaInst alloc = createTempAlloc(convertType(ty), name);
        return alloc;
    }

    /**
     * This creates a alloca and inserts it into the entry block.
     * @param ty
     * @param name
     * @return
     */
    private Instruction.AllocaInst createTempAlloc(Type ty, String name)
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

    private JumpDest getJumpDestInCurrentScope(String name)
    {
        return getJumpDestInCurrentScope(createBasicBlock(name));
    }

    private JumpDest getJumpDestInCurrentScope(BasicBlock target)
    {
        return new JumpDest(target, nextCleanupDestIndex++);
    }

    /**
     * Emits code for the function body through visiting CompoundStmt of function.
     */
    private void emitFunctionBody()
    {
        assert curFnDecl.hasBody()
                :"Can not emit stmt code for function with no body.";
        emitStmt(curFnDecl.getBody());
    }

    private void emitStmt(Tree.Stmt stmt)
    {
        assert stmt!=null:"Null Statement!";

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
                assert !(stmt instanceof DeclStmt): "Unexpected DeclStmt!";
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
                emitIfStmt((IfStmt)stmt);
                break;
            case WhileStmtClass:
                emitWhileStmt((WhileStmt) stmt);
                break;
            case DoStmtClass:
                emitDoStmt((DoStmt) stmt);
                break;
            case ForStmtClass:
                emitForStmt((ForStmt)stmt);
                break;
            case ReturnStmtClass:
                emitReturnStmt((ReturnStmt)stmt);
                break;
            case SwitchStmtClass:
                emitSwitchStmt((SwitchStmt)stmt);
                break;
        }
    }

    /**
     * C99 6.8.4.1: The first substatement is executed if the expression compares
     * unequal to 0.  The condition must be a scalar type.
     * @param s
     */
    private void emitIfStmt(IfStmt s)
    {
        // If the condition constant folds and can be elided, try to avoid emitting
        // the condition and the dead arm of the if/else.
        int cond = constantFoldsToSimpleInteger(s.getCond());
        if (cond!=0)
        {
            // Figure out which block (then or else) is executed.
            final Stmt executed = s.getThenPart(), skipped = s.getElsePart();
            if (cond == -1) // condition is false.
                Util.swap(executed, skipped);

            // if skipped block has no labels within it, just emit code
            // for executed stmt. This avoids emitting dead code.
            if (!containsLabel(skipped, false))
            {
                if (executed!=null)
                    emitStmt(executed);
                return;
            }
        }

        // Otherwise, the condition can not folded.
        BasicBlock thenBB = createBasicBlock("if.then");
        BasicBlock endBB = createBasicBlock("if.end");
        BasicBlock elseBB = endBB;
        if (s.getElsePart() != null)
            elseBB = createBasicBlock("if.else");

        emitBranchOnBoolExpr(s.getCond(), thenBB, elseBB);

        // Emit the 'then' code.
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

        // Emit the continuation block for code after the if stmt.
        emitBlock(endBB, true);
    }

    /**
     * Emit a branch instruction on the specified condition is evaluated as boolean.
     * When condition is false, branch to {@code trueBB}, othewise to {@code falseBB}.
     * <p>Based on condition, it might be taken to simply the codegen of the branch.
     * </p>
     * @param cond
     * @param trueBB
     * @param falseBB
     */
    private void emitBranchOnBoolExpr(Expr cond, BasicBlock trueBB, BasicBlock falseBB)
    {
        if (cond instanceof ParenExpr)
            cond = ((ParenExpr) cond).getSubExpr();

        if (cond instanceof BinaryExpr)
        {
            BinaryExpr condBOp = (BinaryExpr)cond;
            // Handle x&&y in a condition.
            if (condBOp.getOpcode() == BinaryOperatorKind.BO_LAnd)
            {
                // If we have "1 && X", simplify the code.  "0 && X" would have constant
                // folded if the case was simple enough.
                if (constantFoldsToSimpleInteger(condBOp.getLHS()) == 1)
                {
                    // br (1&&x) -> br x.
                    emitBranchOnBoolExpr(condBOp.getRHS(), trueBB, falseBB);;
                    return;
                }

                // If we have "X && 1", simplify the code to use an uncond branch.
                // "X && 0" would have been constant folded to 0.
                if (constantFoldsToSimpleInteger(condBOp.getRHS()) == 1)
                {
                    // br (x&&1) -> br x.
                    emitBranchOnBoolExpr(condBOp.getLHS(), trueBB, falseBB);;
                    return;
                }

                // Emit the LHS as a conditional.  If the LHS conditional is false, we
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
                    emitBranchOnBoolExpr(condBOp.getRHS(), trueBB, falseBB);;
                    return;
                }

                if (constantFoldsToSimpleInteger(condBOp.getRHS()) == -1)
                {
                    // br (X||0) -> br (x).
                    emitBranchOnBoolExpr(condBOp.getLHS(), trueBB, falseBB);;
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
            UnaryExpr condUOp = (UnaryExpr)cond;
            // br(!x, t, f) -> br(x, f, t).
            if (condUOp.getOpCode() == UnaryOperatorKind.UO_LNot)
            {
                emitBranchOnBoolExpr(condUOp.getSubExpr(), trueBB, falseBB);
                return;
            }
        }

        if (cond instanceof ConditionalExpr)
        {
            ConditionalExpr condOp = (ConditionalExpr)cond;
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
     * @param cond
     * @return
     */
    private Value evaluateExprAsBool(Expr cond)
    {
        QualType boolTy = frontend.type.Type.BoolTy;
        assert !cond.getType().isComplexType()
                :"Current complex type not be supported.";

        return emitScalarConversion(emitScalarExpr(cond), cond.getType(), boolTy);
    }

    /**
     * Emits code for while stmt.
     * @param s
     */
    private void emitWhileStmt(WhileStmt s)
    {
        // create a basic block for condition when continue encountered in while.
        // Emit the header for the loop, insert it, which will create an uncond br to
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
            ConstantInt c= (ConstantInt)condVal;
            if (c.isOne())
                emitBoolCondBranch = false;
        }

        // As long as the condition is true, go to the loop body.
        if (emitBoolCondBranch)
            builder.createCondBr(condVal, loopBody, exitBB);

        // Emit the loop body.
        emitBlock(loopBody);
        emitStmt(s.getBody());

        breakContinueStack.pop();

        // goto the condition.
        emitBlock(condBB);

        // Emit the exit block.
        emitBlock(exitBB, true);

        // the loop header typically is just a branch if we skipping emitting
        // a branch, try to erase it.
        if (!emitBoolCondBranch)
            simplifyForwardingBlocks(condBB);
    }

    private void simplifyForwardingBlocks(BasicBlock bb)
    {
        BranchInst inst = bb.getTerminator();

        // Can only simplify direct branch.
        if (inst ==null || !inst.isUnconditional())
            return;

        bb.replaceAllUsesWith(inst.suxAt(0));
        inst.eraseFromBasicBlock();
        bb.eraseFromParent();
    }

    private void emitDoStmt(DoStmt s)
    {
        // Emit the body of the loop, insert it into blocks list.
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
            ConstantInt c = (ConstantInt)boolCondVal;
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

        // Emit the loop exit block.
        emitBlock(forEnd, true);
    }

    private void emitReturnStmt(ReturnStmt s)
    {
        // Emit the result value, even if unused, to evalute the side effects.
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
        }
        else
        {
            emitAggExpr(resultVal, returnValue);
        }
    }

    private RValue emitAnyExpr(Expr e)
    {
        return emitAnyExpr(e, null, false, false, false);
    }

    /**
     * Emit code to compute the specified expression with any type.
     * @param e
     */
    private RValue emitAnyExpr(Expr e,
            Value aggLoc,
            boolean isAggLocVolatile,
            boolean ignoreResult,
            boolean isInitializer)
    {
        if (!hasAggregateBackendType(e.getType()))
            return RValue.get(emitScalarExpr(e));
        else if (e.getType().isComplexType())
        {
            // TODO.
        }
        emitAggExpr(e, aggLoc, ignoreResult, isInitializer);
        return RValue.getAggregate(aggLoc, isAggLocVolatile);
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
     * @param expr
     * @return
     */
    private int constantFoldsToSimpleInteger(Expr expr)
    {
        Expr.EvalResult result = new Expr.EvalResult();
        if (!expr.evaluate(result) || !result.getValue().isInt()
                || result.hasSideEffects())
            return 0;   // can not foldable not integral or not fully evaluatable.

        if (containsLabel(expr, false))
            return 0;

        return result.getValue().getInt().getBoolValue() ? 1 : -1;
    }

    private boolean emitSimpleStmt(Tree.Stmt s)
    {
        switch (s.getStmtClass())
        {
            default: return false;
            case NullStmtClass:
            case CompoundStmtClass:
                emitCompoundStmt((Tree.CompoundStmt)s);
                return true;
            case DeclStmtClass:
                emitDeclStmt((Tree.DeclStmt)s);
                return true;
            case LabelledStmtClass:
                emitLabelStmt((Tree.LabelledStmt)s);
                return true;
            case GotoStmtClass:
                emitGotoStmt((Tree.GotoStmt)s);
                return true;
            case BreakStmtClass:
                emitBreakStmt((Tree.BreakStmt)s);
                return true;
            case ContinueStmtClass:
                emitContinueStmt((Tree.ContinueStmt)s);
                return true;
            case DefaultStmtClass:
                emitDefaultStmt((Tree.DefaultStmt)s);
                return true;
            case CaseStmtClass:
                emitCaseStmt((Tree.CaseStmt)s);
                return true;
        }
    }

    private void emitLabelStmt(Tree.LabelledStmt stmt)
    {
        emitLabel(stmt);
        emitStmt(stmt.body);
    }

    private void emitLabel(Tree.LabelledStmt s)
    {
        emitBlock(getBasicBlockForLabel(s));
    }

    private BasicBlock getBasicBlockForLabel(Tree.LabelledStmt s)
    {
        BasicBlock BB = labelMap.get(s);
        if (BB != null) return BB;

        BB = createBasicBlock(s.getName());
        labelMap.put(s, BB);
        return BB;
    }

    /**
     * Emits code for the compound statement, like {...}.
     * @param s
     */
    private void emitCompoundStmt(Tree.CompoundStmt s)
    {
        for (Iterator<Tree.Stmt> itr = s.iterator(); itr.hasNext();)
        {
            emitStmt(itr.next());
        }
    }

    private void emitDeclStmt(Tree.DeclStmt s)
    {
        for (Iterator<Decl> itr = s.iterator(); itr.hasNext();)
        {
            emitDecl(itr.next());
        }
    }

    private void emitDecl(Decl decl)
    {
        switch (decl.getDeclKind())
        {
            default:assert false:"Unknown decl type.";
                break;
            case ParamVar:
                assert false:"ParamDecls should not be handled in emitDecl().";
                break;
            case FunctionDecl: // void foo();
            case StructDecl:   // struct/union X;
            case EnumDecl:     // enum X;
            case EnumConstant: // enum ? {X =?,};
                // none of those decls required codegen support.
                return;
            case VarDecl:
            {
                VarDecl vd = (VarDecl)decl;
                assert vd.isBlockVarDecl()
                        :"Should not see file-scope variable declaration.";
                emitBlockVarDecl(vd);
                return;
            }
            case TypedefDecl: // typedef int x;
            {
                Decl.TypeDefDecl tf = (Decl.TypeDefDecl)decl;
                QualType ty = tf.getUnderlyingType();

                // TODO handle variable modified type, 2016.11.3.
                emitVLASize(ty);
                return;
            }
        }
    }

    /**
     * This method handles any variable function inside a function.
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
        assert false:"Unknown storage class.";
    }

    /**
     * <p>Emits code and set up an entry in LocalDeclMap for a variable declaration
     * with auto, register, or on storage class specifier.
     * </p>
     * <p>These turn into simple stack objects, or {@linkplain GlobalValue}
     * depending on target.
     * </p>
     * @param vd
     */
    private void emitLocalBlockVarDecl(VarDecl vd)
    {
        QualType ty = vd.getDeclType();
        backend.value.Value declPtr;
        if (ty.isConstantSizeType())
        {
            // A normal fixed sized variable becomes an alloca in the entry block.
            backend.type.Type lty = convertTypeForMem(ty);
            Instruction.AllocaInst alloca = createTempAlloc(lty);
            alloca.setName(vd.getDeclName());

            declPtr = alloca;
        }
        else
        {
            ensureInsertPoint();

            Type elemTy = convertTypeForMem(ty);
            Type elemPtrTy = PointerType.get(elemTy);

            Value vlaSize = emitVLASize(ty);

            // downcast the VLA size expression.
            vlaSize = builder.createIntCast(vlaSize, Type.Int32Ty,false, "");
            // allocate an array with variable size.
            Value vla = builder.createAlloca(Type.Int8Ty, vlaSize, "vla");

            // convert the pointer to array into regular pointer.
            declPtr = builder.creatBitCast(vla, elemPtrTy, "temp");
        }

        Value entry = localVarMaps.get(vd);
        assert entry == null:"Decl already exits in LocalVarMaps";

        entry = declPtr;

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
            Value loc = declPtr;
            if (!hasAggregateBackendType(init.getType()))
            {
                Value v = emitScalarExpr(init);
                emitStoreOfScalar(v, loc, vd.getDeclType());
            }
            else if (init.getType().isComplexType())
            {
                // todo handle var declaration of typed complex type.
            }
            else
            {
                emitAggExpr(init, loc);
            }
        }
    }

    private Value emitVLASize(QualType type)
    {
        // todo handle variable sized type in the future. 2016.11.5.
        assert type.isVariablyModifiedType():
                "Must pass variably modified type to EmitVLASizes!";
        ensureInsertPoint();

        ArrayType.VariableArrayType vat = type.getAsVariableArrayType();
        if (vat != null)
        {
            Value sizeEntry = vlaSizeMap.get(vat.getSizeExpr());
            if (sizeEntry == null)
            {
                Type sizeTy = convertType(vat.getSizeExpr().getType());

                // get the element size.
                QualType elemTy = vat.getElemType();
                Value elemSize;
                if (elemTy.isVariableArrayType())
                    elemSize = emitVLASize(elemTy);
                else
                    elemSize = ConstantInt.get(sizeTy, elemTy.getTypeSize() / 8);

                Value numElements = emitScalarExpr(vat.getSizeExpr());
                numElements = builder.createIntCast(numElements, sizeTy, false, "tmp");
                sizeEntry = builder.createMul(elemSize, numElements, "");
            }
            return sizeEntry;
        }
        ArrayType at = type.getAsArrayType();
        if (at != null)
        {
            emitVLASize(at.getElemType());
            return null;
        }

        frontend.type.PointerType ptr = type.<frontend.type.PointerType>getAs();
        assert ptr != null: "unknown VM type!";
        emitVLASize(ptr.getPointeeType());
        return null;
    }

    private Value emitScalarExpr(Tree.Expr expr)
    {
        assert expr !=null && !hasAggregateBackendType(expr.getType())
                :"Invalid scalar expression to emit";
        return new ScalarExprEmitter(this).visit(expr);
    }

    private void emitAggExpr(Tree.Expr expr, Value destPtr)
    {
        emitAggExpr(expr, destPtr, false, false);
    }

    /**
     * Emit the computation of the specified expression of aggregate
     * type.  The result is computed into {@code destPtr}.
     *
     * Note that if {@code destPtr} is null, the value of the aggregate
     * expression is not needed.
     * @param expr
     * @param destPtr
     */
    private void emitAggExpr(Tree.Expr expr, Value destPtr,
            boolean ignoreResult,
            boolean isInitializer)
    {
        assert expr!=null && hasAggregateBackendType(expr.getType())
                :"Invalid aggregate expression to emit";
        if (destPtr == null)return;

        new AggExprEmitter(this, destPtr, ignoreResult, isInitializer).visit(expr);
    }

    private void emitStoreOfScalar(Value val, Value addr/** boolean isVolatile*/, QualType ty)
    {
        if (ty.isBooleanType())
        {
            // Bool can have different representation in memory than in registers.
            Type srcTy = val.getType();
            PointerType destPtr = (PointerType)addr.getType();
            if (destPtr.getElemType() != srcTy)
            {
                Type memTy = PointerType.get(srcTy);
                addr = builder.createBitCast(addr, memTy, "storetmp");
            }
        }
        builder.createStore(val, addr);
    }

    private boolean containsLabel(Tree.Stmt s, boolean ignoreCaseStmts)
    {
        // Null statement, not a label.
        if (s == null) return false;

        // If this is a labelled statement, we want to emit code for it.
        // like this: if (0) {... foo: bar(); } goto foo;
        if (s instanceof Tree.LabelledStmt)
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
            Tree.CompoundStmt cs = (Tree.CompoundStmt)s;
            for (Tree.Stmt sub : cs.stats)
                if (containsLabel(sub, ignoreCaseStmts))
                    return true;
        }
        return false;
    }

    /**
     * Ensure the insert point has been defined as yet before emit IR.
     */
    private void ensureInsertPoint()
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

    private void emitBlock(BasicBlock bb)
    {
       emitBlock(bb, false);
    }

    private void emitBranch(BasicBlock targetBB)
    {
        // Emit a branch instruction from the current block to the
        // target block if this is a real one. If this is just a fall-through
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
     * @param vd
     */
    private void emitStaticBlockVarDecl(VarDecl vd)
    {
        Value entry = localVarMaps.get(vd);
        assert entry == null:"Decl already exists in localdeclmap!";

        GlobalVariable gv = createStaticBlockVarDecl(vd, ".");

        // Store into localVarMaps before generating initializer to handle
        // circular reference.
        entry = gv;

        if (vd.getDeclType().isVariablyModifiedType())
            emitVLASize(vd.getDeclType());

        if (vd.hasInit())
        {
            Constant init = generator
                    .emitConstantExpr(vd.getInit(), vd.getDeclType(), this);

            // If constant emission failed, then this should be a C++ static
            // initializer.
            if (init == null)
            {
                // TODO generator.errorUnsupported(vd.getInit(), "constant l-value expression");
            }
            else
            {
                if (gv.getType() != init.getType())
                {
                    GlobalVariable oldGV = gv;

                    gv = new GlobalVariable(init.getType(),
                            oldGV.isConstant(),
                            init, "");

                    // Replace all uses of the old global with the new global
                    Constant newPtrForOldDecl =
                            ConstantExpr.getBitCast(gv, oldGV.getType());
                    oldGV.replaceAllUsesWith(newPtrForOldDecl);

                    // Erase the old global, since it is no longer used.
                    oldGV.eraseFromParent();
                }

                gv.setInitializer(init);
            }
        }
    }

    private GlobalVariable createStaticBlockVarDecl(VarDecl vd,
            String separator)
    {
        QualType ty = vd.getDeclType();
        assert ty.isConstantSizeType():"VLAs cann't be static";

        String contextName = "";
        if (curFnDecl != null)
            contextName = curFnDecl.getDeclName();
        else
            assert false:"Unknown context for block var decl";

        String name = contextName + separator + vd.getDeclName();
        Type lty = generator.getCodeGenTypes().convertTypeForMem(ty);
        return new GlobalVariable(lty,
                ty.isConstant(),
                generator.emitNullConstant(vd.getDeclType()),
                name);
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
        assert !breakContinueStack.isEmpty():"break stmt not in a loop or switch!";

        BasicBlock breakBB = breakContinueStack.peek().breakBlock;
        emitBlock(breakBB);
    }

    private void finishFunction()
    {

    }

    private void emitContinueStmt(Tree.ContinueStmt s)
    {
        assert !breakContinueStack.isEmpty():"break stmt not in a loop or switch!";

        BasicBlock continueBB = breakContinueStack.peek().continueBlock;
        emitBlock(continueBB);
    }

    private void emitDefaultStmt(Tree.DefaultStmt s)
    {
        BasicBlock defaultBlock = switchInst.getDefaultBlock();
        assert defaultBlock.isEmpty():"EmitDefaultStmt: Default block already defined?";

        emitBlock(defaultBlock);
        emitStmt(s.getSubStmt());
    }

    private void emitCaseStmt(Tree.CaseStmt s)
    {
        emitBlock(createBasicBlock("sw.bb"));
        BasicBlock caseDest = builder.getInsertBlock();

        APSInt caseVal = s.getCondExpr().evaluateKnownConstInt();
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
        CaseStmt nextCase = s.getNextCaseStmt();
        while (nextCase != null)
        {
            curCase = nextCase;
            caseVal = curCase.getCondExpr().evaluateKnownConstInt();
            switchInst.addCase(ConstantInt.get(caseVal), caseDest);
            nextCase = curCase.getNextCaseStmt();
        }

        // Normal default recursion for non-cases.
        emitStmt(curCase.getSubStmt());
    }

    private BasicBlock createBasicBlock(String name, Function parent)
    {
        return createBasicBlock(name, parent, null);
    }

    private BasicBlock createBasicBlock()
    {
        return createBasicBlock("", curFn, null);
    }

    private BasicBlock createBasicBlock(String name)
    {
        return createBasicBlock(name, curFn, null);
    }

    private BasicBlock createBasicBlock(String name, Function parent, BasicBlock before)
    {
        return BasicBlock.createBasicBlock(id++, name, parent, before);
    }
}
