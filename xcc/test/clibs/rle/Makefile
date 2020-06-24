include ../config/make.common
SRC = $(wildcard deps/*/*.c)
SRC += rle.c rle_test.c

CFLAGS = -std=c99 -I deps -Wall

check: test
	@./test

test: $(SRC)
	@$(GCC) $^ -o $@ $(CFLAGS)

clean:
	@rm -fr test *.dSYM *.o *.s *.ll *.i
.PHONY: clean
