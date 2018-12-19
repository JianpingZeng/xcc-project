CC=/home/xlous/Development/compiler/llvm-6.0.0/cmake-build-debug/bin/clang -cc1
#CC= /home/xlous/Development/experiment/llvm-3.1/bin/clang -cc1 
#CC=clang -cc1

IDEM_CONSTRUCTION = -idempotence-construction=size
IDEM_FLAGS = $(IDEM_CONSTRUCTION) $(IDEM_PRESERVE)

CFLAGS= -O2 -funroll-loops -mllvm $(IDEM_FLAGS) -triple x86_64-linux-gnu -emit-obj -disable-free -disable-llvm-verifier -mrelocation-model static -mdisable-fp-elim -fmath-errno -masm-verbose -mconstructor-aliases -munwind-tables -ferror-limit 19 -fmessage-length 204 -fdiagnostics-show-option

#LDFLAGS= -O2 -static --target=armv7
#GCC= /home/xlous/Development/experiment/arm-2010.09/bin/arm-none-linux-gnueabi-gcc
GCC=clang

FILE1 = qsort_small.c
FILE2 = qsort_large.c

OBJS = qsort_small.o qsort_large.o
PREDS = qsort_small.i qsort_large.i

all: qsort_small qsort_large

qsort_small: qsort_small.o
	$(GCC) ${LDFLAGS} $< -o $@ -lm
qsort_large: qsort_large.o
	$(GCC) ${LDFLAGS} $< -o $@ -lm

$(OBJS): $(PREDS)
	$(CC) $(CFLAGS) $^ 
	
%.i: %.c
	$(GCC) -E $< > $@

clean:
	rm -rf qsort_small qsort_large output* *.s *.bc *.ll *.o *.i
