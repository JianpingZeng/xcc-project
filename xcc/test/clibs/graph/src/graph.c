#include "graph.h"

#define _GRAPH_ITEM_SET_LABEL_TH_BEGIN(                                                                          \
    item_,                                                                                                       \
    type_                                                                                                        \
  )                                                                                                              \
    pthread_t set_label_th;                                                                                      \
    struct _graph_item_set_label_th_args * set_label_th_args = _MALLOC(struct _graph_item_set_label_th_args, 1); \
                                                                                                                 \
    if (! set_label_th_args) { exit(-1); }                                                                       \
                                                                                                                 \
    int set_label_th_err = 0;                                                                                    \
                                                                                                                 \
    set_label_th_args->item  = item_;                                                                            \
    set_label_th_args->label = label;                                                                            \
    set_label_th_args->type  = type_;                                                                            \
                                                                                                                 \
    set_label_th_err = pthread_create(                                                                           \
      &set_label_th,                                                                                             \
      NULL,                                                                                                      \
      _graph_item_set_label_th,                                                                                  \
      set_label_th_args                                                                                          \
    );                                                                                                           \
                                                                                                                 \
    if (set_label_th_err) { exit(-1); }

#define _GRAPH_ITEM_SET_LABEL_TH_END                     \
    set_label_th_err = pthread_join(set_label_th, NULL); \
                                                         \
    if (set_label_th_err) { exit(-1); }                  \
                                                         \
    _FREE(set_label_th_args);

// +-------+---------------------+
// | BEGIN | static declarations |
// +-------+---------------------+

static enum _graph_item_types {
  _GRAPH_ITEM_TYPE_EDGE,
  _GRAPH_ITEM_TYPE_GRAPH,
  _GRAPH_ITEM_TYPE_VERTEX
};

struct _graph_item_set_label_th_args {
  void * item;
  const char * label;
  enum _graph_item_types type;
};

static void
_graph_item_set_label(
  void *,
  const char *,
  enum _graph_item_types
);

static void
_graph_item_set_label_th(
  struct _graph_item_set_label_th_args *
);

static void
graph_adjancency_list_append(
  graph_graph_t *,
  graph_edge_t *
);

static void
graph_adjancency_list_init(
  graph_graph_t *,
  graph_vertex_t *
);

static void
graph_edge_delete(
  graph_edge_t *
);

static void
graph_setup_store(
  graph_graph_t *
);

static void
graph_teardown_store(
  graph_graph_t *
);

static void
graph_teardown_store_adjacency_list(
  graph_graph_t *
);

static void
graph_vertex_delete(
  graph_vertex_t *
);

// +-----+---------------------+
// | END | static declarations |
// +-----+---------------------+

// +-------+------------------+
// | BEGIN | core definitions |
// +-------+------------------+

graph_edge_t *
graph_add_edge(
  graph_graph_t * graph,
  const char * label,
  graph_vertex_t * from_vertex,
  graph_vertex_t * to_vertex,
  intmax_t weight
) {
  graph_edge_t * edge = _MALLOC(graph_edge_t, 1);

  if (! edge) { exit(-1); }

  if (graph->_auto_inc.edge.is_available) { edge->id = graph->_auto_inc.edge.available; }
  else { edge->id = ++graph->_auto_inc.edge.last; }

  edge->from_vertex = from_vertex;
  edge->to_vertex   = to_vertex;
  edge->weight      = weight;
  edge->data        = NULL;

  _GRAPH_ITEM_SET_LABEL_TH_BEGIN(edge, _GRAPH_ITEM_TYPE_EDGE)

  switch (graph->store_type) {
    case GRAPH_STORE_ADJANCENCY_LIST:
      graph_adjancency_list_append(graph, edge);

      break;
  }

  list_rpush(
    graph->edges,
    list_node_new(edge)
  );

  _GRAPH_ITEM_SET_LABEL_TH_END

  return edge;
}

graph_vertex_t *
graph_add_vertex(
  graph_graph_t * graph,
  const char * label
) {
  graph_vertex_t * vertex = _MALLOC(graph_vertex_t, 1);

  if (! vertex) { exit(-1); }

  if (graph->_auto_inc.vertex.is_available) { vertex->id = graph->_auto_inc.vertex.available; }
  else { vertex->id = ++graph->_auto_inc.vertex.last; }

  vertex->data = NULL;

  _GRAPH_ITEM_SET_LABEL_TH_BEGIN(vertex, _GRAPH_ITEM_TYPE_VERTEX)

  switch (graph->store_type) {
    case GRAPH_STORE_ADJANCENCY_LIST:
      graph_adjancency_list_init(graph, vertex);

      break;
  }

  list_rpush(
    graph->vertices,
    list_node_new(vertex)
  );

  graph->cardinality++;

  _GRAPH_ITEM_SET_LABEL_TH_END

  return vertex;
}

