/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.bitcode.writer;

import backend.bitcode.BitcodeUtil;
import backend.bitcode.reader.BitcodeReader;
import backend.bitcode.reader.BitcodeReader.BitCodeAbbrev;
import backend.bitcode.reader.BitcodeReader.BitCodeAbbrevOp;
import backend.bitcode.reader.BitcodeReader.Encoding;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import tools.Util;

import java.util.ArrayList;
import java.util.LinkedList;

import static backend.bitcode.reader.BitcodeReader.BlockInfoCodes.BLOCKINFO_CODE_SETBID;
import static backend.bitcode.reader.BitcodeReader.FixedAbbrevIDs.*;
import static backend.bitcode.reader.BitcodeReader.StandardBlockIDs.BLOCKINFO_BLOCK_ID;
import static backend.bitcode.reader.BitcodeReader.StandardWidths.BlockIDWidth;
import static backend.bitcode.reader.BitcodeReader.StandardWidths.BlockSizeWidth;
import static backend.bitcode.reader.BitcodeReader.StandardWidths.CodeLenWidth;

/**
 * This class is designed to act as a container for storing bit sequence
 * when writing LLVM IR in-memory style to external bitcode file.
 */
public class BitstreamWriter {
  private TByteArrayList buffer;
  /**
   * Always between 0 and 31 inclusive, specifies the next bit to use.
   */
  private int curBit;
  /**
   * The current value. Only bits < curBit are valid.
   */
  private int curValue;
  /**
   * This is the declared size of code values used for the current
   * block, in bits.
   */
  private int curCodeSize;
  /**
   * When emitting a BLOCKINFO_BLOCK, this is the currently selected
   * BLOCK ID.
   */
  private int blockInfoCurBID;
  /**
   * Abbrevs installed at in this block.
   */
  private ArrayList<BitCodeAbbrev> curAbbrevs;

  private static class Block {
    int prevCodeSize;
    int startSizeWord;
    ArrayList<BitCodeAbbrev> prevAbbrevs;
    Block(int pcs, int ssw) {
      prevCodeSize = pcs;
      startSizeWord = ssw;
      prevAbbrevs = new ArrayList<>();
    }
  }

  private LinkedList<Block> blockScope;
  private static class BlockInfo {
    int blockID;
    ArrayList<BitCodeAbbrev> abbrevs;
    BlockInfo() {
      blockID = 0;
      abbrevs = new ArrayList<>();
    }
  }
  private LinkedList<BlockInfo> blockInfoRecords;

  public BitstreamWriter() {
    buffer = new TByteArrayList();
    curAbbrevs = new ArrayList<>();
    blockScope = new LinkedList<>();
    blockInfoRecords = new LinkedList<>();
    curBit = 0;
    curValue = 0;
    curCodeSize= 2;
  }
  public byte[] getBuffer() {
    return buffer.toArray();
  }

  public long getCurrentBitNo() {
    return buffer.size() * 8 + curBit;
  }

  public void emit(int val, int numBits) {
    Util.assertion(val != 0 && numBits <= 32, "Invalid value size!");
    Util.assertion((val & ~(~0 >>> (32-numBits))) == 0, "High bits must not be set!");
    curValue |= val << curBit;
    if (curBit + numBits < 32) {
      curBit += numBits;
      return;
    }

    // add the current word.
    int v = curValue;
    buffer.add((byte)(v >>> 0));
    buffer.add((byte)(v >>> 8));
    buffer.add((byte)(v >>> 16));
    buffer.add((byte)(v >>> 24));

    if (curBit != 0)
      curValue = val >>> (32 - curBit);
    else
      curValue = 0;
    curBit = (curBit + numBits) & 31;
  }

  public void emit64(long val, int numBits) {
    if (numBits <= 32)
      emit((int)val, numBits);
    else {
      emit((int)val, 32);
      emit((int)(val>>>32), numBits - 32);
    }
  }

