include ../config/make.common

all: strlen.o
	ar crs strlen.a $< 
%.o: %.c
	$(GCC) -c $< -o $@

clean:
	$(RM) *.s *.o *.ll *.i *.a
