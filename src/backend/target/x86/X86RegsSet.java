package backend.target.x86;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface X86RegsSet
{
	// 32-bit registers
	int EAX = 0; 
	int ECX = EAX + 1;
	int EDX = ECX + 1;
	int EBX = EDX + 1;
	int ESP = EBX + 1;
	int EBP = ESP + 1;
	int ESI = EBP + 1;
	int EDI = ESI + 1;

	// 16-bit registers
	int AX = EDI + 1;
	int CX = AX + 1;
	int DX = CX + 1;
	int BX = DX + 1;
	int SP = BX + 1;
	int BP = SP + 1;
	int SI = BP + 1;
	int DI = SI + 1;

	// 8-bit registers
	int AL = DI + 1;
	int CL = AL + 1;
	int DL = CL + 1;
	int BL = DL + 1;
	int AH = BL + 1;
	int CH = AH + 1;
	int DH = CH + 1;
	int BH = DH + 1;

	// Pseudo Floating Point registers
	int FP0 = BH + 1;
	int FP1 = FP0 + 1;
	int FP2 = FP1 + 1;
	int FP3 = FP2 + 1;
	int FP4 = FP3 + 1;
	int FP5 = FP4 + 1;
	int FP6 = FP5 + 1;
	int FP7 = FP6 + 1;

	// Floating point stack registers
	int ST0 = FP7 + 1;
	int ST1 = ST0 + 1;
	int ST2 = ST1 + 1;
	int ST3 = ST2 + 1;
	int ST4 = ST3 + 1;
	int ST5 = ST4 + 1;
	int ST6 = ST5 + 1;
	int ST7 = ST6 + 1;

	int X86_REG_NUM = ST7;
}
