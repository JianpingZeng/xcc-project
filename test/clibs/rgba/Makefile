include ../config/make.common

SRCS=test.c src/rgba.c
ASMS=$(SRCS:.c=.s)

test: $(ASMS)
	@$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E $< > $@

clean:
	$(RM) test *.o *.s *.ll *.i
.PHONY: test clean
