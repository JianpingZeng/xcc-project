include ../config/make.common
CFLAGS = -std=c99 -Wall -Wextra

all: test

check: test
	./test

test: test.s timestamp.s
	$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@ $(CFLAGS)
%.i: %.c
	$(GCC) $< -E $(CFLAGS) > $@

clean:
	rm -f test *.o *.s *.ll *.i

.PHONY: check clean
