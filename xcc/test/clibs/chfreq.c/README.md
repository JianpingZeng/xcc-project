chfreq.c
========

Compute character frequency in a string

## install

[clib](https://github.com/clibs/clib):

```sh
$ clib install jwerle/chfreq.c
```

source:

```sh
$ git clone git@github.com:jwerle/chfreq.c.git
$ cd chfreq.c
$ make
$ make install
```

## example

```c
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include "chfreq.h"

int
main (void) {
  char *str = "aabbcc";
  uint32_t freq** = chfreq(str);
  printf("%c %d\n", freq[0][0], freq[0][1]);
  printf("%c %d\n", freq[1][0], freq[1][1]);
  printf("%c %d\n", freq[2][0], freq[2][1]);
  return 0;
}
```

...yields:

```
a 2
b 2
c 2
```

With the command line utility:

```sh
$ echo -n aabbcc | chfreq
a | 2
b | 2
c | 2
```

```sh
$ echo -n 'kinkajous are awesome !'| chfreq
k | 2
i | 1
n | 1
a | 3
j | 1
o | 2
u | 1
s | 2
  | 3
r | 1
e | 3
w | 1
m | 1
! | 1
```

```sh
$ echo -n werle | chfreq -f '%c(%d)'
w(1)
e(2)
r(1)
l(1)
```

## api

```c
uint32_t **
chfreq (const char *src);
```

Returns an array of `uint32_t' type arrays who's character is the first index
and the occurence is the second index.

**example structure:**

```c
uint32_t mat[2][2] = {
  {'a', 2},
  NULL
}
```

## license

MIT
