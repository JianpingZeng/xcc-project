package utils.tablegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
enum MatcherKind {
  // Matcher state manipulation.
  Scope,                // Push a checking scope.
  RecordNode,           // Record the current node.
  RecordChild,          // Record a child of the current node.
  RecordMemRef,         // Record the memref in the current node.
  CaptureFlagInput,     // If the current node has an input glue, save it.
  MoveChild,            // Move current node to specified child.
  MoveParent,           // Move current node to parent.

  // Predicate checking.
  CheckSame,            // Fail if not same as prev match.
  CheckChildSame,       // Fail if child not same as prev match.
  CheckPatternPredicate,
  CheckPredicate,       // Fail if node predicate fails.
  CheckOpcode,          // Fail if not opcode.
  SwitchOpcode,         // Dispatch based on opcode.
  CheckType,            // Fail if not correct type.
  SwitchType,           // Dispatch based on type.
  CheckChildType,       // Fail if child has wrong type.
  CheckInteger,         // Fail if wrong val.
  CheckChildInteger,    // Fail if child is wrong val.
  CheckCondCode,        // Fail if not condcode.
  CheckValueType,
  CheckComplexPat,
  CheckAndImm,
  CheckOrImm,
  CheckFoldableChainNode,

  // Node creation/emisssion.
  EmitInteger,          // Create a TargetConstant
  EmitStringInteger,    // Create a TargetConstant from a string.
  EmitRegister,         // Create a register.
  EmitConvertToTarget,  // Convert a imm/fpimm to target imm/fpimm
  EmitMergeInputChains, // Merge together a chains for an input.
  EmitCopyToReg,        // Emit a copytoreg into a physreg.
  EmitNode,             // Create a DAG node
  EmitNodeXForm,        // Run a SDNodeXForm
  MarkFlagResults,      // Indicate which interior nodes have flag results.
  CompleteMatch,        // Finish a match and update the results.
  MorphNodeTo,           // Build a node, finish a match and update results.
}