  public void flushToWord() {
    if (curBit != 0) {
      int v = curValue;
      buffer.add((byte) (v >>> 0));
      buffer.add((byte) (v >>> 8));
      buffer.add((byte) (v >>> 16));
      buffer.add((byte) (v >>> 24));
      curBit = 0;
      curValue = 0;
    }
  }

  public void emitVBR(int val, int numBits) {
    int threshold = 1 << (numBits - 1);
    // emit the bits with VBR encoding, numbits - 1 bits at a time.
    while (Integer.compareUnsigned(val, threshold) >= 0) {
      emit((val & (1 << (numBits - 1))) | (1 << (numBits - 1)), numBits);
      val >>>= numBits - 1;
    }

    emit(val, numBits);
  }

  public void emitVBR64(long val, int numBits) {
    if ((int)val == val) {
      emitVBR((int)val, numBits);
      return;
    }

    long threshold = 1 << (numBits - 1);
    while (Long.compareUnsigned(val, threshold) >= 0) {
      emit(((int)val & ((1 << (numBits - 1)) - 1)) |
          (1 << (numBits - 1)), numBits);
      val >>>= numBits - 1;
    }
    emit((int)val, numBits);
  }

  public void emitCode(int val) {
    emit(val, curCodeSize);
  }

  public void backpatchWord(int byteNo, int newWord) {
    buffer.set(byteNo++, (byte)(newWord >>> 0));
    buffer.set(byteNo++, (byte)(newWord >>> 8));
    buffer.set(byteNo++, (byte)(newWord >>> 16));
    buffer.set(byteNo, (byte)(newWord >>> 24));
  }

  public BlockInfo getBlockInfo(int blockID) {
    if (!blockInfoRecords.isEmpty() && blockInfoRecords.getLast().blockID == blockID)
      return blockInfoRecords.getLast();
    for (int i = 0, e = blockInfoRecords.size(); i < e; i++) {
      if (blockInfoRecords.get(i).blockID == blockID)
        return blockInfoRecords.get(i);
    }
    return null;
  }

  public void enterSubBlock(int blockID, int codeLen) {
    // Block Header
    // [ENTER_SUBBLOCK, blockid, newcodelen, <align4bytes>, blocklen]
    emitCode(ENTER_SUBBLOCK);
    emitVBR(blockID, BlockIDWidth);
    emitVBR(codeLen, CodeLenWidth);
    flushToWord();

    int blockSizeWordLoc = buffer.size();
    int oldCodeSize = curCodeSize;

    // emit a placeholder, which will be replaced when the block is popped.
    emit(0, BlockSizeWidth);
    curCodeSize = codeLen;

    blockScope.add(new Block(oldCodeSize, blockSizeWordLoc/4));
    ArrayList<BitCodeAbbrev> t = blockScope.getLast().prevAbbrevs;
    blockScope.getLast().prevAbbrevs = curAbbrevs;
    curAbbrevs = t;

    BlockInfo info = getBlockInfo(blockID);
    if (info != null) {
      for (int i = 0, e = info.abbrevs.size(); i < e; i++) {
        curAbbrevs.add(info.abbrevs.get(i));
        info.abbrevs.set(i, null);
      }
    }
  }

  public void exitBlock() {
    Util.assertion(!blockScope.isEmpty(), "Block scope imbalance");
    curAbbrevs.clear();

    Block b = blockScope.getLast();
    emitCode(END_BLOCK);
    flushToWord();

    int sizeInWords = buffer.size()/4 - b.startSizeWord - 1;
    int byteNo = b.startSizeWord * 4;
    backpatchWord(byteNo, sizeInWords);

    curCodeSize = b.prevCodeSize;
    ArrayList<BitCodeAbbrev> t = blockScope.getLast().prevAbbrevs;
    blockScope.getLast().prevAbbrevs = curAbbrevs;
    curAbbrevs = t;
    blockScope.removeLast();
  }

