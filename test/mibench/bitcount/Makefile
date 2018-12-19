#CLANG=/home/xlous/Development/compiler/llvm-6.0.0/cmake-build-debug/bin/clang -cc1
CLANG=/home/xlous/Development/compiler/bolt/cmake-build-debug/bin/clang -cc1

#LLC=/home/xlous/Development/compiler/llvm-6.0.0/cmake-build-debug/bin/llc
LLC=/home/xlous/Development/compiler/bolt/cmake-build-debug/bin/llc

#IDEM_FLAGS = -idempotence-construction=size -idempotence-preservation=vcf
#IDEM_PRESERVE = -livein-checkpoint=cp
IDEM_CONSTRUCTION = -idempotence-construction=size
IDEM_FLAGS = $(IDEM_CONSTRUCTION) $(IDEM_PRESERVE)

#TRIPLE=armv7-linux-gnu
TRIPLE = x86_64-linux-gnu

OPT_LEVEL = -O2
CFLAGS= $(OPT_LEVEL) -emit-llvm -triple $(TRIPLE) -disable-free -disable-llvm-verifier -mrelocation-model static -mdisable-fp-elim -fmath-errno -masm-verbose -mconstructor-aliases -munwind-tables -ferror-limit 19 -fmessage-length 204 -fdiagnostics-show-option -fcolor-diagnostics

LDFLAGS= $(OPT_LEVEL) -static 
#GCC= /home/xlous/Development/experiment/arm-2010.09/bin/arm-none-linux-gnueabi-gcc
GCC=gcc

OBJS = bitarray.o bitcnt_1.o bitcnt_2.o bitcnt_3.o bitcnt_4.o bitcnts.o bitfiles.o bitstrng.o bstr_i.o
PREDS = $(OBJS:.o=.i)
LLS = $(OBJS:.o=.ll)

TARGET = bitcnts

$(TARGET): $(OBJS)
	$(GCC) ${LDFLAGS} $(OBJS) -o $(TARGET)

$(LLS): $(PREDS)
	$(CLANG) $(CFLAGS) $^

%.o: %.ll
	$(LLC) $(IDEM_FLAGS) $(OPT_LEVEL) -mtriple=$(TRIPLE) -filetype=obj $< -o $@

#$(OBJS): $(PREDS)
#	$(CC) $(CFLAGS) $^	

%.i: %.c
	$(GCC) -E $< > $@

clean:
	rm -rf $(TARGET) output* *.ll *.bc *.s *.o *.i
