int
strlen(char *str)
{
  if (!str) {
    return 0;
  }
  
  char *ptr = str;
  while (*str) {
    ++str;
  }

  return str - ptr;
}