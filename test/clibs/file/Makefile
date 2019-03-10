include ../config/make.common
example: example.s src/file.s
	$(GCC) $^ -o $@

%.s: %.c
	$(GCC) -E $< > $(<:%.c=%.i) -std=c99
	$(CLANG) $(<:%.c=%.i) -o $(<:%.c=%.ll) -std=c99
	$(LLC) $(<:%.c=%.ll) -o $(<:%.c=%.s)

clean:
	$(RM) example *.s *.ll *.o *.i src/*.s src/*.o src/*.ll src/*.i
.PHONY: example clean

