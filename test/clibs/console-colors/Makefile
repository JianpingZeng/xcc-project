include ../config/make.common

all: example

example: console-colors.s test/example.s
	$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E $< > $@

clean:
	$(RM) console-colors.s test/example.s example
	
