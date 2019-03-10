
# rgba

  RGB / RGBA parsing / formatting.

## Installation

  Install with [clib](https://github.com/clibs/clib):

```
$ clib install clibs/rgba
```

## API

```c
typedef struct {
  double r, g, b, a;
} rgba_t;

rgba_t
rgba_new(uint32_t rgba);

uint32_t
rgba_from_string(const char *str, short *ok);

void
rgba_to_string(rgba_t rgba, char *buf, size_t len);

void
rgba_inspect(uint32_t rgba);
```
