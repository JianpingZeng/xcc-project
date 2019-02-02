include ../config/make.common
all: sds-test

sds-test: sds.s
	$(GCC) -o sds-test sds.s 
#	@echo ">>> Type ./sds-test to run the sds.c unit tests."
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E -Wall -std=c99 -pedantic -O2 -DSDS_TEST_MAIN $< > $@

clean:
	rm -f sds-test *.s *.o *.ll *.i
