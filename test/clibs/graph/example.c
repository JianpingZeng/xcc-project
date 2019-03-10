/**
 * Note:
 * To compile, simply run `make example`.
 */

#include <stdio.h>
#include "graph/graph.h"

int
main(void) {
  graph_graph_t * graph = graph_new("test", GRAPH_STORE_ADJANCENCY_LIST);

  graph_vertex_t * vertex1 = graph_add_vertex(graph, NULL);
  graph_vertex_t * vertex2 = graph_add_vertex(graph, NULL);
  graph_vertex_t * vertex3 = graph_add_vertex(graph, NULL);
  graph_vertex_t * vertex4 = graph_add_vertex(graph, NULL);
  graph_vertex_t * vertex5 = graph_add_vertex(graph, NULL);

  graph_edge_t * edge1 = graph_add_edge(
    graph,
    NULL,
    vertex1,
    vertex2,
    0
  );

  graph_edge_t * edge2 = graph_add_edge(
    graph,
    NULL,
    vertex1,
    vertex4,
    0
  );

  graph_edge_t * edge3 = graph_add_edge(
    graph,
    NULL,
    vertex3,
    vertex5,
    0
  );

  graph_edge_t * edge4 = graph_add_edge(
    graph,
    NULL,
    vertex2,
    vertex5,
    0
  );

  printf("[graph]: %s\n", graph->label);
  printf("[vertex1]: %s\n", vertex1->label);
  printf("[vertex2]: %s\n", vertex2->label);
  printf("[vertex3]: %s\n", vertex3->label);
  printf("[vertex4]: %s\n", vertex4->label);
  printf("[vertex5]: %s\n", vertex5->label);
  printf("[edge1]: %s\n", edge1->label);
  printf("[edge2]: %s\n", edge2->label);
  printf("[edge3]: %s\n", edge3->label);
  printf("[edge4]: %s\n", edge4->label);
  graph_delete(graph);

  return 0;
}
