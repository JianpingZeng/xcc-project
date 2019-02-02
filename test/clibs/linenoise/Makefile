include ../config/make.common

SRCS=$(wildcard *.c)
ASMS=$(SRCS:.c=.s)

linenoise_example: $(ASMS)
	$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E $< > $@

clean:
	rm -f linenoise_example *.s *.o *.ll *.i
