

#include <locale.h>
#include <stdio.h>
#include <strings.h>

#include "bench.h"
#include "logfmt.h"

static char *s = "request status=200 method=GET path=/pets user_id=1234 "
                 "user=tobi@ferret.com app=api host=api-01 app_id=4321 "
                 "size=512 @timestamp=123123123";

void
report(float d, int ops, size_t len) {
  printf("   len: %lu\n", len);
  printf("   ops: %'d\n", ops);
  printf("   ops/s: %'.2f\n", ops / d);
  printf("   ns/op: %'.2f\n", d / ops * 1e9);
  printf("   MiB/s: %'.2lu\n", ops * len / (1 << 20));
  printf("   duration: %.2fs\n", d);
}

int
scan() {
  float start = cpu();
  int ops = 10e6;
  size_t len = strlen(s);

  for (size_t i = 0; i < ops; i++) {
    if (logfmt_scan(s, len, logfmt_token_noop, NULL) < 0) {
      printf("error parsing\n");
      return 1;
    }
  }

  setlocale(LC_ALL, "");

  float d = cpu() - start;
  printf("\n  Scanner:\n");
  report(d, ops, len);
  return 0;
}

int
parse() {
  float start = cpu();
  int ops = 10e6;
  size_t len = strlen(s);

  for (size_t i = 0; i < ops; i++) {
    if (logfmt_parse(s, len, logfmt_field_noop, NULL) < 0) {
      printf("error parsing\n");
      return 1;
    }
  }

  setlocale(LC_ALL, "");

  float d = cpu() - start;
  printf("\n  Parser:\n");
  report(d, ops, len);
  return 0;
}

int
main() {
  if (scan() < 0) return 1;
  if (parse() < 0) return 1;
  printf("\n");
  return 0;
}
