package backend.target;

import backend.codegen.EVT;
import backend.value.Value;

public class IntrinsicInfo
{
    public int opc;
    public EVT memVT;
    public Value ptrVal;
    public int offset;
    public int align;
    public boolean vol;
    public boolean readMem;
    public boolean writeMem;
}
