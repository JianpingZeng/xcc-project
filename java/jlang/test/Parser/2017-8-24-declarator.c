//RUN: jlang-cc -fsyntax-only %s
int *(*(*arr[5])())();
