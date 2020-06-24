include ../config/make.common
CFLAGS = -std=c99 -Wall -Wextra

all: test

test: strndup.c test.c
	$(GCC) $^ -o $@ $(CFLAGS)

clean:
	rm -f test *.s *.o *.ll *.i

.PHONY: check clean
