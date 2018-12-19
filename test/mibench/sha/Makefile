# By default, the code is compiled for a "big endian" machine.
# To compile on a "little endian" machine set the LITTLE_ENDIAN flag.
# To make smaller object code, but run a little slower, don't use UNROLL_LOOPS.
# To use NIST's modified SHA of 7/11/94, define USE_MODIFIED_SHA
#CC=/home/xlous/Development/compiler/llvm-idem/llvm/cmake-build-debug/bin/clang -cc1	
#CC= /home/xlous/Development/experiment/llvm-3.1/bin/clang -cc1 
CC=/home/xlous/Development/compiler/llvm-6.0.0/cmake-build-debug/bin/clang -cc1
#CC=clang -cc1

#IDEMFLAG = -mllvm -enable-reg-renaming-idem=true
#IDEMFLAG = -mllvm -idempotence-construction=size -mllvm -idempotence-preservation=vcf

#IDEM_CONSTRUCTION = -idempotence-construction=size
#IDEM_FLAGS = $(IDEM_CONSTRUCTION) $(IDEM_PRESERVE)

CFLAGS= -O3 -funroll-loops $(IDEM_FLAGS) -triple=x86_64-linux-gnu -emit-obj -disable-free -disable-llvm-verifier -mrelocation-model static -mdisable-fp-elim -fmath-errno -masm-verbose -mconstructor-aliases -munwind-tables -ferror-limit 19 -fmessage-length 204 -fdiagnostics-show-option

#LDFLAGS= -O3 -static --target=armv7
#GCC= /home/xlous/Development/experiment/arm-2010.09/bin/arm-none-linux-gnueabi-gcc
GCC = clang

OBJS = sha.o sha_driver.o
PREDS = sha.i sha_driver.i
TARGET = sha

$(TARGET): $(OBJS)
	$(GCC) ${LDFLAGS} $(OBJS) -o $@

$(OBJS): $(PREDS)
	$(CC) $(CFLAGS) $^	

%.i: %.c
	$(GCC) -E $< > $@

clean:
	rm -rf $(TARGET) output* *.bc *.s *.ll *.o *.i
