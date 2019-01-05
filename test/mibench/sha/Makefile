# By default, the code is compiled for a "big endian" machine.
# To compile on a "little endian" machine set the LITTLE_ENDIAN flag.
# To make smaller object code, but run a little slower, don't use UNROLL_LOOPS.
# To use NIST's modified SHA of 7/11/94, define USE_MODIFIED_SHA
#CC=/home/xlous/Development/compiler/llvm-idem/llvm/cmake-build-debug/bin/clang -cc1	
#CC= /home/xlous/Development/experiment/llvm-3.1/bin/clang -cc1 
CC=/home/xlous/Development/compiler/llvm/cmake-build-debug/bin/clang -cc1 -emit-llvm -O2
LLC=/home/xlous/Development/compiler/xcc/out/bin/llc -O0

ASMS = sha.s sha_driver.s
PREDS = sha.i sha_driver.i
TARGET = sha

$(TARGET): $(ASMS)
	gcc $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CC) $< -o $@
%.i: %.c
	gcc -E $< > $@

clean:
	rm -rf $(TARGET) output* *.bc *.s *.ll *.o *.i
