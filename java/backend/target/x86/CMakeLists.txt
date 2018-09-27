set(t "X86")
set(GEN_TBLGEN_SRCS "${GEN_SOURCES_DIR}/backend/target/x86" PARENT_SCOPE)
file(GLOB_RECURSE TARGET_SRCS "${CMAKE_CURRENT_SOURCE_DIR}/*.java")

file(MAKE_DIRECTORY "${GEN_TBLGEN_SRCS}")
tablegen(X86GenATTAsmPrinter.java -gen-asm-printer)
tablegen(X86GenRegisterInfo.java -gen-register-info)
tablegen(X86GenRegisterNames.java -gen-register-names)
tablegen(X86GenInstrInfo.java -gen-instr-info)
tablegen(X86GenInstrNames.java -gen-instr-names)
tablegen(X86GenSubtarget.java -gen-subtarget)
tablegen(X86GenCallingConv.java -gen-callingconv)
TableGen(X86GenDAGToDAGISel.java -gen-dag-isel)

add_public_tablegen_target(X86CommonTableGen)

set(LOCAL_TABLEGEN_OUTPUT ${TABLEGEN_OUTPUT} PARENT_SCOPE)
set(LOCAL_TARGET_SRCS ${TARGET_SRCS} PARENT_SCOPE)
set(LOCAL_BACKEND_TARGET RISCVCommonTableGen PARENT_SCOPE)