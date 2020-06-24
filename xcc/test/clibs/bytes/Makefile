include ../config/make.common
SRC = bytes.c test.c
ASMS = $(SRC:.c=.s)

test: $(SRC)
	$(GCC) $^ -o $@

clean:
	rm -f *.s *.ll *.i *.bc *.o test

.PHONY: test clean
