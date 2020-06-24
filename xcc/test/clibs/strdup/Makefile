include ../config/make.common
CFLAGS = -std=c99 -Wall -Wextra

check: test
	./test

test: strdup.c test.c
	$(GCC) $^ -o $@ $(CFLAGS)

clean:
	rm -f test *.s *.ll *.i *.o

.PHONY: check clean
