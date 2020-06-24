
# file

  File utilities

## Installation

  Install with [clib](https://github.com/clibs/clib):

```
$ clib install clibs/file
```

## API

```c
off_t
file_size(const char *filename);

int
file_exists(const char *filename);

char *
file_read(const char *filename);
```

# License

  MIT
  