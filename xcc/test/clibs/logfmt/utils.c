
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <strings.h>

#include "logfmt.h"

static char *names[] = {
#define t(T) #T,
    LOGFMT_TOKEN_LIST
#undef t
};

const char *
logfmt_token_name(logfmt_token_t token) {
  return names[token];
}

void
logfmt_token_dump(logfmt_token_t token, const char *buf, size_t len,
                  void *data) {
  printf("  \e[34m%s\e[0m: '%.*s'\n", logfmt_token_name(token), (int)len, buf);
}

inline void
logfmt_token_noop(logfmt_token_t token, const char *buf, size_t len,
                  void *data) {
}

void
logfmt_field_dump(logfmt_field_t *field, void *data) {
  switch (field->type) {
  case LOGFMT_STRING:
    printf("  \e[34m%.*s\e[0m: '%.*s'\n", (int)field->name_len, field->name,
           (int)field->string_len, field->value.as_string);
    break;
  case LOGFMT_FLOAT:
    printf("  \e[34m%.*s\e[0m: %.2f\n", (int)field->name_len, field->name,
           field->value.as_float);
    break;
  case LOGFMT_INT:
    printf("  \e[34m%.*s\e[0m: %lld\n", (int)field->name_len, field->name,
           field->value.as_int);
    break;
  case LOGFMT_BOOL:
    printf("  \e[34m%.*s\e[0m: %s\n", (int)field->name_len, field->name,
           field->value.as_bool ? "true" : "false");
    break;
  }
}

void
logfmt_field_noop(logfmt_field_t *field, void *data) {
}
