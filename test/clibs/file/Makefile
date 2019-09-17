include ../config/make.common
SRCS=$(wildcard *.c src/*.c)
example: ${SRCS}
	$(GCC) $^ -o $@ -std=c99

clean:
	$(RM) example *.s *.ll *.o *.i src/*.s src/*.o src/*.ll src/*.i
.PHONY: example clean

