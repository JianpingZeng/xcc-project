
//
// bytes.h
//
// Copyright (c) 2012 TJ Holowaychuk <tj@vision-media.ca>
//

#ifndef BYTES
#define BYTES

// prototypes

long long
string_to_bytes(const char *str);

char *
bytes_to_string(long long bytes);

#endif