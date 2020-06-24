include ../config/make.common

SRCS=test.c src/rgba.c

test: $(SRCS)
	@$(GCC) $^ -o $@

clean:
	$(RM) test *.o *.s *.ll *.i
.PHONY: test clean
