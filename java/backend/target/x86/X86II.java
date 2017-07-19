package backend.target.x86;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public interface X86II
{
	//===------------------------------------------------------------------===//
	// X86 Specific MachineOperand flags.

	int MO_NO_FLAG = 0;

	/// MO_GOT_ABSOLUTE_ADDRESS - On a symbol operand, this represents a
	/// relocation of:
	///    SYMBOL_LABEL + [. - PICBASELABEL]
	int MO_GOT_ABSOLUTE_ADDRESS = 1;

	/// MO_PIC_BASE_OFFSET - On a symbol operand this indicates that the
	/// immediate should get the value of the symbol minus the PIC base label:
	///    SYMBOL_LABEL - PICBASELABEL
	int MO_PIC_BASE_OFFSET = 2;

	/// MO_GOT - On a symbol operand this indicates that the immediate is the
	/// offset to the GOT entry for the symbol name from the base of the GOT.
	///
	/// See the X86-64 ELF ABI supplement for more details.
	///    SYMBOL_LABEL @GOT
	int MO_GOT = 3;

	/// MO_GOTOFF - On a symbol operand this indicates that the immediate is
	/// the offset to the location of the symbol name from the base of the GOT.
	///
	/// See the X86-64 ELF ABI supplement for more details.
	///    SYMBOL_LABEL @GOTOFF
	int MO_GOTOFF = 4;

	/// MO_GOTPCREL - On a symbol operand this indicates that the immediate is
	/// offset to the GOT entry for the symbol name from the current code
	/// location.
	///
	/// See the X86-64 ELF ABI supplement for more details.
	///    SYMBOL_LABEL @GOTPCREL
	int MO_GOTPCREL = 5;

	/// MO_PLT - On a symbol operand this indicates that the immediate is
	/// offset to the PLT entry of symbol name from the current code location.
	///
	/// See the X86-64 ELF ABI supplement for more details.
	///    SYMBOL_LABEL @PLT
	int MO_PLT = 6;

	/// MO_TLSGD - On a symbol operand this indicates that the immediate is
	/// some TLS offset.
	///
	/// See 'ELF Handling for Thread-Local Storage' for more details.
	///    SYMBOL_LABEL @TLSGD
	int MO_TLSGD = 7;

	/// MO_GOTTPOFF - On a symbol operand this indicates that the immediate is
	/// some TLS offset.
	///
	/// See 'ELF Handling for Thread-Local Storage' for more details.
	///    SYMBOL_LABEL @GOTTPOFF
	int MO_GOTTPOFF = 8;

	/// MO_INDNTPOFF - On a symbol operand this indicates that the immediate is
	/// some TLS offset.
	///
	/// See 'ELF Handling for Thread-Local Storage' for more details.
	///    SYMBOL_LABEL @INDNTPOFF
	int MO_INDNTPOFF = 9;

	/// MO_TPOFF - On a symbol operand this indicates that the immediate is
	/// some TLS offset.
	///
	/// See 'ELF Handling for Thread-Local Storage' for more details.
	///    SYMBOL_LABEL @TPOFF
	int MO_TPOFF = 10;

	/// MO_NTPOFF - On a symbol operand this indicates that the immediate is
	/// some TLS offset.
	///
	/// See 'ELF Handling for Thread-Local Storage' for more details.
	///    SYMBOL_LABEL @NTPOFF
	int MO_NTPOFF = 11;

	/// MO_DLLIMPORT - On a symbol operand "FOO", this indicates that the
	/// reference is actually to the "__imp_FOO" symbol.  This is used for
	/// dllimport linkage on windows.
	int MO_DLLIMPORT = 12;

	/// MO_DARWIN_STUB - On a symbol operand "FOO", this indicates that the
	/// reference is actually to the "FOO$stub" symbol.  This is used for calls
	/// and jumps to external functions on Tiger and before.
	int MO_DARWIN_STUB = 13;

	/// MO_DARWIN_NONLAZY - On a symbol operand "FOO", this indicates that the
	/// reference is actually to the "FOO$non_lazy_ptr" symbol, which is a
	/// non-PIC-base-relative reference to a non-hidden dyld lazy pointer stub.
	int MO_DARWIN_NONLAZY = 14;

	/// MO_DARWIN_NONLAZY_PIC_BASE - On a symbol operand "FOO", this indicates
	/// that the reference is actually to "FOO$non_lazy_ptr - PICBASE", which is
	/// a PIC-base-relative reference to a non-hidden dyld lazy pointer stub.
	int MO_DARWIN_NONLAZY_PIC_BASE = 15;

	/// MO_DARWIN_HIDDEN_NONLAZY - On a symbol operand "FOO", this indicates
	/// that the reference is actually to the "FOO$non_lazy_ptr" symbol, which
	/// is a non-PIC-base-relative reference to a hidden dyld lazy pointer stub.
	int MO_DARWIN_HIDDEN_NONLAZY = 16;

	/// MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE - On a symbol operand "FOO", this
	/// indicates that the reference is actually to "FOO$non_lazy_ptr -PICBASE",
	/// which is a PIC-base-relative reference to a hidden dyld lazy pointer
	/// stub.
	int MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE = 17;
}
