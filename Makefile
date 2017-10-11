SRC_DIR=java
OUT_DIR=out
CLASSES_DIR=$(OUT_DIR)/classes
GEN_TBLGEN_SRCS=$(OUT_DIR)/gen_tblgen_srcs/backend/target/x86
OUT_LIB_DIR=$(OUT_DIR)/lib
OUT_BIN_DIR=$(OUT_DIR)/bin
OUT_JAVADOC_DIR=$(OUT_DIR)/docs
XCC_JAR=xcc-0.1.jar
TARGET_SRCS=$(SRC_DIR)/backend/target/x86/*.java

LIB_DIR=lib
TROVE=$(LIB_DIR)/trove-3.0.3.jar

CPP=g++
JVM_SO=${JAVA_HOME}/jre/lib/amd64/server

SRCS=c++/NativeLauncher.h	\
	 c++/NativeLauncher.cpp	\

JFLAGS = -g -source 1.8 -target 1.8 -encoding UTF-8 -cp $(TROVE) -d $(CLASSES_DIR)
JC = javac
RM = rm

CLASSES_NO_TARGET = \
        java/backend/analysis/*.java \
        java/backend/codegen/*.java \
        java/backend/codegen/selectDAG/*.java \
        java/backend/heap/*.java \
        java/backend/hir/*.java \
        java/backend/intrinsic/* \
        java/backend/MC/*.java \
        java/backend/pass/*.java \
        java/backend/support/*.java \
        java/backend/target/*.java \
        java/backend/transform/*.java \
        java/backend/transform/scalars/*.java \
        java/backend/type/*.java \
        java/backend/utils/*.java \
        java/backend/value/*.java \
        java/jlang/*.java \
        java/jlang/*/*.java \
        java/tools/*/*.java \
        java/tools/*.java \
        java/utils/*/*.java \
        java/xcc/*.java

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
	java -cp $(TROVE):$(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-asm-printer -o     \
	$(GEN_TBLGEN_SRCS)/X86GenATTAsmPrinter.java              \

	java -cp $(TROVE):$(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-register-info -o   \
	$(GEN_TBLGEN_SRCS)/X86GenRegisterInfo.java               \

	java -cp $(TROVE):$(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-register-names -o  \
	$(GEN_TBLGEN_SRCS)/X86GenRegisterNames.java              \

	java -cp $(TROVE):$(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-instr-info -o \
	$(GEN_TBLGEN_SRCS)/X86GenInstrInfo.java                  \

	java -cp $(TROVE):$(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-fast-isel -o       \
	$(GEN_TBLGEN_SRCS)/X86GenFastISel.java                   \

	java -cp $(TROVE):$(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-instr-names -o     \
	$(GEN_TBLGEN_SRCS)/X86GenInstrNames.java                 \

	java -cp $(TROVE):$(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-subtarget -o       \
	$(GEN_TBLGEN_SRCS)/X86GenSubtarget.java                  \

	java -cp $(TROVE):$(CLASSES_DIR) utils.tablegen.TableGen \
	tds/X86/X86.td -I tds -I tds/X86 -gen-callingconv -o     \
	$(GEN_TBLGEN_SRCS)/X86GenCallingConv.java


classes: gen_tblgen
	$(JC) $(JFLAGS) -cp $(TROVE) -sourcepath $(SRC_DIR) $(GEN_TBLGEN_SRCS)/*.java \
	$(TARGET_SRCS)

jar: classes
	jar -cf $(OUT_LIB_DIR)/$(XCC_JAR) $(CLASSES_DIR)/
	cp -r $(LIB_DIR)/* $(OUT_LIB_DIR)

all: jar
	cd out; cmake ../c++ ; make all -j4

.pony: all
	
clean:
	$(RM) -r $(OUT_DIR)
