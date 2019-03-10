CLANG=/home/xlous/Development/compiler/llvm/cmake-build-debug/bin/clang -emit-llvm -S -O2
LLC=/home/xlous/Development/compiler/xcc/out/bin/llc -O0

SRCS=$(wildcard *.c)
LLS = $(SRCS:.c=.ll)
ASMS=$(SRCS:.c=.s)
TARGET=bitcnts

all: $(TARGET) $(LLS)

$(TARGET): $(ASMS)
	gcc -O2 $^ -o $@

%.s: %.ll
	$(LLC) $< -o $@

%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	gcc -E $< > $@
clean:
	rm -rf $(TARGET) output* *.ll *.bc *.s *.o *.i
