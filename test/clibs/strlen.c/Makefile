include ../config/make.common

all: strlen.o
	ar crs strlen.a $< 
%.o: %.s
	$(GCC) -c $< -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) $< -E > $@

clean:
	$(RM) *.s *.o *.ll *.i *.a
