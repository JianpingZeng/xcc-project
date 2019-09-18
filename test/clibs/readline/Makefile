include ../config/make.common

CFLAGS = -O0 -g -Wall -Wextra -pedantic-errors
SRCS=$(wildcard *.c)

all: test

.PHONY: test

test: $(SRCS)
	$(GCC) $^ -o $@ $(CFLAGS)

clean:
	rm -f test *.s *.ll *.i