void
graph_change_edge_id(
  graph_graph_t * graph,
  graph_edge_t * edge,
  uintmax_t id
) {
  //
}

void
graph_change_vertex_id(
  graph_graph_t * graph,
  graph_vertex_t * vertex,
  uintmax_t id
) {
  //
}

void
graph_delete(
  graph_graph_t * graph
) {
  _FREE(graph->label);
  list_destroy(graph->edges);
  list_destroy(graph->vertices);
  graph_teardown_store(graph);
  _FREE(graph);
}

graph_graph_t *
graph_new(
  const char * label,
  graph_store_t store_type
) {
  graph_graph_t * graph = _MALLOC(graph_graph_t, 1);

  if (! graph) { exit(-1); }

  graph->label      = label;
  graph->edges      = list_new();
  graph->vertices   = list_new();
  graph->store_type = store_type;

  _GRAPH_ITEM_SET_LABEL_TH_BEGIN(graph, _GRAPH_ITEM_TYPE_GRAPH)
  graph_setup_store(graph);

  graph->_auto_inc.edge.available    = 0;
  graph->_auto_inc.edge.is_available = false;
  graph->_auto_inc.edge.last         = 0;

  graph->_auto_inc.vertex.available    = 0;
  graph->_auto_inc.vertex.is_available = false;
  graph->_auto_inc.vertex.last         = 0;

  graph->cardinality = 0;

  graph->edges->free    = graph_edge_delete;
  graph->vertices->free = graph_vertex_delete;

  // graph->edges->match    = graph_edge_match;
  // graph->vertices->match = graph_vertex_match;

  _GRAPH_ITEM_SET_LABEL_TH_END

  return graph;
}

void
graph_remove_edge(
  graph_graph_t * graph,
  uintmax_t id
) {
  //

  graph->_auto_inc.edge.available    = id;
  graph->_auto_inc.edge.is_available = true;
}

void
graph_remove_vertex(
  graph_graph_t * graph,
  uintmax_t id
) {
  //

  graph->_auto_inc.vertex.available    = id;
  graph->_auto_inc.vertex.is_available = true;
}

// +-----+------------------+
// | END | core definitions |
// +-----+------------------+

// +-------+--------------------+
// | BEGIN | static definitions |
// +-------+--------------------+

static void
_graph_item_set_label(
  void * item,
  const char * label,
  enum _graph_item_types type
) {
  uint8_t label_size;
  uint8_t label_wrap_size;

  switch (type) {
    case _GRAPH_ITEM_TYPE_EDGE:
      label_wrap_size = 9;

      break;

    case _GRAPH_ITEM_TYPE_GRAPH:
      label_wrap_size = 10;

      break;

    case _GRAPH_ITEM_TYPE_VERTEX:
      label_wrap_size = 11;

      break;
  }

  if (label) {
    label_size = (
      strlen(label) +
      label_wrap_size +
      1
    );

    switch (type) {
      case _GRAPH_ITEM_TYPE_EDGE:
        ((graph_edge_t *) item)->label = _MALLOC(const char, label_size);

        if (! ((graph_edge_t *) item)->label) { exit(-1); }

        snprintf(
          ((graph_edge_t *) item)->label,
          label_size,
          "<<EDGE:%s>>",
          label
        );

        break;

      case _GRAPH_ITEM_TYPE_GRAPH:
        ((graph_graph_t *) item)->label = _MALLOC(const char, label_size);

        if (! ((graph_graph_t *) item)->label) { exit(-1); }

        snprintf(
          ((graph_graph_t *) item)->label,
          label_size,
          "<<GRAPH:%s>>",
          label
        );

        break;

      case _GRAPH_ITEM_TYPE_VERTEX:
        ((graph_vertex_t *) item)->label = _MALLOC(const char, label_size);

        if (! ((graph_vertex_t *) item)->label) { exit(-1); }

        snprintf(
          ((graph_vertex_t *) item)->label,
          label_size,
          "<<VERTEX:%s>>",
          label
        );

        break;
    }
  }
  else {
    uint8_t _id_num_digits;

    switch (type) {
      case _GRAPH_ITEM_TYPE_EDGE:
        _id_num_digits = _uint_num_digits(((graph_edge_t *) item)->id);

        break;

      case _GRAPH_ITEM_TYPE_GRAPH:
        _id_num_digits = 11; // "[[UNKNOWN]]"

        break;

      case _GRAPH_ITEM_TYPE_VERTEX:
        _id_num_digits = _uint_num_digits(((graph_vertex_t *) item)->id);

        break;
    }

    uint8_t _id_num_digits_str = _id_num_digits + 1;
    label_size = _id_num_digits_str + label_wrap_size;

    switch (type) {
      case _GRAPH_ITEM_TYPE_EDGE:
        ((graph_edge_t *) item)->label = _MALLOC(const char, label_size);

        if (! ((graph_edge_t *) item)->label) { exit(-1); }

        snprintf(
          ((graph_edge_t *) item)->label,
          label_size,
          ("<<EDGE:%" PRIuMAX ">>"),
          ((graph_edge_t *) item)->id
        );

        break;

      case _GRAPH_ITEM_TYPE_GRAPH:
        ((graph_graph_t *) item)->label = _MALLOC(const char, label_size);

        if (! ((graph_graph_t *) item)->label) { exit(-1); }

        snprintf(((graph_graph_t *) item)->label, label_size, "<<GRAPH:[[UNKNOWN]]>>");

        break;

      case _GRAPH_ITEM_TYPE_VERTEX:
        ((graph_vertex_t *) item)->label = _MALLOC(const char, label_size);

        if (! ((graph_vertex_t *) item)->label) { exit(-1); }

        snprintf(
          ((graph_vertex_t *) item)->label,
          label_size,
          ("<<VERTEX:%" PRIuMAX ">>"),
          ((graph_vertex_t *) item)->id
        );

        break;
    }
  }
}

