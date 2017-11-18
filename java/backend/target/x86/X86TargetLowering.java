package backend.target.x86;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.codegen.MVT;
import backend.target.TargetLowering;
import backend.target.TargetRegisterInfo;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86TargetLowering extends TargetLowering
{
    private X86Subtarget subtarget;
    private boolean x86ScalarSSEf64;
    private boolean x86ScalarSSEf32;
    private int x86StackPtr;
    private TargetRegisterInfo regInfo;

    public X86TargetLowering(X86TargetMachine tm)
    {
        super(tm);
        subtarget = tm.getSubtarget();
        x86ScalarSSEf64 = subtarget.hasSSE2();
        x86ScalarSSEf32 = subtarget.hasSSE1();
        x86StackPtr = subtarget.is64Bit() ? X86GenRegisterNames.RSP : X86GenRegisterNames.ESP;

        regInfo = tm.getRegisterInfo();

        // Set up the register classes.
        addRegisterClass(new MVT(MVT.i8), X86GenRegisterInfo.GR8RegisterClass);
        addRegisterClass(new MVT(MVT.i16), X86GenRegisterInfo.GR16RegisterClass);
        addRegisterClass(new MVT(MVT.i32), X86GenRegisterInfo.GR32RegisterClass);
        if (subtarget.is64Bit)
            addRegisterClass(new MVT(MVT.i64), X86GenRegisterInfo.GR64RegisterClass);

        if (x86ScalarSSEf64)
        {
            // f32 and f64 use SSE.
            // Set up the FP register class.
            addRegisterClass(new MVT(MVT.f32), X86GenRegisterInfo.FR32RegisterClass);
            addRegisterClass(new MVT(MVT.f64), X86GenRegisterInfo.FR64RegisterClass);
        }
        else if (x86ScalarSSEf32)
        {
            // f32 use SSE, but f64 use x87.
            // Set up the FP register class.
            addRegisterClass(new MVT(MVT.f32), X86GenRegisterInfo.FR32RegisterClass);
            addRegisterClass(new MVT(MVT.f64), X86GenRegisterInfo.RFP64RegisterClass);
        }
        else
        {
            // f32 and f64 in x87.
            // Set up the FP register classes.
            addRegisterClass(new MVT(MVT.f32), X86GenRegisterInfo.RFP32RegisterClass);
            addRegisterClass(new MVT(MVT.f64), X86GenRegisterInfo.RFP64RegisterClass);
        }

        // Long double always uses X87.
        addRegisterClass(new MVT(MVT.f80), X86GenRegisterInfo.RFP80RegisterClass);

        if (subtarget.hasMMX())
        {
            addRegisterClass(new MVT(MVT.v8i8), X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(new MVT(MVT.v4i16), X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(new MVT(MVT.v2i32), X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(new MVT(MVT.v2f32), X86GenRegisterInfo.VR64RegisterClass);
            addRegisterClass(new MVT(MVT.v1i64), X86GenRegisterInfo.VR64RegisterClass);
        }
        if (subtarget.hasSSE1())
        {
            addRegisterClass(new MVT(MVT.v4f32), X86GenRegisterInfo.VR128RegisterClass);
        }
        if (subtarget.hasSSE2())
        {
            addRegisterClass(new MVT(MVT.v2f64), X86GenRegisterInfo.VR128RegisterClass);

            addRegisterClass(new MVT(MVT.v16i8), X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(new MVT(MVT.v8i16), X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(new MVT(MVT.v4i32), X86GenRegisterInfo.VR128RegisterClass);
            addRegisterClass(new MVT(MVT.v2i64), X86GenRegisterInfo.VR128RegisterClass);
        }
        if (subtarget.hasAVX())
        {
            addRegisterClass(new MVT(MVT.v8f32), X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(new MVT(MVT.v4f64), X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(new MVT(MVT.v8i32), X86GenRegisterInfo.VR256RegisterClass);
            addRegisterClass(new MVT(MVT.v4i64), X86GenRegisterInfo.VR256RegisterClass);
        }

        computeRegisterProperties();
    }
}
