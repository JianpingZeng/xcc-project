cmake_minimum_required(VERSION 2.8)
project(Jlang CXX)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../out/bin")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../out/lib")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../out/lib")

if (CMAKE_BUILD_TYPE EQUAL "DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Og -Wall")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3 -Wall")
endif ()

if (APPLE)
    execute_process(COMMAND /usr/libexec/java_home OUTPUT_VARIABLE JDK_HOME)
    string(REGEX REPLACE "\n$" "" JDK_HOME "${JDK_HOME}")
    set(JAVA_HOME ${JDK_HOME})
    set(JDK_LIBS ${JDK_HOME}/jre/lib/server)
    set(JDK_INCLUDES ${JDK_HOME}/include ${JDK_HOME}/include/darwin)
elseif(LINUX)
    set(JDK_LIBS ${JAVA_HOME}/jre/lib/amd64/server)
    set(JDK_INCLUDES ${JDK_HOME}/include ${JDK_HOME}/include/darwin)
endif ()

message(STATUS "JAVA_HOME: ${JAVA_HOME}")
message(STATUS "JDK_LIBS: ${JDK_LIBS}")
message(STATUS "JDK_INCLUDES: ${JDK_INCLUDES}")

link_directories(${JDK_LIBS})
include_directories(${JDK_INCLUDES})

set(COM_SRCS NativeLauncher.cpp)
set(JLANG_SRCS ${COM_SRCS} Jlang.cpp)
set(JLANG_CC_SRCS ${COM_SRCS} JlangCC.cpp)
set(TABLEGEN_SRCS ${COM_SRCS} TableGen.cpp)

add_executable(jlang ${JLANG_SRCS})
add_executable(jlang-cc ${JLANG_CC_SRCS})
add_executable(tablegen ${TABLEGEN_SRCS})

target_link_libraries(jlang jvm)
target_link_libraries(jlang-cc jvm)
target_link_libraries(tablegen jvm)