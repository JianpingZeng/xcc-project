include ../config/make.common
CFLAGS = -std=c99 -Wall -Wextra -I../
SRCS=$(wildcard *.c */*/*.c)
ASMS=$(SRCS:.c=.s)

check: strsplit
	./test

strsplit: $(ASMS)
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
