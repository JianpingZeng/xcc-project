
#include <stdio.h>
#include <stdlib.h>
#include "src/file.h"

int
main(int argc, char **argv) {
  if (argc < 2) {
    fprintf(stderr, "<filename> required\n");
    exit(1);
  }

  char *name = argv[1];
  printf("exists: %s\n", file_exists(name) ? "yes" : "no");
  printf("size: %lld\n", file_size(name));

  if (file_exists(name))
	  puts(file_read(name));

  char* path = "/tmp/long/dir/sequence";
  file_mkdir_p(path);
  printf("'%s' %s\n", path, file_exists(path) ? "created" : "not created");

  return 0;
}