static void
_graph_item_set_label_th(
  struct _graph_item_set_label_th_args * args
) {
  struct _graph_item_set_label_th_args * _args = args;

  _graph_item_set_label(_args->item, _args->label, _args->type);
}

static void
graph_adjancency_list_append(
  graph_graph_t * graph,
  graph_edge_t * edge
) {
  graph_adjacency_list_ht_t * ht;

  HASH_FIND(
    handle,
    graph->store.adjacency_list_hash,
    &(edge->from_vertex->id),
    sizeof(uintmax_t),
    ht
  );

  list_rpush(
    ht->edges,
    list_node_new(edge)
  );
}

static void
graph_adjancency_list_init(
  graph_graph_t * graph,
  graph_vertex_t * vertex
) {
  graph_adjacency_list_ht_t * ht = _MALLOC(graph_adjacency_list_ht_t, 1);

  if (! ht) { exit(-1); }

  ht->id    = vertex->id;
  ht->edges = list_new();

  HASH_ADD(
    handle,
    graph->store.adjacency_list_hash,
    id,
    sizeof(uintmax_t),
    ht
  );

  // ((list_t *) hash_get(
  //   graph->store.adjacency_list_hash,
  //   id_key
  // ))->match = graph_adjancency_list_match;
}

// static bool
// graph_adjancency_list_match(
//   graph_edge_t * edge,
// ) {
//   return false;
// }

static void
graph_edge_delete(
  graph_edge_t * edge
) {
  _FREE(edge->label);
  _FREE(edge);
}

static void
graph_setup_store(
  graph_graph_t * graph
) {
  switch (graph->store_type) {
    case GRAPH_STORE_ADJANCENCY_LIST:
      graph->store.adjacency_list_hash = NULL;

      break;

    default:
      graph->store_type = GRAPH_STORE_ADJANCENCY_LIST;

      graph_setup_store(graph);

      break;
  }
}

static void
graph_teardown_store(
  graph_graph_t * graph
) {
  switch (graph->store_type) {
    case GRAPH_STORE_ADJANCENCY_LIST:
      graph_teardown_store_adjacency_list(graph);

      break;
  }
}

static void
graph_teardown_store_adjacency_list(
  graph_graph_t * graph
) {
  graph_adjacency_list_ht_t * ht;
  graph_adjacency_list_ht_t * ht_tmp;

  HASH_ITER(
    handle,
    graph->store.adjacency_list_hash,
    ht,
    ht_tmp
  ) {
    list_destroy(ht->edges);

    HASH_DELETE(handle, graph->store.adjacency_list_hash, ht);

    _FREE(ht);
  }
}

static void
graph_vertex_delete(
  graph_vertex_t * vertex
) {
  _FREE(vertex->label);
  _FREE(vertex);
}

// +-----+--------------------+
// | END | static definitions |
// +-----+--------------------+
