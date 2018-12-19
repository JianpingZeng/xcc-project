#CC=/home/xlous/Development/compiler/llvm-idem/llvm/cmake-build-debug/bin/clang -cc1
#CC= /home/xlous/Development/experiment/llvm-3.1/bin/clang -cc1 
CC=/home/xlous/Development/compiler/llvm-6.0.0/cmake-build-debug/bin/clang -cc1

#IDEMFLAG = -mllvm -enable-reg-renaming-idem=true
#IDEMFLAG = -mllvm -idempotence-construction=size -mllvm -idempotence-preservation=vcf

CFLAGS= -O2 -funroll-loops $(IDEMFLAG) -triple x86_64-linux-gnu -emit-obj -disable-free -disable-llvm-verifier -mrelocation-model static -mdisable-fp-elim -fmath-errno -masm-verbose -mconstructor-aliases -munwind-tables -ferror-limit 19 -fmessage-length 204 -fdiagnostics-show-option

#LDFLAGS= -O2 -static --target=armv7
#GCC= /home/xlous/Development/experiment/arm-2010.09/bin/arm-none-linux-gnueabi-gcc
GCC=clang

OBJS = susan.o
PREDS = susan.i
TARGET = susan

$(TARGET): $(OBJS)
	$(GCC) ${LDFLAGS} $(OBJS) -o $@ -lm

$(OBJS): $(PREDS)
	$(CC) $(CFLAGS) $^	

%.i: %.c
	$(GCC) -E $< > $@

clean:
	rm -rf $(TARGET) output* *.bc *.s *.ll *.o *.i
