#include <iostream.h>
#include <String.h>

#define FILTER "./filter < "
#define TMP    " ./tmp.tmp "
#define CP     "cp "
main() {
  while (1) {
    String inp;
    cin >> inp;
    if ( inp.empty() ) break;

    String sys = FILTER;
    sys += inp; sys += " > "; sys += TMP;
    cout << sys.chars() << endl;
    system( sys.chars() );

    String cp = CP;
    cp += TMP; cp += inp;
    cout << cp.chars() << endl;
    system( cp.chars() );
  }
}
