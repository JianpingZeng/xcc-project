cmake_minimum_required(VERSION 2.8)

if(NOT CMAKE_java_COMPILER)
    if (Apple)
        execute_process(COMMAND /usr/libexec/java_home OUTPUT_VARIABLE CMAKE_java_COMPILER)
    endif()
endif()

project(Jlang)

find_package(Java REQUIRED)
#find_package(JNI REQUIRED)
include(UseJava)

set(SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/java)
set(OUT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/out)
set(CLASSES_DIR ${OUT_DIR}/classes)
set(OUT_LIB_DIR ${OUT_DIR}/lib)
set(OUT_BIN_DIR ${OUT_DIR}/bin)
set(OUT_JAVADOC_DIR ${OUT_DIR}/docs)
set(XCC_JAR xcc-0.1.jar)
set(GEN_TBLGEN_SRCS ${OUT_DIR}/gen_tblgen_srcs/backend/target/x86)

file(GLOB TARGET_SRCS
    ${SRC_DIR}/backend/target/x86/*.java)

file(MAKE_DIRECTORY ${OUT_LIB_DIR})
#file(MAKE_DIRECTORY ${OUT_JAVADOC})

set(JFLAGS -g -source 1.8 -target 1.8 -encoding UTF-8 -d ${CLASSES_DIR})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/out/bin")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/out/lib")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/out/lib")

file(GLOB_RECURSE SRCS_NO_TARGET "${CMAKE_CURRENT_SOURCE_DIR}/java/*.java")

set(EXCLUDE_TARGET ${CMAKE_CURRENT_SOURCE_DIR}/java/backend/target/x86)
#message(STATUS "${SRCS_NO_TARGET}")

# Remove target-specific source files.
foreach (file ${SRCS_NO_TARGET})
    if ("${file}" MATCHES "(.*)${EXCLUDE_TARGET}(.*)")
        #message(STATUS "Remove Item from List:${file}")
        list (REMOVE_ITEM SRCS_NO_TARGET ${file})
    endif ()
endforeach()

#message(STATUS "${SRCS_NO_TARGET}")
#file(GLOB_RECURSE TROVE_SRCS "${CMAKE_CURRENT_SOURCE_DIR}/external/trove/*.java")
file(GLOB TROVE_SRCS
    external/trove/gnu/trove/*.java
    external/trove/gnu/trove/decorator/*.java
    external/trove/gnu/trove/function/*.java
    external/trove/gnu/trove/impl/*.java
    external/trove/gnu/trove/impl/hash/*.java
    external/trove/gnu/trove/impl/sync/*.java
    external/trove/gnu/trove/impl/unmodifiable/*.java
    external/trove/gnu/trove/iterator/*.java
    external/trove/gnu/trove/iterator/hash/*.java
    external/trove/gnu/trove/list/*.java
    external/trove/gnu/trove/list/array/*.java
    external/trove/gnu/trove/list/linked/*.java
    external/trove/gnu/trove/map/*.java
    external/trove/gnu/trove/map/custom_hash/*.java
    external/trove/gnu/trove/map/hash/*.java
    external/trove/gnu/trove/procedure/*.java
    external/trove/gnu/trove/procedure/array/*.java
    external/trove/gnu/trove/queue/*.java
    external/trove/gnu/trove/set/*.java
    external/trove/gnu/trove/set/hash/*.java
    external/trove/gnu/trove/stack/*.java
    external/trove/gnu/trove/stack/array/*.java
    external/trove/gnu/trove/strategy/*.java
    )

file(MAKE_DIRECTORY ${CLASSES_DIR})

# Custom target for compiling jlang/backend source files except for
# target-specific codes.
add_custom_target(classes_no_target
    COMMAND ${JAVA_COMPILE} ${JFLAGS} ${TROVE_SRCS} ${SRCS_NO_TARGET}
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/java)

# A macro definition for generating target-specific AsmPrinter, registerInfo
# information etc.
file(MAKE_DIRECTORY ${GEN_TBLGEN_SRCS})
macro(tablegen ofn)
  set(local_tds "${CMAKE_CURRENT_SOURCE_DIR}/tds ${CMAKE_CURRENT_SOURCE_DIR}/tds/X86")
  add_custom_command(OUTPUT ${GEN_TBLGEN_SRCS}/${ofn}
    COMMAND java -cp ${CLASSES_DIR} utils.tablegen.TableGen ${ARGN} -I ${local_tds}
    ${CMAKE_CURRENT_SOURCE_DIR}/tds/X86/X86.td -o ${GEN_TBLGEN_SRCS}/${ofn}
    DEPENDS classes_no_target
    COMMENT "Building ${ofn}...")
    set(TABLEGEN_OUTPUT ${TABLEGEN_OUTPUT} ${GEN_TBLGEN_SRCS}/${ofn})
endmacro(tablegen)

tablegen(X86GenATTAsmPrinter.java -gen-asm-printer)
tablegen(X86GenRegisterInfo.java -gen-register-info)
tablegen(X86GenRegisterNames.java -gen-register-names)
tablegen(X86GenInstrInfo.java -gen-instr-info)
tablegen(X86GenFastISel.java -gen-fast-isel)
tablegen(X86GenInstrNames.java -gen-instr-names)
tablegen(X86GenSubtarget.java -gen-subtarget)
tablegen(X86GenCallingConv.java -gen-callingconv)

macro(add_gen_tblgen target_name)
    if (TABLEGEN_OUTPUT)
        add_custom_target(${target_name}
            COMMAND ${JAVA_COMPILE} -cp ${CLASSES_DIR} ${JFLAGS} ${TABLEGEN_OUTPUT} ${TARGET_SRCS}
        DEPENDS ${TABLEGEN_OUTPUT}
        COMMENT "Compiling taraget-specific source files")
    else()
        message(FATAL "Generates target-specific code failed according to td file")
    endif(TABLEGEN_OUTPUT)
endmacro(add_gen_tblgen)

#message(STATUS "TABLEGEN_OUTPUT: ${TABLEGEN_OUTPUT}")
add_gen_tblgen(CompileTableGen)

add_custom_target(jar ALL
    COMMAND ${JAVA_ARCHIVE} -cf ${CMAKE_CURRENT_BINARY_DIR}/lib/${XCC_JAR} -C ${CLASSES_DIR} .
    DEPENDS CompileTableGen
    WORKING_DIRECTORY ${CLASSES_DIR}
    COMMENT "Building jar")

if (NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "DEBUG")
endif()

if (CMAKE_BUILD_TYPE EQUAL "DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Og -Wall")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3 -Wall")
endif ()

if(APPLE)
    execute_process(COMMAND /usr/libexec/java_home OUTPUT_VARIABLE JDK_HOME)
    string(REGEX REPLACE "\n$" "" JDK_HOME "${JDK_HOME}")
    set(JAVA_HOME ${JDK_HOME})
    set(JDK_LIBS ${JDK_HOME}/jre/lib/server)
    set(JDK_INCLUDES ${JDK_HOME}/include ${JDK_HOME}/include/darwin)
elseif(CMAKE_SYSTEM_NAME MATCHES "Linux")
    set(JDK_HOME "$ENV{JAVA_HOME}")
    string(REGEX REPLACE "\n$" "" JDK_HOME "${JDK_HOME}")
    set(JDK_LIBS ${JDK_HOME}/jre/lib/amd64/server)
    set(JDK_INCLUDES ${JDK_HOME}/include ${JDK_HOME}/include/linux)
endif ()

message(STATUS "JDK_HOME: ${JDK_HOME}")
message(STATUS "JDK_LIBS: ${JDK_LIBS}")
message(STATUS "JDK_INCLUDES: ${JDK_INCLUDES}")

link_directories(${JDK_LIBS})
include_directories(${JDK_INCLUDES})

set(COM_SRCS ${CMAKE_CURRENT_SOURCE_DIR}/cpp/NativeLauncher.cpp)
set(JLANG_SRCS ${COM_SRCS} ${CMAKE_CURRENT_SOURCE_DIR}/cpp/Jlang.cpp)
set(JLANG_CC_SRCS ${COM_SRCS} ${CMAKE_CURRENT_SOURCE_DIR}/cpp/JlangCC.cpp)
set(TABLEGEN_SRCS ${COM_SRCS} ${CMAKE_CURRENT_SOURCE_DIR}/cpp/TableGen.cpp)

macro(add_jlang_target name)
    add_executable(${name} ${ARGN})
    add_dependencies(${name} jar)
endmacro()

add_jlang_target(jlang ${JLANG_SRCS})
add_jlang_target(jlang-cc ${JLANG_CC_SRCS})
add_jlang_target(tablegen ${TABLEGEN_SRCS})


target_link_libraries(jlang jvm)
target_link_libraries(jlang-cc jvm)
target_link_libraries(tablegen jvm)