include ../config/make.common
SRC = $(wildcard deps/*/*.c)
SRC += flag.c
ASMS=$(SRC:.c=.s)
CFLAGS = -std=c99 -I deps

all: test ${LLS}

test: $(SRC) flag_test.c
	$(GCC) $^ -o $@ $(CFLAGS)

example: $(SRC) flag_example.c
	$(GCC) $^ -o $@ $(CFLAGS)

clean:
	rm -fr test example *.dSYM *.s *.o *.ll *.s *.i

.PHONY: clean
