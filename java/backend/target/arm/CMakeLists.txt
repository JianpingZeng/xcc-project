set(t "ARM")
set(GEN_TBLGEN_SRCS "${GEN_SOURCES_DIR}/backend/target/arm")
file(GLOB_RECURSE TARGET_SRCS "${CMAKE_CURRENT_SOURCE_DIR}/*.java")

file(MAKE_DIRECTORY "${GEN_TBLGEN_SRCS}")
tablegen(ARMGenInstPrinter.java -gen-asm-printer)
tablegen(ARMGenRegisterInfo.java -gen-register-info)
tablegen(ARMGenRegisterNames.java -gen-register-names)
tablegen(ARMGenInstrInfo.java -gen-instr-info)
tablegen(ARMGenInstrNames.java -gen-instr-names)
tablegen(ARMGenSubtarget.java -gen-subtarget)
tablegen(ARMGenCallingConv.java -gen-callingconv)
tablegen(ARMGenDAGISel.java -gen-dag-isel)

add_public_tablegen_target(ARMCommonTableGen)

# Compiling collection of generated java source code by TableGen tool.
macro(compile_arm_srcs target_name)
	add_custom_target(${target_name}
	COMMAND ${JAVA_COMPILE} -cp ${CLASSES_DIR} ${JFLAGS}
	${TABLEGEN_OUTPUT}
	${TARGET_SRCS}
	DEPENDS ${BACKEND_TARGET} ARMCommonTableGen
	COMMENT "Building ARM backend")
endmacro(compile_arm_srcs)

compile_arm_srcs(ARMBackend)
set(BACKEND_TARGET ${BACKEND_TARGET} ARMBackend PARENT_SCOPE)
