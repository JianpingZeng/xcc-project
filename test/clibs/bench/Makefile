include ../config/make.common

OS = $(shell uname)
LDFLAGS =

ifneq ($(OS), Darwin)
	LDFLAGS += -lrt
endif

all: clean test

clean:
	rm -f bench *.s *.ll *.i

test: test.s
	$(GCC) $^ -o bench $(LDFLAGS)
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E $< > $@

.PHONY: all clean test
