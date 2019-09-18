
#ifndef LOGFMT_H
#define LOGFMT_H

#include <stdbool.h>
#include <stdlib.h>

// List of tokens.
#define LOGFMT_TOKEN_LIST \
  t(ILLEGAL) t(LIT) t(EQ) t(FLOAT) t(DECIMAL) t(OCTAL) t(HEX) t(BOOL) t(EOL)

// Log token.
typedef enum {
#define t(T) LOGFMT_TOK_##T,
  LOGFMT_TOKEN_LIST
#undef t
} logfmt_token_t;

// Field type.
typedef enum {
  LOGFMT_STRING,
  LOGFMT_FLOAT,
  LOGFMT_BOOL,
  LOGFMT_INT
} logfmt_field_type_t;

// Field value.
typedef struct {
  const char *name;
  size_t name_len;

  logfmt_field_type_t type;

  union {
    long long as_int;
    char *as_string;
    double as_float;
    bool as_bool;
  } value;

  size_t string_len;
} logfmt_field_t;

// Scanner callback function. The buffer passed is a reference of the original,
// and should be copied if you wish you manipulate or store elsewhere.
typedef void (*logfmt_scan_callback_t)(logfmt_token_t token, const char *value,
                                       size_t len, void *data);

// Scan with callback function.
int logfmt_scan(char *buf, size_t len, logfmt_scan_callback_t cb, void *data);

// Helper callback for dumping tokens to stdout.
void logfmt_token_dump(logfmt_token_t token, const char *value, size_t len,
                       void *data);

// Helper callback for no-oping, useful for benchmarks.
void logfmt_token_noop(logfmt_token_t token, const char *value, size_t len,
                       void *data);

// Parser callback function. The field passed is reset with each call, any data
// retained should be copied if you wish to manipulate or store elsewhere.
typedef void (*logfmt_parse_callback_t)(logfmt_field_t *field, void *data);

// Parse with callback function.
int logfmt_parse(char *buf, size_t len, logfmt_parse_callback_t cb, void *data);

// Helper callback for dumping fields to stdout.
void logfmt_field_dump(logfmt_field_t *field, void *data);

// Helper callback for no-oping, useful for benchmarks.
void logfmt_field_noop(logfmt_field_t *field, void *data);

// Helper function for returning a token string name.
const char *logfmt_token_name(logfmt_token_t token);

#endif /* LOGFMT_H */