  private void emitAbbreviatedLiteral(BitCodeAbbrevOp op, long val) {
    Util.assertion(op.isLiteral(), "Not a literal");
    // if the abbrev specifies the literal value to use, don't emit.
    Util.assertion(val == op.getLiteralValue(), "Invlaid abbrev for record!");
  }

  private void emitAbbreviatedField(BitCodeAbbrevOp op, long val) {
    Util.assertion(!op.isLiteral(), "Literals should use emitAbbreviatedLiteral");

    // encode the value as we are commanded.
    switch (op.getEncoding()) {
      default: Util.assertion("Unknown encoding!");
      case Encoding.Fixed:
        if (op.getEncodingData() != 0)
          emit((int)val, (int)op.getEncodingData());
        break;
      case Encoding.VBR:
        if (op.getEncodingData() != 0)
          emitVBR64(val, (int)op.getEncodingData());
        break;
      case Encoding.Char6:
        emit(BitCodeAbbrevOp.encodeChar6((byte)val), 6);
        break;
    }
  }

  private void emitRecordWithAbbrevImpl(int abbrev,
                                        TLongArrayList vals) {
    emitRecordWithAbbrevImpl(abbrev, vals, null);
  }

  private void emitRecordWithAbbrevImpl(int abbrev,
                                        TLongArrayList vals,
                                        String blob) {
    int blobLen = blob.length();
    int abbrevNo = abbrev - FIRST_APPLICATION_ABBREV;
    Util.assertion(abbrevNo < curAbbrevs.size(), "Inlvalid abbrev #");
    BitCodeAbbrev abbv = curAbbrevs.get(abbrevNo);

    emitCode(abbrev);

    int recordIdx = 0;
    for (int i = 0, e = abbv.getNumOperandInfos(); i < e; i++) {
      BitCodeAbbrevOp op = abbv.getOperandInfo(i);
      if (op.isLiteral()) {
        Util.assertion(recordIdx < vals.size(), "Invlaid abbrev/record!");
        emitAbbreviatedLiteral(op, vals.get(recordIdx));
        ++recordIdx;
      }
      else if (op.getEncoding() == Encoding.Array) {
        // Array case.
        Util.assertion(i + 2 == 2, "array op not second to last");
        BitCodeAbbrevOp eltEnc = abbv.getOperandInfo(++i);
        // If this record has blob data, emit it, otherwise we
        // must have record entries to encode this way.
        if (blob != null) {
          Util.assertion(recordIdx == vals.size(),
              "Blob data and record entries specified for array!");
          emitVBR(blobLen, 6);
          // emit each field.
          for (int j = 0; j < blobLen; j++)
            emitAbbreviatedField(eltEnc, (byte)blob.charAt(j));

          // Know that blob data is consumed for assertion above.
          blob = null;
        }
        else {
          // emit a vbr6 to indicate the number of elements present.
          emitVBR(vals.size() - recordIdx, 6);
          // emit each field.
          for (int size = vals.size(); recordIdx != size; ++recordIdx)
            emitAbbreviatedField(eltEnc, vals.get(recordIdx));
        }
      }
      else if (op.getEncoding() == Encoding.Blob) {
        // If this record has blob data, emit it, otherwise we must have record
        // entries to encode this way.
        // Emit a vbr6 to indicate the number of elements present.
        if (blob != null) {
          emitVBR(blobLen, 6);
          Util.assertion(recordIdx == vals.size(), "Blob data and record entries specified for blob operand");
        }
        else {
          emitVBR(vals.size() - recordIdx, 6);
        }

        // Flush to a 32-bit alignment boundary.
        flushToWord();
        Util.assertion((buffer.size() & 3) == 0, "Not 32-bit aligned");

        // Emit each field as a literal byte.
        if (blob != null) {
          for (int j = 0; j < blobLen; j++)
            buffer.add((byte)blob.charAt(j));

          // Know that blob data is consumed for assertion below.
          blob = null;
        }
        else {
          for (int size = vals.size(); recordIdx != size; recordIdx++) {
            Util.assertion(vals.get(recordIdx) < 256, "Value too large to emit as a blob");
            buffer.add((byte)vals.get(recordIdx));
          }
        }
        // Align end to 32-bits.
        while ((buffer.size() & 3) != 0)
          buffer.add((byte)0);
      }
      else {
        // Single scalar field.
        Util.assertion(recordIdx < vals.size(),
            "Invalid addbrev/record");
        emitAbbreviatedField(op, vals.get(recordIdx));
        ++recordIdx;
      }
    }

    Util.assertion(recordIdx == vals.size(),
        "Not all record operands emitted");
    Util.assertion(blob == null, "Blob data specified for record that doesn't use ti!");
  }

