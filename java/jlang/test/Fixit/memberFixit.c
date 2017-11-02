struct Fix
{
  int data;
  int index;
  int dte;
  int indx;
};

void bar(struct Fix *fix)
{
  // Checks whether appropriate fix-it hints should be printed out.
  fix->date = 1;
  fix->idx = 2;
}
