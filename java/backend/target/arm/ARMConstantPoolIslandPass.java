package backend.target.arm;
/*
 * Extremely C language Compiler
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

import backend.codegen.*;
import backend.debug.DebugLoc;
import backend.pass.FunctionPass;
import backend.support.MachineFunctionPass;
import backend.target.TargetData;
import gnu.trove.map.hash.TIntIntHashMap;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashSet;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.target.arm.ARMGenInstrNames.*;

/**
 * Due to the limited pc-relative displacement, ARM requires constant pool entries
 * to be scattered among the instructions inside the function. In order to do this,
 * we places constants in a special machine basic block close to the user of constant
 * pool.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMConstantPoolIslandPass extends MachineFunctionPass {
  /**
   * The size of each MachineBasicBlock in bytes of code, indexed by
   * MBB number.
   */
  private ArrayList<Integer> bbSizes;

  /**
   * The offset of each mbb in bytes from the function entry, starting from 0.
   */
  private ArrayList<Integer> bbOffets;

  /**
   * A sorted list of basic blocks where islands could be placed (e.g. blocks that
   * don't fall through to the following block, due to  a return, unreachable, or
   * unconditional branch).
   */
  private ArrayList<MachineBasicBlock> waterLists;

  /**
   * The subset of waterList that was created since the previous iteration by inserting
   * unconditional branches.
   */
  private HashSet<MachineBasicBlock> newWaterList;

  /**
   * One user of a constant pool, keeping the machine instruction reference, the CONST_ENTRY
   * instruction being referenced, and the max displacement allowed from the instruction to
   * the CONST_ENTRY instruction. The HighWaterMark records the highest basic block where a
   * new CPEntry can be placed. To ensure this pass terminates, the CP entries are initially
   * placed at the end of the function and then move monotonically to lower addresses. The
   *  exception to this rule is when the current CP entry for a particular CPUser is out of
   *  range, but there is another CP entry for the same constant value in range.  We want to
   *  use the existing in-range CP entry, but if it later moves out of range, the search for
   *  new water should resume where it left off.  The HighWaterMark is used to record that point.
   */
  private static class CPUser {
    MachineInstr mi;
    MachineInstr cpemi;
    MachineBasicBlock highWaterMark;
    int maxDisp;
    boolean negOk;
    boolean isSoImm;

    CPUser(MachineInstr mi, MachineInstr cpemi,
           int maxDisp, boolean negOk, boolean isSoImm) {
      this.mi = mi;
      this.cpemi = cpemi;
      this.highWaterMark = cpemi.getParent();
      this.maxDisp = maxDisp;
      this.negOk = negOk;
      this.isSoImm = isSoImm;
    }
  }

  /**
   * Keep track of all of the machine instructions that use various constant pools and
   * their max displacements.
   */
  private ArrayList<CPUser> cpUsers;

  private static class CPEntry {
    MachineInstr cpemi;
    int cpi;
    int refCount;
    CPEntry(MachineInstr cpemi, int cpi) {
      this(cpemi, cpi, 0);
    }

    CPEntry(MachineInstr cpemi, int cpi, int rc) {
      this.cpemi = cpemi;
      this.cpi = cpi;
      this.refCount = rc;
    }
  }

  /**
   * Keep track of all of the constant pool entry machine
   * instructions. For each original constpool index (i.e. those that
   * existed upon entry to this pass), it keeps a vector of entries.
   * Original elements are cloned as we go along; the clones are
   * put in the vector of the original element, but have distinct CPIs.
   */
  private ArrayList<ArrayList<CPEntry>> cpEntries;

  /**
   * Maps a JT index to the offset in {@linkplain this#cpEntries} containing
   * copies of that table.
   */
  private TIntIntHashMap jumpTableEntryIndices;

  private static class ImmBranch {
    MachineInstr mi;
    int maxDisp;
    boolean isCond;
    int unCondBr;
    ImmBranch(MachineInstr mi, int maxDisp, boolean isCond, int unCondBr) {
      this.mi = mi;
      this.maxDisp = maxDisp;
      this.isCond = isCond;
      this.unCondBr = unCondBr;
    }
  }

  /**
   * Keeps track of all the immediate branch instructins.
   */
  private ArrayList<ImmBranch> immBranches;

  /**
   * Keep track of all the thumb push / pop instructions.
   */
  private ArrayList<MachineInstr> pushPopMIs;

  private ARMInstrInfo tii;
  private ARMFunctionInfo afi;
  private ARMSubtarget subtarget;
  private boolean hasFarJump;
  private boolean hasInlineAsm;
  private boolean isThumb;
  private boolean isThumb1;
  private boolean isThumb2;
  private MachineFunction mf;

  private ARMConstantPoolIslandPass() {
    bbSizes = new ArrayList<>();
    bbOffets = new ArrayList<>();
    waterLists = new ArrayList<>();
    newWaterList = new HashSet<>();
    cpUsers = new ArrayList<>();
    cpEntries = new ArrayList<>();
    immBranches = new ArrayList<>();
    pushPopMIs = new ArrayList<>();
    jumpTableEntryIndices = new TIntIntHashMap();
  }

  public static FunctionPass createARMConstantIslandPass() {
    return new ARMConstantPoolIslandPass();
  }

  @Override
  public boolean runOnMachineFunction(MachineFunction mf) {
    this.mf = mf;
    MachineConstantPool mcp = mf.getConstantPool();
    tii = (ARMInstrInfo) mf.getSubtarget().getInstrInfo();
    afi = (ARMFunctionInfo) mf.getInfo();
    subtarget = (ARMSubtarget) mf.getSubtarget();

    hasFarJump = false;
    hasInlineAsm = false;
    isThumb = subtarget.isThumb();
    isThumb1 = subtarget.isThumb1Only();
    isThumb2 = subtarget.isThumb2();

    // renumber all basic blocks of the function.
    mf.renumberBlocks();

    boolean madeChange = false;

    // perform the initial placement of the constant pool entries.
    // to start with, we put them all at the end of function.
    ArrayList<MachineInstr> cpemis = new ArrayList<>();
    if (!mcp.isEmpty()) {
      doInitialPlacement(mf, cpemis);
    }

    if (mf.getJumpTableInfo() != null)
      doInitialJumpTablePlacement(cpemis);

    // set the next available PIC uid.
    afi.initPICLabelUId(cpemis.size());

    // do the initial scan of function, building up information about the sizes
    // of each basic block, the location of all the water, and finding all of
    // the constant pool users.
    initialFunctionScan(mf, cpemis);
    cpemis.clear();

    // remove unused constant pool entries.
    madeChange |= removeUnusedCPEntries();

    // we try to place a constant pool entry as a special CONST_ENTRY instruction
    // after the user of constant as close as possible. However, this procedure must
    // be iterative because the change made by placing a constant pool entry will
    // affect other constant pool entry.
    int noCPIters = 0, noBRIters = 0;
    while (true) {
      boolean cpChanges = false;
      for (int i = 0, e = cpUsers.size(); i < e; ++i)
        cpChanges |= handleConstantPoolUser(mf, i);

      if (cpChanges && ++noCPIters > 30)
        Util.shouldNotReachHere("Constant island pass failed to converge!");

      // clear the newWaterList now. If we split a block for a branches, it should
      // appear as "new water" for the next iteration of constant pool placement.
      newWaterList.clear();

      boolean brChanges = false;
      for (int i = 0, e = immBranches.size(); i != e; ++i)
        brChanges |= fixUpImmediateBr(mf, immBranches.get(i));

      if (brChanges && ++noBRIters > 30)
        Util.shouldNotReachHere("Branch fix up pass failed to converge!");

      if (!cpChanges && !brChanges)
        break;
      madeChange = true;
    }

    verify(mf);

    bbSizes.clear();
    bbOffets.clear();
    waterLists.clear();
    newWaterList.clear();
    cpUsers.clear();
    cpEntries.clear();
    immBranches.clear();
    pushPopMIs.clear();

    return madeChange;
  }

  /**
   * We also need to place the jump table info closed to the user of jt.
   * Otherwise, it generates assembler fixup error for 401.gcc of CPU2006.
   * @param cpemis
   */
  private void doInitialJumpTablePlacement(ArrayList<MachineInstr> cpemis) {
    int i = cpemis.size();
    MachineJumpTableInfo mjti = mf.getJumpTableInfo();
    ArrayList<MachineJumpTableEntry> jt = mjti.getJumpTables();

    MachineBasicBlock lastCorrectNumberedBB = null;
    ArrayList<MachineBasicBlock> worklist = new ArrayList<>(mf.getBasicBlocks());
    for (MachineBasicBlock mbb : worklist) {
      if (mbb.isEmpty()) continue;;
      MachineInstr mi = mbb.getLastInst();
      if (mi == null) continue;
      int jtOpcode;
      switch (mi.getOpcode()) {
        default: continue;
        case BR_JTadd:
        case BR_JTr:
        case tBR_JTr:
        case BR_JTm:
          jtOpcode = JUMPTABLE_ADDRS;
          break;
        case t2BR_JT:
          jtOpcode = JUMPTABLE_INSTS;
          break;
        case t2TBB_JT:
          jtOpcode = JUMPTABLE_TBB;
          break;
        case t2TBH_JT:
          jtOpcode = JUMPTABLE_TBH;
          break;
      }

      MachineOperand jtOp = mi.getOperand(1);
      int jti = jtOp.getIndex();
      int size = jt.get(jti).mbbs.size() * 4;
      MachineBasicBlock jumpTableMBB = mf.createMachineBasicBlock();
      mf.insert(mbb.getIndexToMF()+1, jumpTableMBB);
      MachineInstr cpemi = buildMI(jumpTableMBB, 0, new DebugLoc(),
              tii.get(jtOpcode)).addImm(i++).addJumpTableIndex(jti, 0).addImm(size).getMInstr();
      cpemis.add(cpemi);
      ArrayList<CPEntry> entries = new ArrayList<>();
      entries.add(new CPEntry(cpemi, jti));
      cpEntries.add(entries);
      jumpTableEntryIndices.put(jti, cpEntries.size() - 1);
      if (lastCorrectNumberedBB == null)
        lastCorrectNumberedBB = mbb;
    }
    if (lastCorrectNumberedBB != null)
      mf.renumberBlocks(lastCorrectNumberedBB);
  }

  private void verify(MachineFunction mf) {

  }

  private boolean fixUpImmediateBr(MachineFunction mf, ImmBranch br) {
    MachineInstr mi = br.mi;
    MachineBasicBlock destMBB = mi.getOperand(0).getMBB();

    if (bbIsInRange(mi, destMBB, br.maxDisp))
      return false;

    if (!br.isCond)
      return fixUpUnConditionalBr(mf, br);
    return fixUpConditionalBr(mf, br);
  }

  private boolean bbIsInRange(MachineInstr mi,
                              MachineBasicBlock destMBB,
                              int maxDisp) {
    int pcAdj = isThumb ? 4 : 8;
    int brOffest = getOffsetOf(mi) + pcAdj;
    int destOffset = bbOffets.get(destMBB.getNumber());

    if (brOffest <= destOffset) {
      if (destOffset - brOffest <= maxDisp)
        return true;
    }
    else {
      if (brOffest - destOffset <= maxDisp)
        return true;
    }
    return false;
  }

  private boolean fixUpUnConditionalBr(MachineFunction mf, ImmBranch br) {
    MachineInstr mi = br.mi;
    MachineBasicBlock mbb = mi.getParent();
    if (!isThumb1)
      Util.assertion("fixUpUnconditionalBr is Thumb1 only!");

    br.maxDisp = (1 << 21) * 2;
    mi.setDesc(tii.get(tBfar));
    bbSizes.set(mbb.getNumber(), bbSizes.get(mbb.getNumber()) + 2);
    adjustBBOffsetsAfter(mbb, 2);
    hasFarJump = true;
    return true;
  }

  private boolean fixUpConditionalBr(MachineFunction mf, ImmBranch br) {
    // Add an unconditional branch to the destination and invert the branch
    // condition to jump over it:
    // blt L1
    // =>
    // bge L2
    // b   L1
    // L2:
    MachineInstr mi = br.mi;
    MachineBasicBlock mbb  = mi.getParent();
    MachineBasicBlock destMBB = mi.getOperand(0).getMBB();
    ARMCC.CondCodes cc = ARMCC.getCondCodes((int) mi.getOperand(1).getImm());
    cc = ARMCC.getOppositeCondition(cc);
    int ccReg = mi.getOperand(2).getReg();

    // If the branch is at the end of its MBB and that has a fall-through block,
    // direct the updated conditional branch to the fall-through block. Otherwise,
    // split the MBB before the next instruction.
    boolean needSplit = mi != mbb.getLastInst() || !hasFallThrough(mbb);

    if (mi != mbb.getLastInst()) {
      if (mi.getIndexInMBB() + 1 == mbb.size() - 1 && mbb.getLastInst().getOpcode() == br.unCondBr) {
        // Last MI in the BB is an unconditional branch. Can we simply invert the
        // condition and swap destinations:
        // beq L1
        // b   L2
        // =>
        // bne L2
        // b   L1
        MachineBasicBlock newDest = mbb.getLastInst().getOperand(0).getMBB();
        if (bbIsInRange(mi, newDest, br.maxDisp)) {
          mbb.getLastInst().getOperand(0).setMBB(destMBB);
          mi.getOperand(0).setMBB(newDest);
          mi.getOperand(1).setImm(cc.ordinal());
          return true;
        }
      }
    }

    if (needSplit) {
      splitBlockBeforeInstr(mi);

      int delta = tii.getInstSizeInBytes(mbb.getLastInst());
      bbSizes.set(mbb.getNumber(), bbSizes.get(mbb.getNumber()) - delta);
      MachineBasicBlock splitMBB = mf.getMBBAt(mf.getIndexOfMBB(mbb) + 1);
      adjustBBOffsetsAfter(splitMBB, -delta);
      mbb.getLastInst().removeFromParent();
    }
    MachineBasicBlock nextMBB = mf.getMBBAt(mf.getIndexOfMBB(mbb) + 1);
    // Insert a new conditional branch and a new unconditional branch.
    // Also update the ImmBranch as well as adding a new entry for the new branch.
    buildMI(mbb, new DebugLoc(), tii.get(mi.getOpcode())).addMBB(nextMBB)
        .addImm(cc.ordinal()).addReg(ccReg);
    br.mi = mbb.getLastInst();
    bbSizes.set(mbb.getNumber(), bbSizes.get(mbb.getNumber()) + tii.getInstSizeInBytes(mbb.getLastInst()));
    if (isThumb)
      buildMI(mbb, new DebugLoc(), tii.get(br.unCondBr)).addMBB(destMBB)
          .addImm(ARMCC.CondCodes.AL.ordinal()).addReg(0);
    else
      buildMI(mbb, new DebugLoc(), tii.get(br.unCondBr)).addMBB(destMBB);

    bbSizes.set(mbb.getNumber(), bbSizes.get(mbb.getNumber()) + tii.getInstSizeInBytes(mbb.getLastInst()));
    int maxDisp = getUnconditionalBrDisp(br.unCondBr);
    immBranches.add(new ImmBranch(mbb.getLastInst(), maxDisp, false, br.unCondBr));

    bbSizes.set(mi.getParent().getNumber(), bbSizes.get(mi.getParent().getNumber()) - tii.getInstSizeInBytes(mi));
    mi.removeFromParent();

    int delta = tii.getInstSizeInBytes(mbb.getLastInst());
    adjustBBOffsetsAfter(mbb, delta);
    return true;
  }

  private static int getUnconditionalBrDisp(int opc) {
    switch (opc) {
      case tB:
        return ((1 << 10) -1 ) * 2;
      case t2B:
        return ((1 << 23) -1 ) * 2;
      default:
        break;
    }
    return ((1 << 23) -1 ) * 4;
  }

  private boolean handleConstantPoolUser(MachineFunction mf, int cpIdx) {
    CPUser user = cpUsers.get(cpIdx);
    MachineInstr userMI = user.mi;
    MachineInstr cpeMI = user.cpemi;
    int cpi = getCombinedIndex(cpeMI);
    int size = (int) cpeMI.getOperand(2).getImm();

    // get the value of PC which always points to the next instruction.
    int userOffset = getOffsetOf(userMI) + (isThumb ? 4 : 8);
    int result = lookForExistingCPEntry(user, userOffset);
    if (result == 1) return false;
    if (result == 2) return true;

    int id = afi.createPICLabelUId();

    MachineBasicBlock newIsland = mf.createMachineBasicBlock();
    MachineBasicBlock newMBB;
    int waterIter = lookForWater(user, userOffset);
    if (waterIter != -1) {
      MachineBasicBlock waterMBB = mf.getMBBAt(waterIter);
      if (newWaterList.contains(waterMBB)) {
        newWaterList.remove(waterMBB);
        newWaterList.add(newIsland);
      }
      newMBB = mf.getMBBAt(waterIter+1);
    }
    else {
      newMBB = createNewWater(cpIdx, userOffset);
      MachineBasicBlock waterMBB;
      int prior = mf.getIndexOfMBB(newMBB) - 1;
      Util.assertion(prior >= 0, "Invalid created new water basic block!");
      waterMBB = mf.getMBBAt(prior);
      waterIter = waterLists.indexOf(waterMBB);
      if (waterIter != -1)
        newWaterList.remove(waterMBB);

      newWaterList.add(newIsland);
    }

    if (waterIter != -1)
      waterLists.remove(waterIter);

    mf.insert(mf.getIndexOfMBB(newMBB), newIsland);

    updateForInsertedWaterBlock(newIsland);
    decrementOldEntry(cpi, cpeMI);
    user.highWaterMark = newIsland;
    user.cpemi = buildMI(newIsland, new DebugLoc(), cpeMI.getDesc())
        .addImm(id).addOperand(cpeMI.getOperand(1)).addImm(size).getMInstr();
    cpEntries.get(cpi).add(new CPEntry(user.cpemi, id, 1));

    bbOffets.set(newIsland.getNumber(), bbOffets.get(newMBB.getNumber()));
    if (isThumb && (bbOffets.get(newIsland.getNumber()) % 4 != 0 || hasInlineAsm))
      size += 2;

    bbSizes.set(newIsland.getNumber(), bbSizes.get(newIsland.getNumber()) + size);
    adjustBBOffsetsAfter(newIsland, size);

    for (int i = 0, e = userMI.getNumOperands(); i < e; ++i) {
      if (userMI.getOperand(i).isConstantPoolIndex()) {
        userMI.getOperand(i).setIndex(id);
        break;
      }
    }

    return true;
  }

  /**
   * No existing waterlist entry will work for cpUsers[cpuserIndex], so create a
   * place to put the CPE. The end of the block is used if in range, and the conditional
   * branch merged so control flow is correct. Othewise, the block is split to create
   * a hole with an unconditional branch around it.
   * @param cpi
   * @param userOffset
   * @return
   */
  private MachineBasicBlock createNewWater(int cpi, int userOffset) {
    CPUser user = cpUsers.get(cpi);
    MachineInstr userMI = user.mi;
    MachineInstr cpeMI = user.cpemi;
    MachineBasicBlock userMBB = userMI.getParent();
    MachineFunction mf = userMBB.getParent();
    int offsetOfNextBlock = bbOffets.get(userMBB.getNumber()) +
        bbSizes.get(userMBB.getNumber());
    Util.assertion(offsetOfNextBlock == bbOffets.get(userMBB.getNumber() + 1));

    MachineBasicBlock newMBB;

    if (hasFallThrough(userMBB) &&
        offsetIsInRange(userOffset, offsetOfNextBlock + (isThumb1 ? 2 : 4), user.maxDisp, user.negOk)) {
      if (userMBB.getLastInst() == userMI)
        Util.assertion(hasFallThrough(userMBB), "expected a fallthrough mbb!");

      newMBB = mf.getMBBAt(mf.getIndexOfMBB(userMBB) + 1);
      int uncondBr = isThumb ? (isThumb2 ? t2B : tB) :
          B;
      if (!isThumb)
        buildMI(userMBB, new DebugLoc(), tii.get(uncondBr)).addMBB(newMBB);
      else
        buildMI(userMBB, new DebugLoc(), tii.get(uncondBr)).addMBB(newMBB)
            .addImm(ARMCC.CondCodes.AL.ordinal()).addReg(0);

      int maxDisp = getUnconditionalBrDisp(uncondBr);
      immBranches.add(new ImmBranch(userMBB.getLastInst(), maxDisp, false, uncondBr));
      int delta = isThumb1 ? 2 : 4;
      bbSizes.set(userMBB.getNumber(), bbSizes.get(userMBB.getNumber()) + delta);
      adjustBBOffsetsAfter(userMBB, delta);
    }
    else {
      // What a big block.  Find a place within the block to split it.
      // This is a little tricky on Thumb1 since instructions are 2 bytes
      // and constant pool entries are 4 bytes: if instruction I references
      // island CPE, and instruction I+1 references CPE', it will
      // not work well to put CPE as far forward as possible, since then
      // CPE' cannot immediately follow it (that location is 2 bytes
      // farther away from I+1 than CPE was from I) and we'd need to create
      // a new island.  So, we make a first guess, then walk through the
      // instructions between the one currently being looked at and the
      // possible insertion point, and make sure any other instructions
      // that reference CPEs will be able to use the same island area;
      // if not, we back up the insertion point.

      // The 4 in the following is for the unconditional branch we'll be
      // inserting (allows for long branch on Thumb1).  Alignment of the
      // island is handled inside OffsetIsInRange.
      int baseInsertPoffset = userOffset + user.maxDisp - 4;

      // This could point off the end of the block if we've already got
      // constant pool entries following this block; only the last one is
      // in the water list.  Back past any possible branches (allow for a
      // conditional and a maximally long unconditional).
      if (baseInsertPoffset >= bbOffets.get(userMBB.getNumber()+1))
        baseInsertPoffset = bbOffets.get(userMBB.getNumber()+1) - (isThumb1 ? 6 : 8);

      int endInsertOffset = (int) (baseInsertPoffset + cpeMI.getOperand(2).getImm());
      int idx = userMI.getIndexInMBB();
      MachineInstr mi = userMBB.getInstAt(++idx);
      int cpuIndex = cpi + 1;
      int numCPUsers = cpUsers.size();
      MachineInstr lastIT = null;
      for (int offset = userOffset + tii.getInstSizeInBytes(userMI); offset < baseInsertPoffset;
           offset += tii.getInstSizeInBytes(mi)) {
        if (cpuIndex < numCPUsers && cpUsers.get(cpuIndex).mi.equals(mi)) {
          CPUser u = cpUsers.get(cpuIndex);
          if (!offsetIsInRange(offset, endInsertOffset, u.maxDisp, u.negOk)) {
            baseInsertPoffset -= isThumb1 ? 2 : 4;
            endInsertOffset -= isThumb1 ? 2 : 4;
          }
          endInsertOffset = (int) (endInsertOffset + cpUsers.get(cpuIndex).cpemi.getOperand(2).getImm());
          cpuIndex++;
        }
        if (mi.getOpcode() == t2IT)
          lastIT = mi;

        mi = userMBB.getInstAt(++idx);
      }

      mi = userMBB.getInstAt(--idx);
      if (lastIT != null) {
        OutRef<Integer> predReg = new OutRef<>();
        ARMCC.CondCodes cc = getITInstrPredicate(mi, predReg);
        if (cc != ARMCC.CondCodes.AL)
          mi = lastIT;
      }
      newMBB = splitBlockBeforeInstr(mi);
    }
    return newMBB;
  }

  private MachineBasicBlock splitBlockBeforeInstr(MachineInstr mi) {
    MachineBasicBlock origMBB = mi.getParent();
    MachineFunction mf = origMBB.getParent();
    MachineBasicBlock newMBB = mf.createMachineBasicBlock(origMBB.getBasicBlock());
    mf.getBasicBlocks().add(mf.getIndexOfMBB(origMBB)+1, newMBB);

    newMBB.splice(0, origMBB, origMBB.getIndexOf(mi), origMBB.size());

    int opc = isThumb ? (isThumb2 ? t2B : tB) : B;
    if (!isThumb)
      buildMI(origMBB, new DebugLoc(), tii.get(opc)).addMBB(newMBB);
    else
      buildMI(origMBB, new DebugLoc(), tii.get(opc)).addMBB(newMBB)
          .addImm(ARMCC.CondCodes.AL.ordinal()).addReg(0);

    // Update the CFG.  All succs of OrigBB are now succs of NewBB.
    while (!origMBB.succIsEmpty()) {
      MachineBasicBlock succ = origMBB.suxAt(0);
      origMBB.removeSuccessor(0);
      newMBB.addSuccessor(succ);

      Util.assertion(succ.isEmpty() || !succ.getFirstInst().isPHI(), "PHI should be destructed!");
    }

    origMBB.addSuccessor(newMBB);

    mf.renumberBlocks();
    bbSizes.add(newMBB.getNumber(), 0);
    bbOffets.add(newMBB.getNumber(), 0);
    int ip = waterLists.size();
    for (int i = 0, e = waterLists.size(); i != e; ++i) {
      if (waterLists.get(i).getNumber() <= origMBB.getNumber())
      {ip = i; break;}
    }
    if (ip == waterLists.size())
      --ip;

    MachineBasicBlock waterMBB = waterLists.get(ip);
    if (waterMBB.equals(origMBB))
      waterLists.add(ip+1, newMBB);
    else
      waterLists.add(ip, origMBB);

    newWaterList.add(origMBB);
    int origMBBNum = origMBB.getNumber();
    int newMBBNum = newMBB.getNumber();

    int delta = isThumb1 ? 2 : 4;

    int origBBSize = 0;
    for (MachineInstr itr : origMBB.getInsts())
      origBBSize += tii.getInstSizeInBytes(itr);

    bbSizes.set(origMBBNum, origBBSize);
    bbOffets.set(newMBBNum, bbOffets.get(origMBBNum) + bbSizes.get(origMBBNum));

    int newMBBSize = 0;
    for (MachineInstr itr : newMBB.getInsts())
      newMBBSize += tii.getInstSizeInBytes(itr);

    bbSizes.set(newMBBNum, newMBBSize);

    if (newMBB.getLastInst().getOpcode() == tBR_JTr) {
      int origOffset = bbOffets.get(origMBBNum) + bbSizes.get(origMBBNum) - delta;
      if (origOffset % 4 == 0)
        delta = 0;
      else {
        bbSizes.set(newMBBNum, bbSizes.get(newMBBNum) + 2);
        delta = 4;
      }
    }

    if (delta != 0)
      adjustBBOffsetsAfter(newMBB, delta);
    return newMBB;
  }

  private ARMCC.CondCodes getITInstrPredicate(MachineInstr mi, OutRef<Integer> predReg) {
    int opc = mi.getOpcode();
    predReg.set(0);
    if (opc == tBcc || opc == t2Bcc)
      return ARMCC.CondCodes.AL;
    return ARMRegisterInfo.getInstrPredicate(mi, predReg);
  }

  private void updateForInsertedWaterBlock(MachineBasicBlock newMBB) {
    newMBB.getParent().renumberBlocks();

    bbSizes.add(newMBB.getNumber(), 0);
    bbOffets.add(newMBB.getNumber(), 0);
    int ip = waterLists.size();
    for (int i = 0, e = waterLists.size(); i < e; ++i) {
      if (waterLists.get(i).getNumber() >= newMBB.getNumber()) {
        ip = i;
        break;
      }
    }
    waterLists.add(ip, newMBB);
  }

  /**
   * Find the proper water MBB where we can insert the CONSTPOOL_ENTRY from the
   * water list.
   * @param user
   * @param userOffset
   * @return
   */
  private int lookForWater(CPUser user, int userOffset) {
    if (waterLists.isEmpty())
      return -1;

    int waterIter = -1;
    boolean foundWaterThatWouldPad = false;
    int ipThatWouldPad = -1;
    for (int i = waterLists.size() - 1; ; --i) {
      MachineBasicBlock waterMbb = waterLists.get(i);
      if (waterIsInRange(userOffset, waterMbb, user) &&
          (waterMbb.getNumber() < user.highWaterMark.getNumber() ||
              newWaterList.contains(waterMbb))) {
        int wbbId = waterMbb.getNumber();
        if (isThumb && (bbOffets.get(wbbId) + bbSizes.get(wbbId))%4 != 0 ) {
          if (!foundWaterThatWouldPad) {
            foundWaterThatWouldPad = true;
            ipThatWouldPad = i;
          }
        }
        else {
          waterIter = i;
          return waterIter;
        }
      }
      if (i == 0)
        break;
    }
    if (foundWaterThatWouldPad) {
      return ipThatWouldPad;
    }
    return -1;
  }

  private boolean waterIsInRange(int userOffset, MachineBasicBlock water, CPUser user) {
    int maxDisp = user.maxDisp;
    int cpeOffset = bbOffets.get(water.getNumber()) + bbSizes.get(water.getNumber());

    if (cpeOffset < userOffset)
      userOffset += user.cpemi.getOperand(2).getImm();

    return offsetIsInRange(userOffset, cpeOffset, maxDisp, user.negOk);
  }

  private int getCombinedIndex(MachineInstr mi) {
    int cpi = mi.getOperand(1).getIndex();
    if (mi.getOperand(1).isJumpTableIndex()) {
      Util.assertion(jumpTableEntryIndices.containsKey(cpi),
              String.format("there must be an entry for key '%d' in jumpTableEntryIndices", cpi));
      cpi = jumpTableEntryIndices.get(cpi);
    }
    return cpi;
  }

  private int lookForExistingCPEntry(CPUser user, int userOffset) {
    MachineInstr userMI = user.mi;
    MachineInstr cpeMI = user.cpemi;

    if (cpeIsInRange(userMI, userOffset, cpeMI, user.maxDisp, user.negOk, true)) {
      return 1;
    }

    // No.  Look for previously created clones of the CPE that are in range.
    int cpi = getCombinedIndex(cpeMI);
    ArrayList<CPEntry> cpes = cpEntries.get(cpi);
    for (int i = 0, e = cpes.size(); i != e; ++i) {
      if (cpes.get(i).cpemi == null || cpes.get(i).cpemi.equals(cpeMI))
        continue;

      if (cpeIsInRange(userMI, userOffset, cpes.get(i).cpemi, user.maxDisp, user.negOk, false)) {
        user.cpemi = cpes.get(i).cpemi;

        for (int j = 0, ej = userMI.getNumOperands(); j != ej; ++j) {
          if (userMI.getOperand(j).isConstantPoolIndex()) {
            userMI.getOperand(j).setIndex(cpes.get(i).cpi);
            break;
          }
        }

        ++cpes.get(i).refCount;
        return decrementOldEntry(cpi, cpeMI) ? 2 : 1;
      }
    }
    return 0;
  }

  /**
   * Find the constant pool entry with the index {@code cpi} and instruction
   * {@code cpeMI}, and decrement its refcount. If the refcount becomes zero,
   * remove that dead constant pool entry. Return true if we removed that entry.
   * Otherwise, return false.
   * @param cpi
   * @param cpeMI
   * @return
   */
  private boolean decrementOldEntry(int cpi, MachineInstr cpeMI) {
    CPEntry cpe = findConstantPoolEntry(cpi, cpeMI);
    Util.assertion(cpe != null, "unexpected");
    if (--cpe.refCount == 0) {
      removeDeadCPEMI(cpeMI);
      cpe.cpemi = null;
      return true;
    }
    return false;
  }

  private boolean cpeIsInRange(MachineInstr mi, int userOffset,
                               MachineInstr cpeMI, int maxDisp,
                               boolean negOk, boolean doDump) {
    int cpeOffset = getOffsetOf(cpeMI);
    Util.assertion(cpeOffset % 4 == 0 || hasInlineAsm, "misaligned CPE");
    return offsetIsInRange(userOffset, cpeOffset, maxDisp, negOk);
  }

  private boolean offsetIsInRange(int userOffset,
                                  int trailOffset,
                                  int maxDisp,
                                  boolean negOk) {
    int totalAdj = 0;
    if (isThumb && userOffset % 4 != 0) {
      userOffset -= 2;
      totalAdj = 2;
    }

    if (isThumb && trailOffset % 4 != 0) {
      trailOffset += 2;
      totalAdj += 2;
    }

    if (isThumb2 && totalAdj != 4)
      maxDisp -= 4 - totalAdj;

    if (userOffset <= trailOffset) {
      // user before the trail
      if (trailOffset - userOffset <= maxDisp)
        return true;
    }
    else if (negOk) {
      // user after the trail.
      if (userOffset - trailOffset <= maxDisp)
        return true;
    }
    return false;
  }

  @Override
  public String getPassName() {
    return "ARM constant pool island and shorten branch pass";
  }

  /**
   * Perform the initial constant pool placement. To start with, put them all at the
   * end of function.
   * @param mf
   * @param cpemis
   */
  private void doInitialPlacement(MachineFunction mf, ArrayList<MachineInstr> cpemis) {
    // create an new MachineBasicBlock to hold all mcp.
    MachineBasicBlock mbb = mf.createMachineBasicBlock();
    mf.addMBBNumbering(mbb);

    // add all of the constants from the constant pool to the end block.
    ArrayList<MachineConstantPoolEntry> mcpes = mf.getConstantPool().getConstants();
    TargetData td = subtarget.getDataLayout();

    int i = 0;
    for (MachineConstantPoolEntry mcpe : mcpes) {
      int size = (int) td.getTypeAllocSize(mcpe.getType());
      Util.assertion((size & 3) == 0, "CP entry not multiple of 4 bytes!");

      MachineInstr cpeMI = buildMI(mbb, new DebugLoc(), tii.get(CONSTPOOL_ENTRY))
          .addImm(i).addConstantPoolIndex(i, 0, 0).addImm(size).getMInstr();
      cpemis.add(cpeMI);

      // add a new CPEntry, but there is not corresponding user yet.
      ArrayList<CPEntry> cpes = new ArrayList<>();
      cpes.add(new CPEntry(cpeMI, i));
      cpEntries.add(cpes);
      ++i;
    }
  }

  private static boolean hasFallThrough(MachineBasicBlock mbb) {
    // if the mbb is the last block, return false.
    MachineFunction mf = mbb.getParent();
    int index = mf.getIndexOfMBB(mbb);
    if (index == mf.getNumBlocks() - 1)
      return false;

    MachineBasicBlock nextMBB = mf.getMBBAt(index+1);
    for (int i = 0, e = mbb.getNumSuccessors(); i != e; ++i)
      if (nextMBB == mbb.suxAt(i))
        return true;

    return false;
  }

  /**
   * Do the initial scan of the function, building up information about the sizes of each
   * block, the location of all the water, and finding all of the constant pool entry users.
   * @param mf
   * @param cpemis
   */
  private void initialFunctionScan(MachineFunction mf, ArrayList<MachineInstr> cpemis) {
    // check there is inline asm.
    for (MachineBasicBlock mbb: mf.getBasicBlocks()) {
      for (MachineInstr mi : mbb.getInsts())
        if (mi.isInlineAsm()) {
          hasInlineAsm = true;
          break;
        }
    }

    int offset = 0;
    for (MachineBasicBlock mbb : mf.getBasicBlocks()) {
      // if this mbb doesn't fall through into the next MBB, then this is a
      // candidate for water insertion.
      if (!hasFallThrough(mbb))
        waterLists.add(mbb);

      int mbbSize = 0;
      for (MachineInstr mi : mbb.getInsts()) {
        mbbSize += tii.getInstSizeInBytes(mi);

        int opc = mi.getOpcode();
        if (mi.getDesc().isBranch()) {
          boolean isCond = false;
          int bits = 0;
          int scale = 1;
          int uOpc = opc;
          switch (opc) {
            default:
              continue;
            case tBR_JTr:
              mf.ensureAlignment(2);
              if ((offset + mbbSize)%4 != 0 || hasInlineAsm)
                mbbSize += 2; // padding.
              continue;
            case t2BR_JT:
              // TODO
              continue;
            case Bcc:
              isCond = true;
              uOpc = B;
              // fall through.
            case B:
              bits = 24;
              scale = 4;
              break;
            case tBcc:
              isCond = true;
              uOpc = tB;
              bits = 8;
              scale = 2;
              break;
            case tB:
              bits = 11;
              scale = 2;
              break;
            case t2Bcc:
              isCond = true;
              uOpc = t2B;
              bits = 20;
              scale = 2;
              break;
            case t2B:
              bits = 24;
              scale = 2;
              break;
          }

          // record the immediate branch.
          int maxOffsets = ((1 << (bits - 1)) - 1) * scale;
          immBranches.add(new ImmBranch(mi, maxOffsets, isCond, uOpc));
        }

        if (opc == tPUSH || opc == tPOP_RET)
          pushPopMIs.add(mi);

        if (opc == CONSTPOOL_ENTRY ||
            opc == JUMPTABLE_ADDRS ||
            opc == JUMPTABLE_INSTS ||
            opc == JUMPTABLE_TBB ||
            opc == JUMPTABLE_TBH)
          continue;

        // scan the instruction for constant pool operands.
        for (int op = 0, e = mi.getNumOperands(); op != e; ++op) {
          if (mi.getOperand(op).isConstantPoolIndex() ||
                  mi.getOperand(op).isJumpTableIndex()) {
            // we found a constant pool entry index constant.
            int bits = 0;
            int scale = 1;
            boolean negOk = false;
            boolean isSoImm = false;

            switch (opc) {
              case LEApcrel:
              case LEApcrelJT:
                bits = 8;
                scale = 4;
                negOk = true;
                isSoImm = true;
                break;
              case t2LEApcrel:
              case t2LEApcrelJT:
              case LDRi12:
              case LDRcp:
              case t2LDRpci:
                bits = 12;
                negOk = true;
                break;
              case tLEApcrel:
              case tLEApcrelJT:
              case tLDRpci:
                bits = 8;
                scale = 4;
                break;
              case VLDRD:
              case VLDRS:
                bits = 8;
                scale = 4;
                negOk = true;
                break;
            }

            // remember this user of a CP entry.
            int cpi = getCombinedIndex(mi);
            MachineInstr cpeMI = cpemis.get(cpi);
            int maxOffets = ((1 << bits) - 1)* scale;
            cpUsers.add(new CPUser(mi, cpeMI, maxOffets, negOk, isSoImm));

            // increment corresponding CPEntry reference count.
            CPEntry cpe = findConstantPoolEntry(cpi, cpeMI);
            Util.assertion(cpe != null, "can't find a corresponding CPEntry");
            ++cpe.refCount;

            // Instruction can only use one Constant Pool index. don't need to scan other
            // operand.
            break;
          }
        }
      }

      // in thumb mode, if this block is a constant pool island, we might need padding
      // so it is aligned on 4 bytes boundary.
      if (isThumb && !mbb.isEmpty() &&
          mbb.getFirstInst().getOpcode() == CONSTPOOL_ENTRY &&
          (offset % 4) != 0 || hasInlineAsm)
        mbbSize += 2;

      bbSizes.add(mbbSize);
      bbOffets.add(offset);
      offset += mbbSize;
    }
  }

  private CPEntry findConstantPoolEntry(int cpi, MachineInstr cpeMI) {
    ArrayList<CPEntry> cpes = cpEntries.get(cpi);
    for (CPEntry entry : cpes)
      if (entry.cpemi != null && entry.cpemi.equals(cpeMI))
        return entry;

    return null;
  }

  private boolean removeUnusedCPEntries() {
    boolean madeChange = false;
    for (ArrayList<CPEntry> cpes : cpEntries) {
      for (CPEntry entry : cpes) {
        if (entry.refCount == 0 && entry.cpemi != null) {
          removeDeadCPEMI(entry.cpemi);
          entry.cpemi = null;
          madeChange = true;
        }
      }
    }
    return madeChange;
  }

  /**
   * Remove a dead CONSTPOOL_ENTRY instruction.
   * @param mi
   */
  private void removeDeadCPEMI(MachineInstr mi) {
    MachineBasicBlock mbb = mi.getParent();
    int size = (int) mi.getOperand(2).getImm();
    mi.removeFromParent();
    bbSizes.set(mbb.getNumber(), bbSizes.get(mbb.getNumber()) - size);

    // All succeeding offsets have the current size value added in, fix this.
    if (mbb.isEmpty()) {
      if (bbSizes.get(mbb.getNumber()) != 0) {
        size += bbSizes.get(mbb.getNumber());
        bbSizes.set(mbb.getNumber(), 0);
      }
    }

    // After remove that CONSTPOOL_ENTRY, we need to adjust the offset of all
    // following MBB.
    adjustBBOffsetsAfter(mbb, -size);
  }

  private void adjustBBOffsetsAfter(MachineBasicBlock mbb, int delta) {
    MachineFunction mf = mbb.getParent();
    int idx = mf.getIndexOfMBB(mbb);
    for (int i = idx + 1, e = mf.getNumBlocks(); i != e; ++i) {
      bbOffets.set(i, bbOffets.get(i) + delta);
      if (!isThumb)
        continue;

      mbb = mf.getMBBAt(i);
      if (!mbb.isEmpty() && !hasInlineAsm) {
        if (mbb.getFirstInst().getOpcode() == CONSTPOOL_ENTRY) {
          int oldOffset = bbOffets.get(i) - delta;
          if ((oldOffset % 4) == 0 && bbOffets.get(i)%4 != 0) {
            bbSizes.set(i, bbSizes.get(i) + 2);
            delta += 2;
          }
          else if (oldOffset % 4 != 0 && bbOffets.get(i)%4 == 0) {
            bbSizes.set(i, bbSizes.get(i) - 2);
            delta -= 2;
          }
        }

        // Thumb1 jump tables require padding.  They should be at the end;
        // following unconditional branches are removed by AnalyzeBranch.
        // tBR_JTr expands to a mov pc followed by .align 2 and then the jump
        // table entries. So this code checks whether offset of tBR_JTr
        // is aligned; if it is, the offset of the jump table following the
        // instruction will not be aligned, and we need padding.
        MachineInstr thumbJTMI = mbb.getLastInst();
        if (thumbJTMI.getOpcode() == tBR_JTr) {
          int newMIOffset = getOffsetOf(thumbJTMI);
          int oldMIOffset = newMIOffset - delta;
          if (oldMIOffset % 4 == 0 && newMIOffset % 4 != 0) {
            bbSizes.set(i, bbSizes.get(i) - 2);
            delta -= 2;
          }
          else if (oldMIOffset % 4 != 0 && newMIOffset % 4 == 0) {
            // add new padding.
            bbSizes.set(i, bbSizes.get(i) + 2);
            delta += 2;
          }
        }
        if (delta == 0)
          return;
      }
    }
  }

  private int getOffsetOf(MachineInstr mi) {
    MachineBasicBlock mbb = mi.getParent();

    int offset = bbOffets.get(mbb.getNumber());
    if (isThumb && mi.getOpcode() == CONSTPOOL_ENTRY &&
      (offset % 4 != 0 || hasInlineAsm))
      offset += 2;

    for (int i = 0, e = mi.getIndexInMBB(); i != e; ++i) {
      offset += tii.getInstSizeInBytes(mbb.getInstAt(i));
    }
    return offset;
  }
}
