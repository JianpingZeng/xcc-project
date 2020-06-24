
# logfmt

This package implements a fast SAX-style [logfmt](https://brandur.org/logfmt) lexer & parser via the almighty Ragel FSM generator.

## Installation

```
$ clib install logfmt
```

## Example

Contrived example that just parses stdin and prints the fields back out to stdout:

```c
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
```

## Benchmarks

 Non-scientific results from my MBP with 122b logs:

```
Scanner:
 len: 120
 ops: 10,000,000
 ops/s: 4,444,444.50
 ns/op: 225.00
 MiB/s: 1,144
 duration: 2.25s

Parser:
 len: 120
 ops: 10,000,000
 ops/s: 2,962,963.00
 ns/op: 337.50
 MiB/s: 1,144
 duration: 3.38s
```

## Badges

![](https://img.shields.io/badge/license-MIT-blue.svg)
![](https://img.shields.io/badge/status-stable-green.svg)

---

> [tjholowaychuk.com](http://tjholowaychuk.com) &nbsp;&middot;&nbsp;
> GitHub [@tj](https://github.com/tj) &nbsp;&middot;&nbsp;
> Twitter [@tjholowaychuk](https://twitter.com/tjholowaychuk)
