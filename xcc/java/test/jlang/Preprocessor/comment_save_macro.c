// RUN: jlang-cc -E -C %s | grep '^boo bork bar // zot$' &&
// RUN: jlang-cc -E -CC %s | grep -F '^boo bork /* blah*/ bar // zot$' &&
// RUN: jlang-cc -E %s | grep '^boo bork bar$'


#define FOO bork // blah
boo FOO bar // zot

