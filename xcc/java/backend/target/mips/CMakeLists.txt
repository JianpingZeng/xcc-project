set(t "Mips")
set(GEN_TBLGEN_SRCS "${GEN_SOURCES_DIR}/backend/target/mips")
file(GLOB_RECURSE TARGET_SRCS "${CMAKE_CURRENT_SOURCE_DIR}/*.java")

file(MAKE_DIRECTORY "${GEN_TBLGEN_SRCS}")
tablegen(MipsGenInstPrinter.java -gen-asm-printer)
tablegen(MipsGenRegisterInfo.java -gen-register-info)
tablegen(MipsGenRegisterNames.java -gen-register-names)
tablegen(MipsGenInstrInfo.java -gen-instr-info)
tablegen(MipsGenInstrNames.java -gen-instr-names)
tablegen(MipsGenSubtarget.java -gen-subtarget)
tablegen(MipsGenCallingConv.java -gen-callingconv)
tablegen(MipsGenDAGISel.java -gen-dag-isel)

add_public_tablegen_target(MipsCommonTableGen)

# Compiling collection of generated java source code by TableGen tool.
macro(compile_mips_srcs target_name)
	add_custom_target(${target_name}
	COMMAND ${JAVA_COMPILE} -cp ${CLASSES_DIR} ${JFLAGS}
	${TABLEGEN_OUTPUT}
	${TARGET_SRCS}
	DEPENDS ${BACKEND_TARGET} MipsCommonTableGen
	COMMENT "Building Mips backend")
endmacro(compile_mips_srcs)

compile_mips_srcs(MipsBackend)
set(BACKEND_TARGET ${BACKEND_TARGET} MipsBackend PARENT_SCOPE)
