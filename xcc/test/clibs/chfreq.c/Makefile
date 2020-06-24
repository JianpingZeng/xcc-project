include ../config/make.common
BIN=chfreq
all: $(BIN) test

$(BIN): main.c chfreq.c
	$(GCC) $^ -o $@ -std=c99

test: test.c chfreq.c
	$(GCC) $^ -o $@ -std=c99

clean:
	rm -f test $(BIN) *.s *.i *.ll *.o

.PHONY: test
