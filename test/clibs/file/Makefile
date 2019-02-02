include ../config/make.common
example: example.s src/file.s
	$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@ -std=c99
%.i: %.c
	$(GCC) -E -std=c99 $< > $@

clean:
	$(RM) example *.s *.ll *.o *.i 
.PHONY: example clean

