CLANG=/home/xlous/Development/compiler/llvm/cmake-build-debug/bin/clang -cc1 -emit-llvm -O2
LLC=/home/xlous/Development/compiler/xcc/out/bin/llc

TARGET=qsort_small qsort_large 

all: $(TARGET)

qsort_small: qsort_small.s
	gcc -O2 $< -o $@
qsort_large: qsort_large.s
	gcc -O2 $< -o $@

%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	gcc -E $< > $@

clean:
	rm -rf $(TARGET) output* *.ll *.bc *.s *.o *.i
