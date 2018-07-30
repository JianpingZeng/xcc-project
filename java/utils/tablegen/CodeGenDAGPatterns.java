package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import tools.Util;
import backend.codegen.EVT;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Pair;
import utils.tablegen.CodeGenInstruction.OperandInfo;
import utils.tablegen.Init.DagInit;
import utils.tablegen.Init.DefInit;
import utils.tablegen.Init.ListInit;
import utils.tablegen.Init.TypedInit;

import java.util.*;
import java.util.function.Predicate;

import static backend.codegen.MVT.isVoid;
import static utils.tablegen.SDNP.SDNPAssociative;
import static utils.tablegen.SDNP.SDNPCommutative;
import static utils.tablegen.TGParser.resolveTypes;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class CodeGenDAGPatterns
{
    private RecordKeeper records;
    private CodeGenTarget target;
    private ArrayList<CodeGenIntrinsic> intrinsics;
    private ArrayList<CodeGenIntrinsic> tgtIntrinsics;

    /**
     * Deterministic comparison of Record.
     */
    private static Comparator<Record> RecordCmp = new Comparator<Record>()
    {
        @Override
        public int compare(Record o1, Record o2)
        {
            return Integer.compare(o1.getID(), o2.getID());
        }
    };

    private TreeMap<Record, SDNodeInfo> sdnodes;
    private TreeMap<Record, Pair<Record, String>> sdNodeXForms;
    private TreeMap<Record, ComplexPattern> complexPatterns;
    private TreeMap<Record, TreePattern> patternFragments;
    private TreeMap<Record, DAGDefaultOperand> defaultOperands;
    private TreeMap<Record, DAGInstruction> instructions;

    private Record intrinsicVoidSDNode;
    private Record intrinsicWChainSDNode;
    private Record intrinsicWOChainSDNode;


    private ArrayList<PatternToMatch> patternsToMatch;

    public CodeGenDAGPatterns(RecordKeeper records) throws Exception
    {
        this.records = records;
        target = new CodeGenTarget();
        defaultOperands = new TreeMap<>(RecordCmp);
        patternFragments = new TreeMap<>(RecordCmp);
        complexPatterns = new TreeMap<>(RecordCmp);
        sdNodeXForms = new TreeMap<>(RecordCmp);
        sdnodes = new TreeMap<>(RecordCmp);
        instructions = new TreeMap<>(RecordCmp);
        patternsToMatch = new ArrayList<>();

        intrinsics = loadIntrinsics(records, false);
        tgtIntrinsics = loadIntrinsics(records, true);

        parseNodeInfo();
        parseNodeTransforms();
        parseComplexPatterns();
        parsePatternFragments();
        parseDefaultOperands();
        parseInstructions();
        // FIXME (Already fixed 2017.7.23) LLVM-tblgen has 1715 instructions after parseInstructions.
        //System.err.println(patternsToMatch.size());
        parsePatterns();

        // For example, commutative patterns can match multiple ways.Add them
        // to patternToMatch as well.
        generateVariants();
        // FIXME LLVM-tblgen has 2605 patterns after generateVariants.
        // System.err.println(patternsToMatch.size());

        // For example, we can detect loads, stores, and side effects in many
        // cases by examining an instruction's pattern.
        inferInstructionFlags();
    }

    private void inferInstructionFlags() throws Exception
    {
        TreeMap<String, CodeGenInstruction> instrDesc = target.getInstructions();
        for (Map.Entry<String, CodeGenInstruction> pair : instrDesc.entrySet())
        {
            CodeGenInstruction instInfo = pair.getValue();

            boolean[] res = inferFromPattern(instInfo, this);
            Util.assertion( res.length == 3);
            instInfo.mayStore = res[0];
            instInfo.mayLoad = res[1];
            instInfo.hasSideEffects = res[2];
        }
    }

    private static boolean[] inferFromPattern(CodeGenInstruction instInfo, CodeGenDAGPatterns cdp)
    {
        boolean mayStore, mayLoad, hasSideEffect;
        InstAnalyzer analyzer = new InstAnalyzer(cdp, false, false, false);
        boolean hadPattern =  analyzer.analyze(instInfo.theDef);
        mayStore = analyzer.mayStore;
        mayLoad = analyzer.mayLoad;
        hasSideEffect= analyzer.hasSideEffect;

        if (instInfo.mayStore)
        {
            if (mayStore)
                System.err.printf("Warning: mayStore flag explicitly set on instruction '%s'"
                                +  " but flag already inferred from pattern.\n",
                                instInfo.theDef.getName());
            mayStore = true;
        }

        if (instInfo.mayLoad)
        {
            if(mayLoad)
                System.err.printf("Warning: mayLoad flag explicitly set on instruction '%s'"+
                        " but flag already inferred from pattern.\n", instInfo.theDef.getName());
            mayLoad = true;
        }

        if (instInfo.neverHasSideEffects)
        {
            if (hadPattern)
                System.err.printf("Warning: neverHasSideEffects set on instruction '%s' " +
                        "which already has a pattern\n", instInfo.theDef.getName());
            hasSideEffect = false;
        }

        if (instInfo.hasSideEffects)
        {
            if (hasSideEffect)
                System.err.printf("Warning: hasSideEffects set on instruction '%s' " +
                        "which already inferred this.\n", instInfo.theDef.getName());
            hasSideEffect = true;
        }
        return new boolean[]{mayStore, mayLoad, hasSideEffect};
    }

    /**
     * Generates variants. For example, commutative patterns can match multiple ways.
     * Add them to PatternsToMatch as well.
     */
    private void generateVariants()
    {
        if (TableGen.DEBUG)
            System.out.println("Generating instruction variants.");

        // Loop over all of the patterns we've collected, checking to see if we can
        // generate variants of the instruction, through the exploitation of
        // identities.  This permits the target to provide aggressive matching without
        // the .td file having to contain tons of variants of instructions.
        //
        // Note that this loop adds new patterns to the PatternsToMatch list, but we
        // intentionally do not reconsider these.  Any variants of added patterns have
        // already been added.
        for (int i = 0, e = patternsToMatch.size();i != e; i++)
        {
            PatternToMatch match = patternsToMatch.get(i);

            HashSet<String> depVars = new HashSet<>();
            ArrayList<TreePatternNode> variants = new ArrayList<>();
            findDepVars(match.getSrcPattern(), depVars);
            if (TableGen.DEBUG)
            {
                System.out.print("Dependent/multiply used variables: ");
                dumpDepVars(depVars);
                System.out.println();
            }
            generateVariantsOf(match.getSrcPattern(), variants, this, depVars);

            Util.assertion(!variants.isEmpty(), "Must create at least orignal variant!");
            variants.remove(0);

            if (variants.isEmpty())
                continue;

            if (TableGen.DEBUG)
            {
                System.out.print("FOUND VARIANTS OF: ");
                match.getSrcPattern().dump();
                System.out.println();
            }


            for (int j = 0, sz = variants.size(); j != sz; j++)
            {
                TreePatternNode var = variants.get(j);
                if (TableGen.DEBUG)
                {
                    System.out.print("\tVAR" + j + ": ");
                    var.dump();
                    System.out.println();
                }

                boolean alreadyExists = false;
                for (int p = 0, ee = patternsToMatch.size(); p < ee; p++)
                {
                    if (match.getPredicates() != patternsToMatch.get(p).getPredicates())
                        continue;

                    if (var.isIsomorphicTo(patternsToMatch.get(p).getSrcPattern(), depVars))
                    {
                        if (TableGen.DEBUG)
                        {
                            System.out.println("\t***ALREADY EXISTS, igoring variant.");
                        }
                        alreadyExists = true;
                        break;
                    }
                }

                if (alreadyExists) continue;

                addPatternsToMatch(new PatternToMatch(match.getPredicates(),
                        var, match.getDstPattern(), match.getDstRegs(),
                        match.getAddedComplexity()));
            }
            if (TableGen.DEBUG)
                System.out.println();
        }
    }

    /**
     * Given a pattern N, generate all permutations we can of
     * the (potentially recursive) pattern by using algebraic laws.
     * @param node
     * @param outVariants
     * @param cdp
     * @param depVars
     */
    private static void generateVariantsOf(TreePatternNode node,
            ArrayList<TreePatternNode> outVariants,
            CodeGenDAGPatterns cdp,
            HashSet<String> depVars)
    {
        if (node.isLeaf())
        {
            outVariants.add(node);
            return;
        }

        SDNodeInfo nodeInfo = cdp.getSDNodeInfo(node.getOperator());

        // If this node is associative, re-associate.
        if (nodeInfo.hasProperty(SDNPAssociative))
        {
            // Re-associate by pulling together all of the linked operators
            ArrayList<TreePatternNode> maximalChildren = new ArrayList<>();
            gatherChildrenOfAssociativeOpcode(node, maximalChildren);

            // Only handle child sizes of 3.  Otherwise we'll end up trying too many
            // permutations.
            if (maximalChildren.size() == 3)
            {
                // Find the variants of all of our maximal children.
                ArrayList<TreePatternNode> avariants = new ArrayList<>(),
                bvariants = new ArrayList<>(),
                cvariants = new ArrayList<>();

                generateVariantsOf(maximalChildren.get(0), avariants, cdp, depVars);
                generateVariantsOf(maximalChildren.get(1), bvariants, cdp, depVars);
                generateVariantsOf(maximalChildren.get(2), cvariants, cdp, depVars);

                // There are only two ways we can permute the tree:
                //   (A op B) op C    and    A op (B op C)
                // Within these forms, we can also permute A/B/C.

                // Generate legal pair permutations of A/B/C.
                ArrayList<TreePatternNode> abvariants = new ArrayList<>(),
                        bavariants = new ArrayList<>(),
                        acVariants = new ArrayList<>(),
                        caVariants = new ArrayList<>(),
                        bcVariants = new ArrayList<>(),
                        cbVariants = new ArrayList<>();
                combineChildVariants(node, avariants, bvariants, abvariants, cdp, depVars);
                combineChildVariants(node, bvariants, avariants, bavariants, cdp, depVars);
                combineChildVariants(node, avariants, cvariants, acVariants, cdp, depVars);
                combineChildVariants(node, cvariants, avariants, caVariants, cdp, depVars);
                combineChildVariants(node, bvariants, cvariants, bcVariants, cdp, depVars);
                combineChildVariants(node, cvariants, bvariants, cbVariants, cdp, depVars);

                // Combine those into the result: (x op x) op x
                combineChildVariants(node, abvariants, cvariants, outVariants, cdp, depVars);
                combineChildVariants(node, bavariants, cvariants, outVariants, cdp, depVars);
                combineChildVariants(node, acVariants, bvariants, outVariants, cdp, depVars);
                combineChildVariants(node, caVariants, bvariants, outVariants, cdp, depVars);
                combineChildVariants(node, bcVariants, avariants, outVariants, cdp, depVars);
                combineChildVariants(node, cbVariants, avariants, outVariants, cdp, depVars);

                // Combine those into the result: x op (x op x)
                combineChildVariants(node, cvariants, abvariants, outVariants, cdp, depVars);
                combineChildVariants(node, cvariants, bavariants, outVariants, cdp, depVars);
                combineChildVariants(node, bvariants, acVariants, outVariants, cdp, depVars);
                combineChildVariants(node, bvariants, caVariants, outVariants, cdp, depVars);
                combineChildVariants(node, avariants, bcVariants, outVariants, cdp, depVars);
                combineChildVariants(node, avariants, cbVariants, outVariants, cdp, depVars);
                return;
            }
        }

        // Compute permutations of all children.
        ArrayList<ArrayList<TreePatternNode>> childVariants = new ArrayList<>(node.getNumChildren());
        for (int i = 0, e = node.getNumChildren(); i != e; i++)
        {
            ArrayList<TreePatternNode> list = new ArrayList<>();
            generateVariantsOf(node.getChild(i), list, cdp, depVars);
            childVariants.add(list);
        }

        // Build all permutations based on how the children were formed.
        combineChildVariants(node, childVariants, outVariants, cdp, depVars);

        // If this node is commutative, consider the commuted order.
        boolean isCommutativeIntrinsic = node.isCommutativeIntrinsic(cdp);
        if (nodeInfo.hasProperty(SDNPCommutative) || isCommutativeIntrinsic)
        {
            Util.assertion((node.getNumChildren() == 2 || isCommutativeIntrinsic), "Commutative but does't not have 2 chidren!");


            int nc = 0;
            for (int i = 0, e = node.getNumChildren(); i != e; i++)
            {
                TreePatternNode child = node.getChild(i);
                if (child.isLeaf())
                {
                    DefInit di = child.getLeafValue() instanceof DefInit ? (DefInit)child.getLeafValue() : null;
                    if (di != null)
                    {
                        Record rr = di.getDef();
                        if (rr.isSubClassOf("Register"))
                            continue;
                    }
                }
                ++nc;
            }

            if (isCommutativeIntrinsic)
            {
                Util.assertion(nc >= 3, "Commutative intrinsic should have at least 3 children!");
                ArrayList<ArrayList<TreePatternNode>> variants = new ArrayList<>();
                variants.add(childVariants.get(0));
                variants.add(childVariants.get(2));
                variants.add(childVariants.get(1));

                for (int i = 3; i != nc; i++)
                    variants.add(childVariants.get(i));
                combineChildVariants(node, variants, outVariants, cdp, depVars);
            }
            else if (nc == 2)
            {
                combineChildVariants(node, childVariants.get(1), childVariants.get(0),
                        outVariants, cdp, depVars);
            }
        }
    }

    private static void combineChildVariants(TreePatternNode orig,
            ArrayList<TreePatternNode> lhs,
            ArrayList<TreePatternNode> rhs,
            ArrayList<TreePatternNode> outVars,
            CodeGenDAGPatterns cdp,
            HashSet<String> depVars)
    {
        ArrayList<ArrayList<TreePatternNode>> childVariants = new ArrayList<>();
        childVariants.add(lhs);
        childVariants.add(rhs);
        combineChildVariants(orig, childVariants, outVars, cdp, depVars);
    }

    /**
     * Given a bunch of permutations of each child of the
     * 'operator' node, put them together in all possible ways.
     * @param orig
     * @param childVariants
     * @param outVariants
     * @param cdp
     * @param depVars
     */
    private static void combineChildVariants(TreePatternNode orig,
            ArrayList<ArrayList<TreePatternNode>> childVariants,
            ArrayList<TreePatternNode> outVariants,
            CodeGenDAGPatterns cdp,
            HashSet<String> depVars)
    {
        for (int i = 0, e = childVariants.size(); i != e; i++)
        {
            if (childVariants.get(i).isEmpty())
                return;
        }

        TIntArrayList idxs = new TIntArrayList();
        idxs.fill(0, childVariants.size(),0);
        boolean notDone;
        do
        {
            if (TableGen.DEBUG && !idxs.isEmpty())
            {
                System.err.printf("%s: Idxs = [ ", orig.getOperator().getName());
                for (int i = 0, e = idxs.size(); i != e; i++)
                    System.err.printf("%d ", idxs.get(i));

                System.err.println("]");
            }

            ArrayList<TreePatternNode> newChildren = new ArrayList<>();
            for (int i = 0, e = childVariants.size(); i != e; i++)
                newChildren.add(childVariants.get(i).get(idxs.get(i)));

            TreePatternNode r = new TreePatternNode(orig.getOperator(), newChildren);

            r.setName(orig.getName());
            r.setPredicateFns(orig.getPredicateFns());
            r.setTransformFn(orig.getTransformFn());
            r.setTypes(orig.getExtTypes());

            StringBuilder errString = new StringBuilder();
            if (r.canPatternMatch(errString, cdp))
            {
                boolean alreadyExists = false;
                for (int i = 0, e = outVariants.size(); i != e; i++)
                {
                    if (r.isIsomorphicTo(outVariants.get(i), depVars))
                    {
                        alreadyExists = true;
                        break;
                    }
                }

                if (!alreadyExists)
                    outVariants.add(r);
            }

            // Increment indices to the next permutation by incrementing the
            // indicies from last index backward, e.g., generate the sequence
            // [0, 0], [0, 1], [1, 0], [1, 1].
            int j;
            for (j = idxs.size() - 1; j >= 0; --j)
            {
                int tmp = idxs.get(j);
                idxs.set(j, tmp + 1);
                if (tmp + 1 == childVariants.get(j).size())
                    idxs.set(j, 0);
                else
                    break;
            }
            notDone = j >= 0;
        }while (notDone);
    }

    private static void gatherChildrenOfAssociativeOpcode(
            TreePatternNode node,
            ArrayList<TreePatternNode> children)
    {
        Util.assertion(node.getNumChildren() == 2, "Assocative but doesn't have 2 children");
        Record operator = node.getOperator();

        if (!node.getName().isEmpty() || !node.getPredicateFns().isEmpty()
                || node.getTransformFn() != null)
        {
            children.add(node);
            return;
        }

        if (node.getChild(0).isLeaf() || !node.getChild(0).getOperator().equals(operator))
            children.add(node.getChild(0));
        else
            gatherChildrenOfAssociativeOpcode(node.getChild(0), children);

        if (node.getChild(1).isLeaf() || !node.getChild(1).getOperator().equals(operator))
            children.add(node.getChild(1));
        else
            gatherChildrenOfAssociativeOpcode(node.getChild(1), children);
    }

    private void findDepVarsOf(TreePatternNode node, TObjectIntHashMap<String> depMap)
    {
        if (node.isLeaf())
        {
            if (node.getLeafValue() instanceof DefInit)
            {
                if (depMap.containsKey(node.getName()))
                    depMap.put(node.getName(), depMap.get(node.getName()) + 1);
                else
                    depMap.put(node.getName(), 1);
            }
        }
        else
        {
            for (int i = 0, e = node.getNumChildren(); i != e; i++)
                findDepVarsOf(node.getChild(i), depMap);
        }
    }
    /**
     * Find dependent variables within child patterns.
     * @param node
     * @param depVars
     */
    private void findDepVars(TreePatternNode node, HashSet<String> depVars)
    {
        TObjectIntHashMap<String> depCounts = new TObjectIntHashMap<>();
        for(TObjectIntIterator<String> itr = depCounts.iterator(); itr.hasNext();)
        {
            if (itr.value() > 1)
                depVars.add(itr.key());
        }
    }

    private static void dumpDepVars(Set<String> depVars)
    {
        if (depVars.isEmpty())
        {
            System.out.printf("<empty set>");
        }
        else
        {
            System.out.printf("[ ");
            for (String depVar : depVars)
            {
                System.out.printf("%s ", depVar);
            }
            System.out.printf("]");
        }
    }

    private void parsePatterns() throws Exception
    {
        ArrayList<Record> patterns = records.getAllDerivedDefinition("Pattern");

        for (int i = 0, e = patterns.size(); i != e; ++i)
        {
            DagInit tree = patterns.get(i).getValueAsDag("PatternToMatch");
            DefInit opDef = (DefInit)tree.getOperator();
            Record operator = opDef.getDef();
            TreePattern pattern;
            if (!operator.getName().equals("parallel"))
                pattern = new TreePattern(patterns.get(i), tree, true, this);
            else
            {
                ArrayList<Init> values = new ArrayList<>();
                RecTy listTy = null;
                for (int j = 0, ee = tree.getNumArgs(); j != ee; ++j)
                {
                    values.add(tree.getArg(j));
                    TypedInit targ = tree.getArg(j) instanceof TypedInit ? (TypedInit)tree.getArg(j):null;
                    if (targ == null)
                    {
                        System.err.printf("In dag: %s", tree.toString());
                        System.err.printf(" -- Untyped argument in pattern\n");
                        Util.assertion(false, "Untyped argument in pattern");
                    }
                    if (listTy != null)
                    {
                        listTy = resolveTypes(listTy, targ.getType());
                        if (listTy == null)
                        {
                            System.err.printf("In dag: %s", tree.toString());
                            System.err.printf(" -- Incompatible types in pattern argument\n");
                            Util.assertion(false, "Incompatible types in pattern arguments");
                        }
                    }
                    else
                    {
                        listTy = targ.getType();
                    }
                }

                ListInit li = new ListInit(values, new RecTy.ListRecTy(listTy));
                pattern = new TreePattern(patterns.get(i), li, true, this);
            }

            // Inline pattern fragments into it.
            pattern.inlinePatternFragments();

            ListInit li = patterns.get(i).getValueAsListInit("ResultInstrs");
            if (li.getSize() == 0) continue;

            // Parse the instruction.
            TreePattern result = new TreePattern(patterns.get(i), li, false, this);

            // Inline pattern fragments into it.
            result.inlinePatternFragments();

            if (result.getNumTrees() != 1)
            {
                result.error("Cannot handle instructions producing instructions " +
                        "with temporaries yet!");
            }

            boolean iterateInference;
            boolean inferredAllPatternTypes, inferredAllResultTypes;
            do
            {
                // Infer as many types as possible.  If we cannot infer all of them, we
                // can never do anything with this pattern: report it to the user.
                inferredAllPatternTypes = pattern.inferAllTypes();

                // Infer as many types as possible.  If we cannot infer all of them, we
                // can never do anything with this pattern: report it to the user.
                inferredAllResultTypes = result.inferAllTypes();

                // Apply the type of the result to the source pattern.  This helps us
                // resolve cases where the input type is known to be a pointer type (which
                // is considered resolved), but the result knows it needs to be 32- or
                // 64-bits.  Infer the other way for good measure.
                iterateInference = pattern.getTree(0).
                        updateNodeType(result.getTree(0).getExtTypes(), result);
                iterateInference |= result.getTree(0).
                        updateNodeType(pattern.getTree(0).getExtTypes(), result);
            }while (iterateInference);

            if (!inferredAllPatternTypes)
                pattern.error("Could not infer all types in pattern!");
            if (!inferredAllResultTypes)
                pattern.error("Could not infer all types in pattern result!");

            TreeMap<String, TreePatternNode> instInputs = new TreeMap<>();
            TreeMap<String, TreePatternNode> instResults = new TreeMap<>();
            ArrayList<Record> instImpInputs = new ArrayList<>();
            ArrayList<Record> instImpResults = new ArrayList<>();
            for (int j = 0, ee = pattern.getNumTrees(); j != ee; ++j)
            {
                findPatternInputsAndOutputs(pattern, pattern.getTree(j), instInputs, instResults,
                        instImpInputs, instImpResults);
            }

            // Promote the xform function to be an explicit node if set.
            TreePatternNode dstPattern = result.getOnlyTree();
            ArrayList<TreePatternNode> resultNodeOperands = new ArrayList<>();
            for (int ii = 0, ee = dstPattern.getNumChildren(); ii != ee; ++ii)
            {
                TreePatternNode opNode = dstPattern.getChild(ii);
                Record xform = opNode.getTransformFn();
                if (xform != null)
                {
                    opNode.setTransformFn(null);
                    ArrayList<TreePatternNode> children = new ArrayList<>();
                    children.add(opNode);
                    opNode = new TreePatternNode(xform, children);
                }
                resultNodeOperands.add(opNode);
            }

            dstPattern = result.getOnlyTree();
            if (!dstPattern.isLeaf())
                dstPattern = new TreePatternNode(dstPattern.getOperator(),
                        resultNodeOperands);
            dstPattern.setTypes(result.getOnlyTree().getExtTypes());
            TreePattern temp = new TreePattern(result.getRecord(), dstPattern, false, this);
            temp.inferAllTypes();

            StringBuilder reason = new StringBuilder();
            if (!pattern.getTree(0).canPatternMatch(reason, this))
                pattern.error("Pattern can never match: " + reason.toString());

            addPatternsToMatch(new PatternToMatch(
                    patterns.get(i).getValueAsListInit("Predicates"),
                    pattern.getTree(0),
                    temp.getOnlyTree(), instImpResults,
                    (int) patterns.get(i).getValueAsInt("AddedComplexity")));
        }
    }

    /**
     * Scan the specified TreePatternNode (which is
     * part of "I", the instruction), computing the set of inputs and outputs of
     * the pattern.  Report errors if we see anything naughty.
     * @param pattern
     * @param tree
     * @param instInputs
     * @param instResults
     * @param instImpInputs
     * @param instImpResults
     */
    private void findPatternInputsAndOutputs(TreePattern pattern,
            TreePatternNode tree,
            TreeMap<String, TreePatternNode> instInputs,
            TreeMap<String, TreePatternNode> instResults,
            ArrayList<Record> instImpInputs, ArrayList<Record> instImpResults)
            throws Exception
    {
        if (tree.isLeaf())
        {
            boolean isUse = handleUse(pattern, tree, instInputs, instImpInputs);
            if (!isUse && tree.getTransformFn() != null)
                pattern.error("Cannot specify a transform function for a non-input value!");
            return;
        }
        else if (tree.getOperator().getName().equals("implicit"))
        {
            for (int i = 0, e = tree.getNumChildren(); i != e; i++)
            {
                TreePatternNode dest = tree.getChild(i);
                if (!dest.isLeaf())
                    pattern.error("implicitly defined value should be a register!");

                DefInit val = dest.getLeafValue() instanceof DefInit ? (DefInit)dest.getLeafValue() : null;
                if (val == null || !val.getDef().isSubClassOf("Register"))
                    pattern.error("implicitly defined value should be a register!");

                instImpResults.add(val.getDef());
            }
            return;
        }
        else if (!tree.getOperator().getName().equals("set"))
        {
            for (int i = 0, e = tree.getNumChildren(); i != e; i++)
            {
                int vt = tree.getChild(i).getExtTypeNum(0);
                if (vt == isVoid)
                    pattern.error("Cannot have void nodes inside of patterns!");

                findPatternInputsAndOutputs(pattern, tree.getChild(i), instInputs,
                        instResults, instImpInputs, instImpResults);
            }

            boolean isUse = handleUse(pattern, tree, instInputs, instImpInputs);

            if (!isUse && tree.getTransformFn() != null)
                pattern.error("Cannot specify a transform function for a non-input value!");
            return;
        }

        // Otherwise, this is a set, validate and collect instruction results.
        if (tree.getNumChildren() == 0)
            pattern.error("set requires operands!");

        if (tree.getTransformFn() != null)
            pattern.error("Cannot specify a transform function on a set node!");

        int numDests = tree.getNumChildren() - 1;
        for (int i= 0; i != numDests; ++i)
        {
            TreePatternNode dest = tree.getChild(i);
            if (!dest.isLeaf())
                pattern.error("set destination should be a register!");

            DefInit val = dest.getLeafValue() instanceof DefInit ? (DefInit)dest.getLeafValue() : null;
            if (val == null)
                pattern.error("set destination should be a register!");

            if (val.getDef().isSubClassOf("RegisterClass") ||
                    val.getDef().isSubClassOf("PointerLikeRegClass"))
            {
                if (dest.getName().isEmpty())
                    pattern.error("set destination must have a name!");
                if (instResults.containsKey(dest.getName()))
                    pattern.error("cannot set '" + dest.getName() + "' multiple times");
                instResults.put(dest.getName(), dest);
            }
            else if (val.getDef().isSubClassOf("Register"))
            {
                instImpResults.add(val.getDef());
            }
            else
            {
                pattern.error("set destination should be a register!");
            }
        }

        // Verify and collect info from the computation.
        findPatternInputsAndOutputs(pattern, tree.getChild(numDests),
                instInputs, instResults, instImpInputs, instImpResults);
    }

    /**
     * Given "tree" a leaf in the pattern, check to see if it is an
     * instruction input.  Return true if this is a real use.
     * @param pattern
     * @param tree
     * @param instInputs
     * @param instImpInputs
     * @return
     */
    private static boolean handleUse(TreePattern pattern,
            TreePatternNode tree,
            TreeMap<String, TreePatternNode> instInputs,
            ArrayList<Record> instImpInputs) throws Exception
    {
        if (tree.getName().isEmpty())
        {
            if (tree.isLeaf())
            {
                DefInit di = (tree.getLeafValue() instanceof DefInit) ? (DefInit)tree.getLeafValue() : null;
                if (di != null && di.getDef().isSubClassOf("RegisterClass"))
                    pattern.error("Input " + di.getDef().getName() + " must be named!");
                else if (di != null && di.getDef().isSubClassOf("Register"))
                    instImpInputs.add(di.getDef());
            }
            return false;
        }

        Record rec;
        if (tree.isLeaf())
        {
            DefInit di = (tree.getLeafValue() instanceof DefInit) ? (DefInit)tree.getLeafValue() : null;
            if (di == null)
                pattern.error("Input $" + tree.getName() + " must be an identifier!");
            rec = di.getDef();
        }
        else
            rec = tree.getOperator();

        if (rec.getName().equals("srcvalue"))
            return false;

        if(!instInputs.containsKey(tree.getName()))
            instInputs.put(tree.getName(), tree);
        else
        {
            TreePatternNode slot = instInputs.get(tree.getName());
            Record slotRec;
            if (slot.isLeaf())
                slotRec = (slot.getLeafValue() instanceof DefInit)? ((DefInit)slot.getLeafValue()).getDef() :null;
            else
            {
                Util.assertion(slot.getNumChildren() == 0, "cann't be a use with children!");
                slotRec = slot.getOperator();
            }

            if (!rec.equals(slotRec))
                pattern.error("All $" + tree.getName() + " inputs must agree with each other");
            if (!slot.getExtTypes().equals(tree.getExtTypes()))
                pattern.error("All $" + tree.getName() + " inputs must agree with each other");
        }

        return true;
    }

    /**
     * Parse all of the instructions, inlining and resolving
     * any fragments involved.  This populates the Instructions list with fully
     * resolved instructions.
     */
    private void parseInstructions() throws Exception
    {
        ArrayList<Record> instrs = records.getAllDerivedDefinition("Instruction");
        // DONE !!! FIXME the number of instrs is correct.

        for (int idx = 0; idx < instrs.size(); idx++)
        {
            Record instr = instrs.get(idx);
            //System.out.println(idx);
            ListInit li = null;

            if(instr.getValueInit("Pattern") instanceof ListInit)
                li = instr.getValueAsListInit("Pattern");

            // If there is no pattern, only collect minimal information about the
            // instruction for its operand list.  We have to assume that there is one
            // result, as we have no detailed info.
            if (li == null || li.getSize() == 0)
            {
                ArrayList<Record> results = new ArrayList<>();
                ArrayList<Record> operands = new ArrayList<>();

                CodeGenInstruction instrInfo = target.getInstruction(instr.getName());

                if (!instrInfo.operandList.isEmpty())
                {
                    // These produce no results
                    if (instrInfo.numDefs == 0)
                    {
                        instrInfo.operandList.forEach(op->operands.add(op.rec));
                    }
                    else
                    {
                        // Assume the first operand is the result.
                        results.add(instrInfo.operandList.get(0).rec);

                        // The rest are inputs.
                        for (int i = 1, e = instrInfo.operandList.size(); i != e; i++)
                            operands.add(instrInfo.operandList.get(i).rec);
                    }
                }

                // Create and insert the instruction.
                ArrayList<Record> impResults = new ArrayList<>();
                ArrayList<Record> impOperands = new ArrayList<>();
                instructions.put(instr, new DAGInstruction(null, results, operands,
                        impResults, impOperands));

                continue;   // no pattern.
            }

            // Parse the instruction.
            // FIXME 2017.7.11 (Done!!!!) This constructor causes the child node of 'i' is not be initialized properly.
            TreePattern i = new TreePattern(instr, li, true, this);

            // Inline pattern fragments into it.
            i.inlinePatternFragments();

            /*
            if (idx == 748)
            {
                System.err.println("After inline pattern fragment: ");
                i.getOnlyTree().dump();
                System.err.println();
            }
            */

            /**
            idx = 748, llvm-tblegen 中i=2020.
             该程序:
            V_SETALLONES:	(set VR128:v16i8:$dst, immAllOnesV:v4i32)
                                                   ~~~~~~~~~~
                                                      ^
                                                      此处和llvm-tblgen中的结果不同
            In V_SETALLONES: Undefined node flavour used in pattern: immAllOnesV

             llvm-tblgen
             V_SETALLONES: 	(set VR128:$dst, (build_vector:v4i32)<<P:predicate_immAllOnesV>>)
            */
            // Infer as many types as possible.  If we cannot infer all of them, we can
            // never do anything with this instruction pattern: report it to the user.
            if (!i.inferAllTypes())
                i.error("Could not infer all type in pattern");

            // InstInputs - Keep track of all of the inputs of the instruction, along
            // with the record they are declared as.
            TreeMap<String, TreePatternNode> instInputs = new TreeMap<>();

            // InstResults - Keep track of all the virtual registers that are 'set'
            // in the instruction, including what reg class they are.
            TreeMap<String, TreePatternNode> instResults = new TreeMap<>();

            ArrayList<Record> instImpInputs = new ArrayList<>();
            ArrayList<Record> instImpResults = new ArrayList<>();

            // Verify that the top-level forms in the instruction are of void type, and
            // fill in the InstResults map.
            // FIXME  [(set VR128:$dst, (v4f32 (shufp:$src3 VR128:$src1, VR128:$src2)))]
            for (int j = 0, e = i.getNumTrees(); j != e; ++j)
            {
                TreePatternNode pat = i.getTree(j);
                int vt = pat.getExtTypeNum(0);
                if (vt != isVoid)
                    i.error("Top-level forms in instruction pattern should have"+
                            " void types");

                // Find inputs and outputs, and verify the structure of the uses/defs.
                findPatternInputsAndOutputs(i, pat, instInputs, instResults, instImpInputs, instImpResults);
            }

            // Now that we have inputs and outputs of the pattern, inspect the operands
            // list for the instruction.  This determines the order that operands are
            // added to the machine instruction the node corresponds to.
            int numResults = instResults.size();

            // Parse the operands list from the (ops) list, validating it.
            Util.assertion(i.getArgList().isEmpty(), "Args list should still be empty here!");
            CodeGenInstruction cgi = target.getInstruction(instr.getName());

            // Check that all of the results occur first in the list.
            ArrayList<Record> results = new ArrayList<>();
            TreePatternNode res0Node = null;
            for (int j = 0; j != numResults; j++)
            {
                if (j == cgi.operandList.size())
                    i.error("'" + instResults.entrySet().iterator().next().getKey()
                        + "' set but does not appear in operand list!");

                String opName = cgi.operandList.get(j).name;

                // Check that it exists in InstResults.
                if (!instResults.containsKey(opName))
                    i.error("Operand $" + opName + " does not exist in operand list!");
                TreePatternNode rnode = instResults.get(opName);

                if (j == 0)
                    res0Node = rnode;
                Record r = rnode.getLeafValue() instanceof DefInit ? ((DefInit) rnode.getLeafValue()).getDef() : null;
                if (r == null)
                    i.error("Operand $" + opName + " should be a set destination: all "
                            + "outputs must occur before inputs in operand list!");

                if (cgi.operandList.get(j).rec != r)
                {
                    i.error("Operand $" + opName + " class mismatch!");
                }

                // Remember the return type.
                results.add(cgi.operandList.get(j).rec);

                // Okay, this one checks out.
                instResults.remove(opName);
            }

            // Loop over the inputs next.  Make a copy of InstInputs so we can destroy
            // the copy while we're checking the inputs.
            TreeMap<String, TreePatternNode> instInputsCheck = new TreeMap<>(instInputs);

            ArrayList<TreePatternNode> resultNodeOperands = new ArrayList<>();
            ArrayList<Record> operands = new ArrayList<>();
            for (int j = numResults, e = cgi.operandList.size(); j != e; j++)
            {
                OperandInfo op = cgi.operandList.get(j);
                String opName = op.name;
                if (opName.isEmpty())
                    i.error("Operand #" + j + " in operands list has no name!");

                if (!instInputsCheck.containsKey(opName))
                {
                    // If this is an predicate operand or optional def operand with an
                    // DefaultOps set filled in, we can ignore this.  When we codegen it,
                    // we will do so as always executed.
                    if (op.rec.isSubClassOf("PredicateOperand")
                            || op.rec.isSubClassOf("OptionalDefOperand"))
                    {
                        // Does it have a non-empty DefaultOps field?  If so, ignore this
                        // operand.
                        if (!getDefaultOperand(op.rec).defaultOps.isEmpty())
                            continue;
                    }
                    i.error("Operand $" + opName +
                            " does not appear in the instruction pattern");
                }

                TreePatternNode inVal = instInputsCheck.get(opName);
                instInputsCheck.remove(opName);

                if (inVal.isLeaf() && inVal.getLeafValue() instanceof DefInit)
                {
                    Record inRec = ((DefInit)inVal.getLeafValue()).getDef();
                    if (op.rec != inRec && !inRec.isSubClassOf("ComplexPattern"))
                        i.error("Operand $" + opName + "'s register class disagrees" +
                                " between the operand and pattern");
                }
                operands.add(op.rec);

                // Construct the result for the dest-pattern operand list.
                TreePatternNode opNode = inVal.clone();

                // No predicate is useful on the result.
                opNode.clearPredicateFns();

                // Promote the xform function to be an explicit node if set.
                Record xform = opNode.getTransformFn();
                if (xform != null)
                {
                    opNode.setTransformFn(null);
                    ArrayList<TreePatternNode> childs = new ArrayList<>();
                    childs.add(opNode);
                    opNode = new TreePatternNode(xform, childs);
                }
                resultNodeOperands.add(opNode);
            }

            if (!instInputsCheck.isEmpty())
                i.error("Input operand $" + instInputsCheck.entrySet().iterator().next().getKey() +
                        " occurs in pattern but not in operands list!");

            TreePatternNode resultPattern = new TreePatternNode(i.getRecord(), resultNodeOperands);

            // Copy fully inferred output node type to instruction result pattern.
            if (numResults > 0)
            {
                resultPattern.setTypes(res0Node.getExtTypes());
            }

            // Create and insert the instruction.
            // FIXME: InstImpResults and InstImpInputs should not be part of
            // DAGInstruction.
            DAGInstruction theInst = new DAGInstruction(i, results, operands, instImpResults, instImpInputs);
            instructions.put(i.getRecord(), theInst);

            // Use a temporary tree pattern to infer all types and make sure that the
            // constructed result is correct.  This depends on the instruction already
            // being inserted into the Instructions map.
            TreePattern temp = new TreePattern(i.getRecord(), resultPattern, false, this);
            temp.inferAllTypes();

            DAGInstruction theInsertedInst = instructions.get(i.getRecord());
            theInsertedInst.setResultPattern(temp.getOnlyTree());

            if (TableGen.DEBUG)
                i.dump();
        }

        //System.err.println("The number of Record:" + instructions.size());
        //System.err.println("The number of Patterns to match before parseInstruction:" + patternsToMatch.size());
        for(Map.Entry<Record, DAGInstruction> pair : instructions.entrySet())
        {
            DAGInstruction theInst = pair.getValue();
            TreePattern pat = theInst.getPattern();
            if (pat == null)
            {
                // FIXME (DONE!!!) to many pattern be skipped. 2017.7.21
                //  Skip 965.  actucally, the number should be 344
                //System.err.println(pair.getKey().getName());
                continue;
            }
            TreePatternNode pattern = pat.getTree(0);
            TreePatternNode srcPattern;
            if (pattern.getOperator().getName().equals("set"))
                srcPattern = pattern.getChild(pattern.getNumChildren() - 1).clone();
            else
                srcPattern = pattern;

            StringBuilder reason = new StringBuilder();
            if (!srcPattern.canPatternMatch(reason, this))
                pat.error("Instruction can never match: " + reason.toString());

            Record instr = pair.getKey();
            TreePatternNode dstPattern = theInst.getResultPattern();
            addPatternsToMatch(new PatternToMatch(instr.getValueAsListInit("Predicates"),
                    srcPattern, dstPattern, theInst.getImpResults(),
                    (int)instr.getValueAsInt("AddedComplexity")));
        }

        //System.err.println("The number of Patterns to match after parseInstruction:" + patternsToMatch.size());
    }

    private void parseDefaultOperands() throws Exception
    {
        ArrayList<Record>[] defaultOps = new ArrayList[2];

        defaultOps[0] = records.getAllDerivedDefinition("PredicateOperand");
        defaultOps[1] = records.getAllDerivedDefinition("OptionalDefOperand");

        // Find some SDNode.
        Util.assertion(!sdnodes.isEmpty(), "No SDNodes parsed?");
        Init someSDNode = new DefInit(sdnodes.entrySet().iterator().next().getKey());

        for (int itr = 0; itr != 2; itr++)
        {
            for (int i = 0, e = defaultOps[itr].size(); i != e; i++)
            {
                DagInit defaultInfo = defaultOps[itr].get(i).getValueAsDag("DefaultOps");

                ArrayList<Pair<Init, String>> ops = new ArrayList<>();
                for (int op = 0, ee = defaultInfo.getNumArgs(); op != ee; ++op)
                {
                    ops.add(Pair.get(defaultInfo.getArg(op), defaultInfo.getArgName(op)));
                }

                DagInit di = new DagInit(someSDNode, "", ops);

                TreePattern pattern = new TreePattern(defaultOps[itr].get(i), di, false, this);
                Util.assertion(pattern.getNumTrees() == 1, "This ctor can only produce one tree!");

                DAGDefaultOperand defaultOpInfo = new DAGDefaultOperand();

                TreePatternNode t = pattern.getTree(0);
                for (int op = 0, sz = t.getNumChildren(); op != sz; ++op)
                {
                    TreePatternNode node = t.getChild(op);
                    // Resolve all types.
                    while (node.applyTypeConstraints(pattern, false));

                    if (node.containsUnresolvedType())
                    {
                        if (itr == 0)
                        {
                            throw new Exception("Value #" + i + " of PredicateOperand '"
                                + defaultOps[itr].get(i).getName() + "' doesn't have concrete type!");
                        }
                        else
                        {
                            throw new Exception("Value #" + i + " of OptionalDefOperand '"
                                    + defaultOps[itr].get(i).getName() + "' doesn't have concrete type!");
                        }
                    }

                    defaultOperands.put(defaultOps[itr].get(i), defaultOpInfo);
                }
            }
        }
    }

    private void parsePatternFragments() throws Exception
    {
        ArrayList<Record> fragments = records.getAllDerivedDefinition("PatFrag");

        // Step#1. parse all of the fragments.
        for (int i = 0, e = fragments.size(); i != e; i++)
        {
            Record fragment = fragments.get(i);
            DagInit tree = fragment.getValueAsDag("Fragment");
            TreePattern pattern = new TreePattern(fragment, tree, true, this);
            patternFragments.put(fragment, pattern);

            ArrayList<String> args = pattern.getArgList();
            HashSet<String> operandsSet = new HashSet<>(args);

            if (operandsSet.contains(""))
                pattern.error("Cannot have unnamed 'node' values in pattern fragment!");

            DagInit opsList = fragment.getValueAsDag("Operands");
            DefInit opsOp = opsList.getOperator() instanceof DefInit ?
                    (DefInit) opsList.getOperator() : null;
            if (opsOp == null || (!opsOp.getDef().getName().equals("ops")
                    && !opsOp.getDef().getName().equals("outs") && !opsOp.getDef().getName().equals("ins"))) {
                pattern.error("Operands list should start with '(ops ...'!");
            }

            // Copy over the arguments.
            args.clear();

            for (int j = 0, sz = opsList.getNumArgs(); j != sz; ++j)
            {
                if (!(opsList.getArg(j) instanceof DefInit)
                        || !((DefInit) opsList.getArg(j)).getDef().getName().equals("node"))
                {
                    pattern.error("Operands list should all be 'node' values.");
                }

                if (opsList.getArgName(j).isEmpty())
                    pattern.error("Operands list should have names for each operand!");
                if (!operandsSet.contains(opsList.getArgName(j)))
                {
                    pattern.error("'" + opsList.getArgName(j)
                            + "' does not occur in pattern or was multiply specified!");
                }
                operandsSet.remove(opsList.getArgName(j));
                args.add(opsList.getArgName(j));
            }

            if (!operandsSet.isEmpty())
            {
                pattern.error("Operands list nodes not contain an entry for operand '"
                        + operandsSet.iterator().next() + "'!");
            }

            // If there is a code init for this fragment, keep track of the fact that
            // this fragment uses it.
            String code = fragment.getValueAsCode("Predicate");
            if (!code.isEmpty())
            {
                pattern.getOnlyTree().addPredicateFn("predicate_" + fragment.getName());
            }

            Record transform = fragment.getValueAsDef("OperandTransform");
            if (!getSDNodeTransform(transform).second.isEmpty())
                pattern.getOnlyTree().setTransformFn(transform);
        }

        // Now that we've parsed all of the tree fragments, do a closure on them so
        // that there are not references to PatFrags left inside of them.
        for (int i = 0, e = fragments.size(); i != e; i++)
        {
            TreePattern pat = patternFragments.get(fragments.get(i));
            pat.inlinePatternFragments();

            // Infer as many types as possible.  Don't worry about it if we don't infer
            // all of them, some may depend on the inputs of the pattern.
            pat.inferAllTypes();

            // If debugging, print out the pattern fragment result.
            if (TableGen.DEBUG)
            {
                System.err.println(i);
                pat.dump();
                System.err.println();
            }
        }
    }

    private void parseComplexPatterns() throws Exception
    {
        ArrayList<Record> ams = records.getAllDerivedDefinition("ComplexPattern");
        for (Record r : ams)
        {
            complexPatterns.put(r, new ComplexPattern(r));
        }
    }

    private void parseNodeTransforms() throws Exception
    {
        ArrayList<Record> xforms = records.getAllDerivedDefinition("SDNodeXForm");
        for (Record r : xforms)
        {
            Record sdNode = r.getValueAsDef("Opcode");
            String code = r.getValueAsCode("XFormFunction");
            sdNodeXForms.put(r, new Pair<>(sdNode, code));
        }
    }

    /**
     * Parse all of the SDNode definitions for the target, populating SDNodes.
     */
    private void parseNodeInfo() throws Exception
    {
        ArrayList<Record> nodes = records.getAllDerivedDefinition("SDNode");
        for (Record node : nodes)
        {
            sdnodes.put(node, new SDNodeInfo(node));
        }

        intrinsicVoidSDNode = getSDNodeNamed("intrinsic_void");
        intrinsicWChainSDNode = getSDNodeNamed("intrinsic_w_chain");
        intrinsicWOChainSDNode = getSDNodeNamed("intrinsic_wo_chain");
    }


    /**
     * Read all of the intrinsics defined in the specified .td file.
     * @param rc
     * @param targetOnly
     * @return
     */
    public ArrayList<CodeGenIntrinsic> loadIntrinsics(RecordKeeper rc, boolean targetOnly)
    {
        try
        {
            ArrayList<Record>  i = rc.getAllDerivedDefinition("Intrinsic");

            ArrayList<CodeGenIntrinsic> res = new ArrayList<>();

            for (Record intr : i)
            {
                if (intr.getValueAsBit("isTarget") == targetOnly)
                    res.add(new CodeGenIntrinsic(intr));
            }
            return res;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public CodeGenTarget getTarget()
    {
        return target;
    }

    public Record getSDNodeNamed(String name)
    {
        Record rec = records.getDef(name);
        if (rec == null || !rec.isSubClassOf("SDNode"))
        {
            System.err.printf("Error getting SDNode '%s'!\n", name);
            System.exit(1);
        }
        return rec;
    }

    public SDNodeInfo getSDNodeInfo(Record r)
    {
        Util.assertion(sdnodes.containsKey(r), "Undefined node!");
        return sdnodes.get(r);
    }

    public Pair<Record, String> getSDNodeTransform(Record r)
    {
        Util.assertion(sdNodeXForms.containsKey(r), "Invalid transform");
        return sdNodeXForms.get(r);
    }

    public TreeMap<Record, Pair<Record, String>> getSdNodeXForms()
    {
        return sdNodeXForms;
    }

    public ComplexPattern getComplexPattern(Record r)
    {
        Util.assertion(complexPatterns.containsKey(r), "Unknown address mode!");
        return complexPatterns.get(r);
    }

    public CodeGenIntrinsic getIntrinsic(Record r)
    {
        for (CodeGenIntrinsic cgi : intrinsics)
            if (cgi.theDef.equals(r)) return cgi;
        for (CodeGenIntrinsic cgi : tgtIntrinsics)
            if (cgi.theDef.equals(r)) return cgi;

        Util.assertion(false, "Undefined intrinsic!");
        return null;
    }

    public CodeGenIntrinsic getIntrinsicInfo(int iid)
    {
        if (iid - 1 < intrinsics.size())
            return intrinsics.get(iid - 1);
        if (iid - intrinsics.size() -1 < tgtIntrinsics.size())
            return tgtIntrinsics.get(iid - intrinsics.size() - 1);
        Util.assertion(false, "Bad intrinsic ID!");
        System.exit(1);
        return null;
    }

    public int getIntrinsicID(Record r)
    {
        int idx = 0;
        for (CodeGenIntrinsic cgi : intrinsics)
        {
            if (cgi.theDef.equals(r))
                return idx;
            ++idx;
        }
        idx = 0;
        for (CodeGenIntrinsic cgi : tgtIntrinsics)
        {
            if (cgi.theDef.equals(r))
                return idx;
            ++idx;
        }
        Util.assertion(false, "Undefined intrinsic!");
        System.exit(1);
        return -1;
    }

    public DAGDefaultOperand getDefaultOperand(Record r)
    {
        Util.assertion(defaultOperands.containsKey(r), "Isn't an analyzed default operand?");
        return defaultOperands.get(r);
    }

    public TreePattern getPatternFragment(Record r)
    {
        Util.assertion(patternFragments.containsKey(r), "Invalid pattern fragment request!");
        return patternFragments.get(r);
    }

    public TreeMap<Record, TreePattern> getPatternFragments()
    {
        return patternFragments;
    }

    public ArrayList<PatternToMatch> getPatternsToMatch()
    {
        return patternsToMatch;
    }

    private void addPatternsToMatch(PatternToMatch pat)
    {
        //Util.assertion( (pat != null));
        //if (!patternsToMatch.contains(pat))
        patternsToMatch.add(pat);
    }

    public DAGInstruction getInstruction(Record r)
    {
        Util.assertion(instructions.containsKey(r), "Undefined instruction!");
        return instructions.get(r);
    }

    public Record getIntrinsicVoidSDNode()
    {
        return intrinsicVoidSDNode;
    }

    public Record getIntrinsicWChainSDNode()
    {
        return intrinsicWChainSDNode;
    }

    public Record getIntrinsicWOChainSDNode()
    {
        return intrinsicWOChainSDNode;
    }

    public static TIntArrayList filterEVTs(TIntArrayList inVTs, Predicate<Integer> filter)
    {
        TIntArrayList result = new TIntArrayList();
        for (int i = 0, e = inVTs.size(); i != e; i++)
        {
            int val = inVTs.get(i);
            if (filter.test(val))
                result.add(val);
        }
        return result;
    }

    public static TIntArrayList filterVTs(TIntArrayList inVTs, Predicate<Integer> filter)
    {
        TIntArrayList result = new TIntArrayList();
        for (int i = 0; i < inVTs.size(); i++)
        {
            int vt = inVTs.get(i);
            if (filter.test(vt))
                result.add(vt);
        }
        return result;
    }

    public static TIntArrayList convertVTs(TIntArrayList inVTs)
    {
        TIntArrayList res = new TIntArrayList();
        inVTs.forEach(res::add);
        return res;
    }

    public static Predicate<Integer> isInteger = new Predicate<Integer>()
    {
        @Override
        public boolean test(Integer vt)
        {
            return new EVT(vt).isInteger();
        }
    };

    public static Predicate<Integer> isFloatingPoint = new Predicate<Integer>()
    {
        @Override
        public boolean test(Integer vt)
        {
            return new EVT(vt).isFloatingPoint();
        }
    };

    public static Predicate<Integer> isVector = new Predicate<Integer>()
    {
        @Override
        public boolean test(Integer vt)
        {
            return new EVT(vt).isVector();
        }
    };
}
