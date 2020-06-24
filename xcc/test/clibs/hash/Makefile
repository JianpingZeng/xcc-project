include ../config/make.common

test: hash.c
	$(GCC) $^ -o $@ -std=c99 -DTEST_HASH

clean:
	rm -f test *.s *.ll *.o *.i

.PHONY: test clean
