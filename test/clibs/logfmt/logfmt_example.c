
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <strings.h>

#include "logfmt.h"

void
field(logfmt_field_t *field, void *data) {
  switch (field->type) {
  case LOGFMT_STRING:
    printf("  \e[34m%.*s\e[0m: '%.*s'", (int)field->name_len, field->name,
           (int)field->string_len, field->value.as_string);
    break;
  case LOGFMT_FLOAT:
    printf("  \e[34m%.*s\e[0m: %.2f", (int)field->name_len, field->name,
           field->value.as_float);
    break;
  case LOGFMT_INT:
    printf("  \e[34m%.*s\e[0m: %lld", (int)field->name_len, field->name,
           field->value.as_int);
    break;
  case LOGFMT_BOOL:
    printf("  \e[34m%.*s\e[0m: %s", (int)field->name_len, field->name,
           field->value.as_bool ? "true" : "false");
    break;
  }
}

int
main() {
  char buf[1024];

  while (fgets(buf, 1024, stdin)) {
    if (logfmt_parse(buf, strlen(buf), field, NULL) < 0) {
      fprintf(stderr, "Error: failed to parse\n");
      return 1;
    }
    printf("\n");
  }

  return 0;
}
