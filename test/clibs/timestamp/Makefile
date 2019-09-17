include ../config/make.common
CFLAGS = -std=c99 -Wall -Wextra

all: test

check: test
	./test

test: test.c timestamp.c
	$(GCC) $^ -o $@ $(CFLAGS)

clean:
	rm -f test *.o *.s *.ll *.i

.PHONY: check clean
