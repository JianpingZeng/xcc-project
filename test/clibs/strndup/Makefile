include ../config/make.common
CFLAGS = -std=c99 -Wall -Wextra

all: test

test: strndup.s test.s
	$(GCC) $^ -o $@

%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c	
	$(GCC) $< -E $(CFLAGS) > $@

clean:
	rm -f test *.s *.o *.ll *.i

.PHONY: check clean
