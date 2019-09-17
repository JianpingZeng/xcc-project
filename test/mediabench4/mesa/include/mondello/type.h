/******************************************************************************
*
*   Module:     TYPE.H          Generic Type Header Module
*
*   Revision:   1.00
*
*   Date:       April 14, 1994
*
*   Author:     Randy Spurlock
*
*******************************************************************************
*
*   Module Description:
*
*       This module contains generic type declarations.
*
*******************************************************************************
*
*   Changes:
*
*    DATE     REVISION  DESCRIPTION                             AUTHOR
*  --------   --------  -------------------------------------------------------
*  04/14/94     1.00    Original                                Randy Spurlock
*  09/26/94     1.01    Add few new defines                     Goran Devic
*
*/
#ifndef __TYPE__
#define __TYPE__


/******************************************************************************
*   Constant Declarations
******************************************************************************/
#define TRUE            1               /* Define TRUE as the value 1        */
#define FALSE           0               /* Define FALSE as the value 0       */
#define OK              1               /* Define OK as the value 1          */
#define ERROR           0               /* Define ERROR as the value 0       */

/******************************************************************************
*   Type Definitions
******************************************************************************/
typedef int BOOL;                       /* Define a boolean as an integer    */
typedef unsigned char BYTE;             /* Define a byte data type           */
typedef unsigned short int WORD;        /* Define a word data type           */
typedef unsigned long DWORD;            /* Define a double word data type    */
typedef signed long FIX;                /* Define a 16.16 fixpoint           */

#endif

