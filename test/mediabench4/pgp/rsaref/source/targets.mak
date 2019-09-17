# Dependencies for the TIPEM library
# Use a tab before the $(CC) command for portability (UNIX)
# Put O=obj or O=o as needed in the makefile
# Use $(O) instead of $O for portability (VMS)
# Put a space before and after colon ":" for portability (VMS)

desc.$(O) : $(SRCDIR)desc.c global.h $(SRCDIR)rsaref.h $(SRCDIR)des.h
	$(CC) $(CFLAGS) $(SRCDIR)desc.c

digit.$(O) : $(SRCDIR)digit.c global.h $(SRCDIR)rsaref.h $(SRCDIR)nn.h\
  $(SRCDIR)digit.h
	$(CC) $(CFLAGS) $(SRCDIR)digit.c

md2c.$(O) : $(SRCDIR)md2c.c global.h $(SRCDIR)md2.h
	$(CC) $(CFLAGS) $(SRCDIR)md2c.c

md5c.$(O) : $(SRCDIR)md5c.c global.h $(SRCDIR)md5.h
	$(CC) $(CFLAGS) $(SRCDIR)md5c.c

nn.$(O) : $(SRCDIR)nn.c global.h $(SRCDIR)rsaref.h $(SRCDIR)nn.h\
  $(SRCDIR)digit.h
	$(CC) $(CFLAGS) $(SRCDIR)nn.c

prime.$(O) : $(SRCDIR)prime.c global.h $(SRCDIR)rsaref.h $(SRCDIR)nn.h\
  $(SRCDIR)prime.h
	$(CC) $(CFLAGS) $(SRCDIR)prime.c

rsa.$(O) : $(SRCDIR)rsa.c global.h $(SRCDIR)rsaref.h $(SRCDIR)r_random.h\
  $(SRCDIR)rsa.h $(SRCDIR)nn.h
	$(CC) $(CFLAGS) $(SRCDIR)rsa.c

r_encode.$(O) : $(SRCDIR)r_encode.c global.h $(SRCDIR)rsaref.h\
  $(SRCDIR)r_encode.h
	$(CC) $(CFLAGS) $(SRCDIR)r_encode.c

r_enhanc.$(O) : $(SRCDIR)r_enhanc.c global.h $(SRCDIR)rsaref.h\
  $(SRCDIR)r_encode.h $(SRCDIR)r_random.h $(SRCDIR)rsa.h $(SRCDIR)md2.h\
  $(SRCDIR)md5.h $(SRCDIR)des.h
	$(CC) $(CFLAGS) $(SRCDIR)r_enhanc.c

r_keygen.$(O) : $(SRCDIR)r_keygen.c global.h $(SRCDIR)rsaref.h\
  $(SRCDIR)r_random.h $(SRCDIR)nn.h
	$(CC) $(CFLAGS) $(SRCDIR)r_keygen.c

r_random.$(O) : $(SRCDIR)r_random.c global.h $(SRCDIR)rsaref.h\
  $(SRCDIR)r_random.h $(SRCDIR)md5.h
	$(CC) $(CFLAGS) $(SRCDIR)r_random.c

r_stdlib.$(O) : $(SRCDIR)r_stdlib.c global.h $(SRCDIR)rsaref.h
	$(CC) $(CFLAGS) $(SRCDIR)r_stdlib.c
