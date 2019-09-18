readline
================

Tiny C readline library, note: this is not used in CLI-readline's one.


Installation
==================

Install it with git

```bash
$ git clone https://github.com/yorkie/readline.git master
```

Install it with [clib](https://github.com/clibs/clib)

```bash
$ clib install clibs/readline
```

Get Started
=================

```c
#include "readline.h"

readline_t * rl = readline_new(text);
char * line;

do {
  line = readline_next(rl);
  // get line.

} while (line != NULL);

// free memory
readline_free(rl);
```

API
=================

```c

/*
 * Create a context of readline from a buffer
 */
readline_t *
readline_new(char * buffer);

/*
 * Get the next line of the context
 */
char *
readline_next(readline_t * rl);

/*
 * Get last line of a buffer, ignoring any context of readline
 */
char *
readline_last_from_rl(readline_t * rl);

/*
 * Get the last line directly buffer
 */
char *
readline_last(char * buffer);

/*
 * free the object
 */
void
readline_free(readline_t * rl);
```


License
===================

MIT
