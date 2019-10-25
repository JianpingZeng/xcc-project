set(t "X86")
set(GEN_TBLGEN_SRCS "${GEN_SOURCES_DIR}/backend/target/x86")
file(GLOB_RECURSE TARGET_SRCS "${CMAKE_CURRENT_SOURCE_DIR}/*.java")

file(MAKE_DIRECTORY "${GEN_TBLGEN_SRCS}")
tablegen(X86GenATTInstPrinter.java -gen-asm-printer)
tablegen(X86GenRegisterInfo.java -gen-register-info)
tablegen(X86GenRegisterNames.java -gen-register-names)
tablegen(X86GenInstrInfo.java -gen-instr-info)
tablegen(X86GenInstrNames.java -gen-instr-names)
tablegen(X86GenSubtarget.java -gen-subtarget)
tablegen(X86GenCallingConv.java -gen-callingconv)
tablegen(X86GenDAGISel.java -gen-dag-isel)

add_public_tablegen_target(X86CommonTableGen)

# Compiling collection of generated java source code by TableGen tool.
macro(compile_x86_srcs target_name)
    add_custom_target(${target_name}
    COMMAND ${JAVA_COMPILE} -cp ${CLASSES_DIR} ${JFLAGS}
    ${TABLEGEN_OUTPUT}
    ${TARGET_SRCS}
    DEPENDS ${BACKEND_TARGET} X86CommonTableGen
    COMMENT "Building X86 backend")
endmacro(compile_x86_srcs)

compile_x86_srcs(X86Backend)
set(BACKEND_TARGET ${BACKEND_TARGET} X86Backend PARENT_SCOPE)
