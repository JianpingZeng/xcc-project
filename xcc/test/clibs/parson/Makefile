include ../config/make.common

CFLAGS = -O0 -g -Wall -Wextra -std=c89 -pedantic-errors
all: test

.PHONY: test

test: tests.c parson.c
	$(GCC) $^ -o $@ $(CFLAGS)

clean:
	rm -f test *.s *.ll *.i

