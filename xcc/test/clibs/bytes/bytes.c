
//
// bytes.c
//
// Copyright (c) 2012 TJ Holowaychuk <tj@vision-media.ca>
//

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "bytes.h"

// bytes

#define KB 1024
#define MB 1024 * KB
#define GB 1024 * MB

/*
 * Convert the given `str` to byte count.
 */

long long
string_to_bytes(const char *str) {
  long long val = strtoll(str, NULL, 10);
  if (!val) return -1;
  if (strstr(str, "kb")) return val * KB;
  if (strstr(str, "mb")) return val * MB;
  if (strstr(str, "gb")) return val * GB;
  return val;
}

/*
 * Convert the given `bytes` to a string. This
 * value must be `free()`d by the user.
 */

char *
bytes_to_string(long long bytes) {
  long div = 1;
  char *str, *fmt;
  if (bytes < KB) { fmt = "%lldb"; }
  else if (bytes < MB) { fmt = "%lldkb"; div = KB; }
  else if (bytes < GB) { fmt = "%lldmb"; div = MB; }
  else { fmt = "%lldgb"; div = GB; }
  asprintf(&str, fmt, bytes / div);
  return str;
}
