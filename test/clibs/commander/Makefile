include ../config/make.common

test: test.c src/commander.c
	$(GCC) $^ -o $@ -std=c99

clean:
	rm -f test *.s *.o *.ll *.i

.PHONY: clean
