include ../config/make.common
SRC = $(wildcard deps/*/*.c)
SRC += flag.c
ASMS=$(SRC:.c=.s)
CFLAGS = -std=c99 -I deps -g

all: test

test: $(ASMS) flag_test.s
	@$(GCC) $^ -o $@

example: $(SRC) flag_example.s
	@$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@ $(CFLAGS)
%.i: %.c
	$(GCC) -E $(CFLAGS) $< > $@

clean:
	rm -fr test example *.dSYM *.s *.o *.ll *.s

.PHONY: clean
