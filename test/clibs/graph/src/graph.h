/**
 * The MIT License (MIT).
 *
 * https://github.com/clibs/graph
 *
 * Copyright (c) 2014 clibs, Jonathan Barronville (jonathan@scrapum.photos), and contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

#ifndef __GRAPH_H__INCLUDED__
#define __GRAPH_H__INCLUDED__

#ifdef __cplusplus
#include <cinttypes>
#include <cmath>
#include <cstdbool>
#include <cstdio>
#include <cstdlib>
#include <cstring>

using namespace std;
#else
#include <inttypes.h>
#include <math.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#endif

#include <pthread.h>

#include "list/list.h"
#include "uthash/uthash.h"

#if                         \
  defined(__GNUC__) &&      \
  (                         \
    (__GNUC__ > 3) ||       \
    (                       \
      (__GNUC__ == 3) &&    \
      (__GNUC_MINOR__ >= 1) \
    )                       \
  )
#define _ALWAYS_INLINE __attribute__ ((always_inline))
#else
#define _ALWAYS_INLINE
#endif

#if                         \
  defined(__GNUC__) &&      \
  (                         \
    (__GNUC__ > 2) ||       \
    (                       \
      (__GNUC__ == 2) &&    \
      (__GNUC_MINOR__ >= 7) \
    )                       \
  )
#define _UNUSED_VAR __attribute__ ((unused))
#else
#define _UNUSED_VAR
#endif

#ifdef __cplusplus
#define _MALLOC(type, count) ((type *) malloc(sizeof(type) * count))
#else
#define _MALLOC(type, count) (malloc(sizeof(type) * count))
#endif

#define _FREE free

#if                         \
  defined(__GNUC__) &&      \
  (                         \
    (__GNUC__ > 3) ||       \
    (                       \
      (__GNUC__ == 3) &&    \
      (__GNUC_MINOR__ >= 3) \
    )                       \
  )
#define GRAPH_ABI_EXPORT __attribute__ ((visibility ("default")))
#define GRAPH_ABI_HIDDEN __attribute__ ((visibility ("hidden")))
#else
#define GRAPH_ABI_EXPORT
#define GRAPH_ABI_HIDDEN
#endif

#define _CAST_INTMAX_T(val) ((intmax_t) val)
#define _CAST_UINT8_T(val) ((uint8_t) val)
#define _CAST_UINTMAX_T(val) ((uintmax_t) val)

#ifdef __cplusplus
extern "C" {
#endif

// +-------+------------------+
// | BEGIN | type definitions |
// +-------+------------------+

typedef enum _graph_stores {
  GRAPH_STORE_ADJANCENCY_LIST
} graph_store_t;

typedef struct _graph_adjacency_list_ht graph_adjacency_list_ht_t;
typedef struct _graph_edge graph_edge_t;
typedef struct _graph_graph graph_graph_t;
typedef struct _graph_vertex graph_vertex_t;

// +-----+------------------+
// | END | type definitions |
// +-----+------------------+

// +-------+----------+
// | BEGIN | core api |
// +-------+----------+

GRAPH_ABI_EXPORT graph_edge_t *
graph_add_edge(
  graph_graph_t *,
  const char *,
  graph_vertex_t *,
  graph_vertex_t *,
  intmax_t
);

GRAPH_ABI_EXPORT graph_vertex_t *
graph_add_vertex(
  graph_graph_t *,
  const char *
);

GRAPH_ABI_EXPORT void
graph_change_edge_id(
  graph_graph_t *,
  graph_edge_t *,
  uintmax_t
);

GRAPH_ABI_EXPORT void
graph_change_vertex_id(
  graph_graph_t *,
  graph_vertex_t *,
  uintmax_t
);

GRAPH_ABI_EXPORT void
graph_delete(
  graph_graph_t *
);

GRAPH_ABI_EXPORT graph_graph_t *
graph_new(
  const char *,
  graph_store_t
);

GRAPH_ABI_EXPORT void
graph_remove_edge(
  graph_graph_t *,
  uintmax_t
);

GRAPH_ABI_EXPORT void
graph_remove_vertex(
  graph_graph_t *,
  uintmax_t
);

// +-----+----------+
// | END | core api |
// +-----+----------+

// +-------+--------------------+
// | BEGIN | struct definitions |
// +-------+--------------------+

struct _graph_adjacency_list_ht {
  list_t * edges;
  UT_hash_handle handle;
  uintmax_t id;
};

struct _graph_edge {
  void * data;
  graph_vertex_t * from_vertex;
  uintmax_t id;
  const char * label;
  graph_vertex_t * to_vertex;
  intmax_t weight;
};

struct _graph_graph {
  struct {
    struct {
      uintmax_t available;
      bool is_available;
      uintmax_t last;
    } edge;

    struct {
      uintmax_t available;
      bool is_available;
      uintmax_t last;
    } vertex;
  } _auto_inc;

  uintmax_t cardinality;
  list_t * edges;
  const char * label;

  union {
    graph_adjacency_list_ht_t * adjacency_list_hash;
  } store;

  graph_store_t store_type;
  list_t * vertices;
};

struct _graph_vertex {
  void * data;
  uintmax_t id;
  const char * label;
};

// +-----+--------------------+
// | END | struct definitions |
// +-----+--------------------+

// +-------+-----------+
// | BEGIN | utilities |
// +-------+-----------+

_ALWAYS_INLINE inline uint8_t
_uint_num_digits(
  uintmax_t num
) {
  uint8_t digits = 0;

  if (num == 0) { digits = 1; }
  else {
    digits = floor(
      log10(num)
    ) + 1;
  }

  return digits;
}

// +-----+-----------+
// | END | utilities |
// +-----+-----------+

#ifdef __cplusplus
}
#endif

#endif
