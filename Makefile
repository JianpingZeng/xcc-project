SRC_DIR=java
OUT_DIR=out
CLASSES_DIR=$(OUT_DIR)/classes
GEN_TBLGEN_SRCS=$(OUT_DIR)/gen_tblgen_srcs/backend/target/x86
OUT_LIB_DIR=$(OUT_DIR)/lib
OUT_BIN_DIR=$(OUT_DIR)/bin
OUT_JAVADOC_DIR=$(OUT_DIR)/docs
XCC_JAR=xcc-0.1.jar
TARGET_SRCS=$(SRC_DIR)/backend/target/x86/*.java


CPP=g++
JVM_SO=${JAVA_HOME}/jre/lib/amd64/server

SRCS=c++/NativeLauncher.h	\
	 c++/NativeLauncher.cpp	\

JFLAGS = -g -source 1.8 -target 1.8 -encoding UTF-8 -d $(CLASSES_DIR)
JC = javac
RM = rm

CLASSES_NO_TARGET =                                         \
        java/backend/analysis/*.java                        \
        java/backend/codegen/*.java                         \
        java/backend/codegen/selectDAG/*.java               \
        java/backend/heap/*.java                            \
        java/backend/hir/*.java                             \
        java/backend/intrinsic/*                            \
        java/backend/MC/*.java                              \
        java/backend/pass/*.java                            \
        java/backend/support/*.java                         \
        java/backend/target/*.java                          \
        java/backend/transform/*.java                       \
        java/backend/transform/scalars/*.java               \
        java/backend/type/*.java                            \
        java/backend/utils/*.java                           \
        java/backend/value/*.java                           \
        java/jlang/*.java                                   \
        java/jlang/*/*.java                                 \
        java/tools/*/*.java                                 \
        java/tools/*.java                                   \
        java/utils/*/*.java                                 \
        java/xcc/*.java                                     \
        external/trove/gnu/trove/decorator/*.java           \
        external/trove/gnu/trove/function/*.java            \
        external/trove/gnu/trove/impl/hash/*.java           \
        external/trove/gnu/trove/impl/sync/*.java           \
        external/trove/gnu/trove/impl/unmodifiable/*.java   \
        external/trove/gnu/trove/impl/*.java                \
        external/trove/gnu/trove/iterator/*.java            \
        external/trove/gnu/trove/iterator/hash/*.java       \
        external/trove/gnu/trove/list/array/*.java          \
        external/trove/gnu/trove/list/linked/*.java         \
        external/trove/gnu/trove/list/*.java                \
        external/trove/gnu/trove/map/custom_hash/*.java     \
        external/trove/gnu/trove/map/hash/*.java            \
        external/trove/gnu/trove/map/*.java                 \
        external/trove/gnu/trove/procedure/array/*.java     \
        external/trove/gnu/trove/procedure/*.java           \
        external/trove/gnu/trove/queue/*.java               \
        external/trove/gnu/trove/set/*.java                 \
        external/trove/gnu/trove/set/hash/*.java            \
        external/trove/gnu/trove/stack/array/*.java         \
        external/trove/gnu/trove/stack/*.java               \
        external/trove/gnu/trove/strategy/*.java            \
        external/trove/gnu/trove/*.java

TARGETS = \
        java/backend/target/x86/*.java

init:
	mkdir -p $(OUT_DIR)
	mkdir -p $(CLASSES_DIR)
	mkdir -p $(GEN_TBLGEN_SRCS)
	mkdir -p $(OUT_LIB_DIR)
	mkdir -p $(OUT_BIN_DIR)
	mkdir -p $(OUT_JAVADOC_DIR)

classes_no_target: $(CLASSES_NO_TARGET) init
	$(JC) $(JFLAGS) $(CLASSES_NO_TARGET)

gen_tblgen: classes_no_target
	java -cp $(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-asm-printer -o     \
	$(GEN_TBLGEN_SRCS)/X86GenATTAsmPrinter.java              \

	java -cp $(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-register-info -o   \
	$(GEN_TBLGEN_SRCS)/X86GenRegisterInfo.java               \

	java -cp $(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-register-names -o  \
	$(GEN_TBLGEN_SRCS)/X86GenRegisterNames.java              \

	java -cp $(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-instr-info -o \
	$(GEN_TBLGEN_SRCS)/X86GenInstrInfo.java                  \

	java -cp $(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-fast-isel -o       \
	$(GEN_TBLGEN_SRCS)/X86GenFastISel.java                   \

	java -cp $(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-instr-names -o     \
	$(GEN_TBLGEN_SRCS)/X86GenInstrNames.java                 \

	java -cp $(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-subtarget -o       \
	$(GEN_TBLGEN_SRCS)/X86GenSubtarget.java                  \

	java -cp $(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-callingconv -o     \
	$(GEN_TBLGEN_SRCS)/X86GenCallingConv.java


classes: gen_tblgen
	$(JC) $(JFLAGS) -cp $(CLASSES_DIR) $(GEN_TBLGEN_SRCS)/*.java \
	$(TARGET_SRCS)

jar: classes
	cd $(CLASSES_DIR); \
	jar -cf ../../$(OUT_LIB_DIR)/$(XCC_JAR) * ;\
	cd ../../;

all: jar
	cd out; cmake ../cpp ; make all -j4

.pony: all
	
clean:
	$(RM) -r $(OUT_DIR)
