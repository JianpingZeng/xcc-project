set(t "RISCV")
set(GEN_TBLGEN_SRCS "${GEN_SOURCES_DIR}/backend/target/riscv")
file(GLOB_RECURSE TARGET_SRCS "${CMAKE_CURRENT_SOURCE_DIR}/*.java")

file(MAKE_DIRECTORY "${GEN_TBLGEN_SRCS}")
tablegen(RISCVGenRISCVAsmPrinter.java -gen-asm-printer)
tablegen(RISCVGenRegisterInfo.java -gen-register-info)
tablegen(RISCVGenRegisterNames.java -gen-register-names)
tablegen(RISCVGenInstrInfo.java -gen-instr-info)
tablegen(RISCVGenInstrNames.java -gen-instr-names)
tablegen(RISCVGenSubtarget.java -gen-subtarget)
tablegen(RISCVGenCallingConv.java -gen-callingconv)
TableGen(RISCVGenDAGISel.java -gen-dag-isel)

add_public_tablegen_target(RISCVCommonTableGen)

set(LOCAL_TABLEGEN_OUTPUT ${TABLEGEN_OUTPUT} PARENT_SCOPE)
set(LOCAL_TARGET_SRCS ${TARGET_SRCS} PARENT_SCOPE)
set(LOCAL_BACKEND_TARGET RISCVCommonTableGen PARENT_SCOPE)