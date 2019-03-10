include ../config/make.common
BIN=chfreq
all: $(BIN) test

$(BIN): main.s chfreq.s
	$(GCC) $^  -o $@

test: test.s chfreq.s
	$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i 
	$(CLANG) $< -o $@
%.i: %.c 
	$(GCC) -E $< > $@

clean:
	rm -f test $(BIN) *.s *.i *.ll

.PHONY: test
