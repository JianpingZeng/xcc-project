
#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "readline.h"

#define test_start() \
  printf("\n================\n")  \

/*
 * Reads two lines
 */
void
test_readline_simple() {

  test_start();
  char * buf = "" \
    "c++\n"
    "lua\n";

  readline_t * rl = readline_new(buf);
  char * line = NULL;
  size_t count = 0;

  while ((line = (readline_next(rl))) != NULL) {
    count++;
    printf("line: %s\n", line);
  }

  assert(count == 2);
  printf("\t OK\n");
}

/*
 * Reads 3 lines, including an empty line
 */
void
test_readline_empty_line() {
  test_start();
  char * buf = "" \
    "c++\n"
    "\n"
    "javascript";

  readline_t * rl = readline_new(buf);
  char * line = NULL;
  size_t count = 0;

  while ((line = (readline_next(rl))) != NULL) {
    count++;
    printf("line: %s\n", line);
  }

  assert(count == 3);
  printf("\t OK\n");
}

/*
 * Reads the last line
 */
void
test_readline_last_line() {

  test_start();
  char * buf = "" \
    "c++\n"
    "lua\n"
    "javascript";

  readline_t * rl = readline_new(buf);
  char * line = NULL;
  size_t count = 0;

  while ((line = (readline_next(rl))) != NULL) {
    count++;
    printf("line: %s\n", line);
    assert(count <= 3);
  }

  assert(count == 3);
  printf("\t OK\n");
}

/*
 * Reads the last that isn't empty
 */
void
test_readline_last_empty_line() {

  test_start();
  char * buf = "" \
    "c++\n"
    "lua\n"
    "";

  readline_t * rl = readline_new(buf);
  char * line = NULL;
  size_t count = 0;

  while ((line = (readline_next(rl))) != NULL) {
    count++;
    printf("line: %s\n", line);
  }

  assert(count == 2);
  printf("\t OK\n");
}

/*
 * Reads the last line
 */
void
test_readline_last() {

  test_start();
  char * buf = "" \
    "c++\n"
    "lua\n"
    "javascript\n";

  char * last = readline_last(buf);
  printf("last: %s\n", last);

  assert(strcmp("javascript", last) == 0);
  printf("\t OK\n");
}

/*
 * Reads the last line without \n
 */
void
test_readline_last_without_10() {

  test_start();
  char * buf = "" \
    "c++\n"
    "lua\n"
    "javascript";

  char * last = readline_last(buf);
  printf("last: %s\n", last);

  assert(strcmp("javascript", last) == 0);
  printf("\t OK\n");
}


int
main() {

  /* next func */
  test_readline_simple();
  test_readline_empty_line();
  test_readline_last_line();
  test_readline_last_empty_line();

  /* last line */
  test_readline_last();
  test_readline_last_without_10();
  return 0;
}
