
 char *strndup(const char *s, size_t n);

The strndup() function only copies at most n bytes. If s is longer than n, only n bytes are copied, and a terminating null byte ('\0') is added.
