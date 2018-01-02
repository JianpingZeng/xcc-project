// Example from C99 6.10.3.4p7

// RUN: jlang-cc -E %s | grep -F 'int j[] = { 123, 45, 67, 89,' &&
// RUN: jlang-cc -E %s | grep -F '10, 11, 12, };'

#define t(x,y,z) x ## y ## z 
int j[] = { t(1,2,3), t(,4,5), t(6,,7), t(8,9,), 
t(10,,), t(,11,), t(,,12), t(,,) }; 

