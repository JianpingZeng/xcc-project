include ../config/make.common
all: sds-test

sds-test: sds.c
	$(GCC) -o $@ $^ -Wall -std=c99 -pedantic -O2 -DSDS_TEST_MAIN

clean:
	rm -f sds-test *.s *.o *.ll *.i
