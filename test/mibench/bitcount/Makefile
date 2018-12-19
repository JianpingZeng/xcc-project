CLANG=/home/xlous/Development/compiler/llvm/cmake-build-debug/bin/clang -cc1 -emit-llvm -O2
LLC=/home/xlous/Development/compiler/xcc/out/bin/llc

SRCS=$(wildcard *.c)
PREDS = $(SRCS:.c=.i)
LLS = $(SRCS:.c=.ll)
ASMS=$(SRCS:.c=.s)

all: $(ASMS) $(LLS) $(PREDS)

%.s: %.ll
	$(LLC) $< -o $@

%.ll: %.i
	$(CLANG) $< -o $@

%.i: %.c
	gcc -E $< > $@

clean:
	rm -rf $(TARGET) output* *.ll *.bc *.s *.o *.i
