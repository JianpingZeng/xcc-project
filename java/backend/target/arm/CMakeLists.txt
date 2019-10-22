set(t "ARM")
set(GEN_TBLGEN_SRCS "${GEN_SOURCES_DIR}/backend/target/arm")
file(GLOB_RECURSE TARGET_SRCS "${CMAKE_CURRENT_SOURCE_DIR}/*.java")

file(MAKE_DIRECTORY "${GEN_TBLGEN_SRCS}")
tablegen(ARMGenInstPrinter.java -gen-asm-printer)
tablegen(ARMGenRegisterInfo.java -gen-register-info)
tablegen(ARMGenRegisterNames.java -gen-register-names)
tablegen(ARMGenInstrInfo.java -gen-instr-info)
tablegen(ARMGenInstrNames.java -gen-instr-names)
#tablegen(ARMGenSubtarget.java -gen-subtarget)
#tablegen(ARMGenCallingConv.java -gen-callingconv)
TableGen(ARMGenDAGISel.java -gen-dag-isel)

add_public_tablegen_target(ARMCommonTableGen)

set(LOCAL_TABLEGEN_OUTPUT ${TABLEGEN_OUTPUT} PARENT_SCOPE)
set(LOCAL_TARGET_SRCS ${TARGET_SRCS} PARENT_SCOPE)
set(LOCAL_BACKEND_TARGET ARMCommonTableGen PARENT_SCOPE)