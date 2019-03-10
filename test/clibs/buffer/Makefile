include ../config/make.common

SRCS=$(wildcard *.c)
ASMS=$(SRCS:.c=.s)

all: clean buffer

buffer: $(ASMS)
	$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E $< > $@

clean:
	$(RM) buffer *.s *.ll *.o

