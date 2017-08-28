// RUN: %jlang-cc -fsyntax-only -verify %s
int main() {
  char *s;
  s = (char []){"whatever"}; 
}
