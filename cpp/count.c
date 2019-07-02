/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Xlous
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

 #include <stdlib.h>
 #include <stdio.h>

 int main(int argc, char **argv)
 {
   unsigned Count, NumLines, NumRead;
   char Buffer[4096], *End;

   if (argc != 2)
   {
     fprintf(stderr, "usage: %s <expected line count>\n", argv[0]);
     return 2;
   }

   Count = strtol(argv[1], &End, 10);
   if (*End != '\0' && End != argv[1])
   {
     fprintf(stderr, "%s: invalid count argument '%s'\n", argv[0], argv[1]);
     return 2;
   }

   NumLines = 0;
   do
   {
     unsigned i;

     NumRead = fread(Buffer, 1, sizeof(Buffer), stdin);

     for (i = 0; i != NumRead; ++i)
       if (Buffer[i] == '\n')
         ++NumLines;
   } while (NumRead == sizeof(Buffer));

   if (!feof(stdin))
   {
     fprintf(stderr, "%s: error reading stdin\n", argv[0]);
     return 3;
   }

   if (Count != NumLines)
   {
     fprintf(stderr, "Expected %d lines, got %d.\n", Count, NumLines);
     return 1;
   }

   return 0;
 }