  public void emitRecord(int code, TLongArrayList vals) {
    emitRecord(code, vals, 0);
  }

  public void emitRecord(int code, TLongArrayList vals, int abbrev) {
    if (abbrev == 0) {
      // If we don't have an abbrev to use, emit this in its
      // fully unabbreviated form.
      emitCode(UNABBREV_RECORD);
      emitVBR(code, 6);
      emitVBR(vals.size(), 6);
      for (int i = 0, e = vals.size(); i< e; i++)
        emitVBR64(vals.get(i), 6);
      return;
    }

    vals.insert(0, code);
    emitRecordWithAbbrevImpl(abbrev, vals);
  }

  public void emitRecordWithBlob(int abbrev,
                                 TLongArrayList vals,
                                 String blob) {
    emitRecordWithAbbrevImpl(abbrev, vals, blob);
  }

  public void emitRecordWithArray(int abbrev,
                                  TLongArrayList vals,
                                  String array) {
    emitRecordWithAbbrevImpl(abbrev, vals, array);
  }

  private void encodeAbbrev(BitCodeAbbrev abbv) {
    emitCode(DEFINE_ABBREV);
    emitVBR(abbv.getNumOperandInfos(), 5);
    for (int i = 0, e = abbv.getNumOperandInfos(); i < e; i++) {
      BitCodeAbbrevOp op = abbv.getOperandInfo(i);
      emit(op.isLiteral()?1:0, 1);
      if (op.isLiteral()) {
        emitVBR64(op.getLiteralValue(), 8);
      }
      else {
        emit(op.getEncoding(), 3);
        if (op.hasEncodingData())
          emitVBR64(op.getEncodingData(), 5);
      }
    }
  }

  public int emitAbbrev(BitCodeAbbrev abbv) {
    encodeAbbrev(abbv);
    curAbbrevs.add(abbv);
    return curAbbrevs.size() - 1 + FIRST_APPLICATION_ABBREV;
  }

  public void enterBlockInfoBlock(int codeWidth) {
    enterSubBlock(BLOCKINFO_BLOCK_ID, codeWidth);
    blockInfoCurBID = -1;
  }

  private void switchToBlockID(int blockID) {
    if (blockInfoCurBID == blockID) return;
    TLongArrayList vals = new TLongArrayList();
    vals.add(blockID);
    emitRecord(BLOCKINFO_CODE_SETBID, vals);
    blockInfoCurBID = blockID;
  }

  private BlockInfo getOrCreateBlockInfo(int blockID) {
    BlockInfo bi = getBlockInfo(blockID);
    if (bi != null)
      return bi;
    bi = new BlockInfo();
    blockInfoRecords.add(bi);
    bi.blockID = blockID;
    return bi;
  }

  public int emitBlockInfoAbbrev(int blockID,
                                 BitCodeAbbrev abbv) {
    switchToBlockID(blockID);
    encodeAbbrev(abbv);

    // Add the abbrev to the specified block record.
    BlockInfo bi = getOrCreateBlockInfo(blockID);
    bi.abbrevs.add(abbv);
    return bi.abbrevs.size() - 1 + FIRST_APPLICATION_ABBREV;
  }
}
