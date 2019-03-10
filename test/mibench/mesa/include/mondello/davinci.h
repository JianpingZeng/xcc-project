/******************************************************************************
*
*   Module:     DAVINCI.H       DaVinci Specific Define Module
*
*   Revision:   1.00
*
*   Date:       September 26, 1995
*
*   Author:     Goran Devic
*
*******************************************************************************
*
*   Module Description:
*
*       This module contains DaVinci board Rev B specific defines and
*       macros for microcode.
*
*******************************************************************************
*
*   Changes:
*
*    DATE     REVISION  DESCRIPTION                             AUTHOR
*  --------   --------  -------------------------------------------------------
*  09/26/95     1.00    Original                                Goran Devic
*
******************************************************************************/
#ifndef __DAVINCI__
#define __DAVINCI__

#include "type.h"

/******************************************************************************
*
*  Display Context structure definition
*
******************************************************************************/

typedef struct StructDC
{
    DWORD * pdwBoardPtr;        /* Pointer to the first meg of DaVinci board */
    DWORD * pdwBoardPtr2;       /* Pointer to the second meg                 */
    DWORD * pdwDLbuild;         /* Ptr for building the display list         */
    
    DWORD   dwTotalMemory;      /* Amount of private memory installed (Kb)   */
    WORD    wResolution;        /* Graphics mode that is set                 */
    WORD    wColors;            /* Color mode that is set                    */

    WORD    wErrorCode;         /* Error code of the last error              */

    BYTE    bQuality;           /* Speed/Quality dial variable               */
    BYTE    bPing;              /* Execution display list toggle             */

/*    LL_DisplayContext UserInfo; */ /* User relevant information about the state */
                                /* of the graphics engine                    */

} TypeDC;

extern TypeDC DC;               /* The system wide Display Context           */


/******************************************************************************
*
*  System defines that are not relevant to the users
*
******************************************************************************/

/* The last error number (they start with 0 and have contigous numbers)      */
#define LL_LAST_ERR     LLE_SYSTEM

/* The last operation code (they start with 0 and have contigous numbers)    */
#define LL_LAST_OP      LL_SET_DISPLAY_PAGE


/******************************************************************************
*
*  Opcode definition for microinstructions
*
******************************************************************************/

#define BLUE_BANK               0x00000008
#define GREEN_BANK              0x00000080
#define RED_BANK                0x00000800

#define VECTF1P                 0x00008004
#define VECTH1P                 0x02008004
#define VECTF2PF                0x00408004
#define VECTH2PF                0x02408004
#define VECTF2P                 0x00808004
#define VECTH2P                 0x02808004
#define VECTF3P                 0x00C08004
#define VECTH3P                 0x02C08004
#define VECTFZ2P                0x04800006
#define VECTHZ2P                0x06800006
#define VECTFZ3P                0x04C00006
#define VECTHZ3P                0x06C00006
#define VECTFZG2P               0x0C800008
#define VECTHZG2P               0x0E800008
#define VECTFZG3P               0x0CC00008
#define VECTHZG3P               0x0EC00008

#define PVECTF1P                0x00008080
#define PVECTH1P                0x02008080
#define PVECTF2PF               0x00408080
#define PVECTH2PF               0x02408080
#define PVECTF2P                0x00808080
#define PVECTH2P                0x02808080
#define PVECTF3P                0x00C08080
#define PVECTH3P                0x02C08080
#define PVECTFZ2P               0x04800080
#define PVECTHZ2P               0x06800080
#define PVECTFZ3P               0x04C00080
#define PVECTHZ3P               0x06C00080
#define PVECTFZG2P              0x0C800080
#define PVECTHZG2P              0x0E800080
#define PVECTFZG3P              0x0CC00080
#define PVECTHZG3P              0x0EC00080

#define POLYF1P                 0x1000800C
#define POLYH1P                 0x1200800C
#define POLYF2PF                0x1040800C
#define POLYH2PF                0x1240800C
#define POLYF2P                 0x1080800C
#define POLYH2P                 0x1280800C
#define POLYF3P                 0x10C0800C
#define POLYH3P                 0x12C0800C
#define POLYFZ2P                0x1480000E
#define POLYHZ2P                0x1680000E
#define POLYFZ3P                0x14C0000E
#define POLYHZ3P                0x16C0000E
#define POLYFZG2P               0x1C800010
#define POLYHZG2P               0x1E800010
#define POLYFZG3P               0x1CC00010
#define POLYHZG3P               0x1EC00010
#define POLYFZGA2P              0x18800012
#define POLYHZGA2P              0x1A800012
#define POLYFZGA3P              0x18C00012
#define POLYHZGA3P              0x1AC00012

#define BLTF                    0x2040c004
#define BLTZ                    0x21408004
#define BLTFZ                   0x23400004
#define BLTH                    0x2240c005
#define BLTFF                   0x2480c005
#define BLTFH                   0x2280c005
#define BLTZH                   0x2180c005
#define BLTHH                   0x2680c005
#define BLTHF                   0x2C80c005
#define BLTHZ                   0x2D808005
#define BLTHFZ                  0x28C00000
#define PBLTHFZ                 0x29C00000
#define BLTHHZ                  0x2AC00000
#define PBLTHHZ                 0x2BC00000
#define BLTSS                   0x3580c080
#define BLTSC                   0x3EC0c080

#define LOAD_SHORT              0x80000000
#define LOAD_LONG               0xC0000080
#define SNOP                    0x8F000000

#define STORE                   0x60000000
#define RETURN                  0x40000000
#define BRANCH                  0x41000000
#define CALL                    0x42000000
#define START                   0x43000000
#define IDLE                    0x44000000
#define WAIT                    0x45000000
#define INTOUT                  0x46000000
#define CLEAR                   0x57000000
#define FLUSH                   0x5F000000

#define NFIELD_LSHIFT           16
#define ZFIELD_LSHIFT           14
#define MFIELD_LSHIFT           8
#define REG_LSHIFT              24

/***************************************************************************
 *
 *  Parameter definitions for opcodes
 */

/* NFIELD OPTIONS */
/* option 1 */
#define         SRCA_PATB       0x00        /* alu isrc a/pattern b         */
#define         PATA_PATB       0x10        /* pattern a/pattern b          */
#define         SRCA_OFFB       0x20        /* isrc a/offset b              */
#define         PATA_OFFB       0x30        /* pattern a/offset b           */

/* option 2 */
#define         IOUTPAT         0x00        /* intensity interpolator       */
#define         SRCPAT          0x04        /* isrc                         */
#define         PRAMPAT         0x08        /* pattern for c1 and c0        */

/* option 3 */
#define         AOUTALF         0x00        /* alpha interpolator           */
#define         SRCALF          0x01        /* isrc                         */
#define         PRAMALF         0x02        /* pattern for a1 and a0        */

/* ZFIELD OPTIONS */
#define         ZNORMAL         0x00        /* normal z buffer op           */
#define         ZMASK           0x01        /* compare z update frame if true*/
                                            /* but do not write z           */
#define         ZALWAYS         0x02        /* always write frame and z     */
#define         ZFLAT           0x03        /* always to frame, mask z      */

/*

    M-field (alu)  (bit 0 is CIN)
    -------------
    
    00000?  -   A +  B + CIN
    00001?  -   B + ~A + CIN        (B - A + CIN)   2's comp needs CIN=1
    00010?  -   A + ~B + CIN        (A - B + CIN)   2's comp needs CIN=1
    00011?  -  ~A + ~B + CIN

    01000x  -   A ^  B
    01001x  - ~(A ^  B)
    01010x  - ~(A ^  B)
    01011x  -   A ^  B

    10000x  - ~(A ^  B)
    10001x  -   A ^  B
    10010x  -   A ^  B
    10011x  - ~(A ^  B)

    00100?  - ~(A &  B) ^ CIN
    00101?  -  (A | ~B) ^ CIN
    00110?  - (~A |  B) ^ CIN
    00111?  -  (A |  B) ^ CIN

    01100x  - ~(A &  B)
    01101x  -   A | ~B
    01110x  -  ~A |  B
    01111x  -   A |  B

    10100x  -   A &  B
    10101x  -  ~A &  B
    10110x  -   A & ~B
    10111x  - ~(A |  B)

    11000x  -   1
    11001x  -  ~B
    11011x  -   B
    11100x  -   0
    11110x  -  ~A
    11111x  -   A
*/

/* M FIELD OPTIONS (alu modes) */
#define         A_ONLY                   0x3e            /* changed */
#define         ABAR                     0x3c
#define         B_ONLY                   0x36
#define         BBAR                     0x32
#define         ALL_ZEROS                0x38
#define         ALL_ONES                 0x30
#define         NEG_1                    ALL_ONES

#define         A_NAND_B                 0x18
#define         A_NOR_B                  0x2e
#define         A_XNOR_B                 0x20

#define         A_AND_B                  0x28
#define         ABAR_AND_B               0x2a
#define         A_AND_BBAR               0x2c
#define         ABAR_AND_BBAR            A_NOR_B

#define         A_OR_B                   0x1e
#define         ABAR_OR_B                0x1c
#define         A_OR_BBAR                0x1a
#define         ABAR_OR_BBAR             A_NAND_B

#define         ABAR_NAND_B              A_OR_BBAR
#define         A_NAND_BBAR              ABAR_OR_B
#define         ABAR_NAND_BBAR           A_OR_B

#define         ABAR_NOR_B               A_AND_BBAR
#define         A_NOR_BBAR               ABAR_AND_B
#define         ABAR_NOR_BBAR            A_AND_B

#define         A_XOR_B                  0x22            /* changed */
#define         ABAR_XOR_B               A_XNOR_B
#define         A_XOR_BBAR               A_XNOR_B

#define         A_MINUS_B                0x5
#define         B_MINUS_A                0x3
#define         A_MINUS_B_MINUS_1        0x4
#define         B_MINUS_A_MINUS_1        0x2

#define         A_PLUS_B_PLUS_1          0x1
#define         A_PLUS_B                 0x0


/***************************************************************************
 *
 *  Core registers for DaVInci board
 */

#define PATTERN_RAM             0x00000000      /* eight 32-bit 16x16 */
#define PATTERN_RAM1            0x00000001
#define PATTERN_RAM2            0x00000002
#define PATTERN_RAM3            0x00000003
#define PATTERN_RAM4            0x00000004
#define PATTERN_RAM5            0x00000005
#define PATTERN_RAM6            0x00000006
#define PATTERN_RAM7            0x00000007
#define C_AND_A_REG             0x00000008      /* a1,a0,c1,c0 */
#define OFFSET_REG              0x00000009      /* x,x,offset1,offset0 */
#define BNDH_REG                0x0000000a      /* x,x,x,boundh */
#define BNDL_REG                0x0000000b      /* x,x,x,boundl */
#define PMASK_REG               0x0000000c      /* x,x,x,pmask */
#define DASH_REG                0x0000000d      /* 32-bit line pattern */
#define DISABLE_REG             0x0000000e      /* 32-bit line pattern */
#define WRSYNC_REG              0x00000010
#define REF0_REG                0x00000010
#define REF1_REG                0x00000011
#define REF2_REG                0x00000012
#define REF3_REG                0x00000013
#define REF4_REG                0x00000014
#define REF5_REG                0x00000015
#define REF6_REG                0x00000016
#define REF7_REG                0x00000017
#define REF8_REG                0x00000018
#define REF9_REG                0x00000019
#define REFA_REG                0x0000001a
#define REFB_REG                0x0000001b
#define REFC_REG                0x0000001c
#define REFD_REG                0x0000001d
#define REFE_REG                0x0000001e
#define REFF_REG                0x0000001f
#define CONTROL_REG             0x00000020
/*      bit 21, 20  z control
    0       0    new >= old
    1       0    new <  old
    0       1    new >  old
    1       1    new =  old
    bit 19, 18  alpha control
    1       1    disable
    1       0    idest=1/a(host)*(isrc+offset1)
    0       1    idest=(1-1/a(host))*idestold-offset2
    0       0    idest=1/a(host)*(isrc+offset1)
            +(1-1/a(host))*(idestold+offset1)-offset2
    bit 17  - zoff  kills z compare
    bit 16  - rndh  render to host
    bit 15  - slave 1
    bit 14  - dbuff offsets y by 2048
    bit 13, 12 - host DRAM memory config
    bit 11, 10 - VRAM/DRAM memory config
    bit 9   - zms z most significant bank select
    bit 8   - ztype 0 - 256k x n 1 - 1M x n devices
          also used for special both bank z clear with 256k DRAMs
    bit 7   - global mask - pattern RAM used for transparency (1 masks)
    bit 6, 5    pattern control
    1       1       every step or pixel incs (vert lines)
    1       0       dash register enable
    0       1       pattern register enable
    0       0       disable register select
    bit 4   - inverts pattern data

    operates on idest
    -----------------
    bit 3   - ccin - color inclusive
    bit 2   - ccmp - color compare
    bit 1   - csat - color saturation to bounds
    bit 0   - csrc - isrc for bound high

    01xx    - color compare, mask to bounds, inclusive
    11xx    - color compare, mask to bounds, exclusive
    0xx1    - color compare, saturate to isrc, inclusive
    1xx1    - color compare, saturate to isrc, exclusive
    0x1x    - color compare, saturate to bounds, inclusive
    1x1x    - color compare, saturate to bounds, exclusive
*/
#define MASK_REG                0x00000022
#define MASK_B_REG              0x00000023
/*
    !! WRITE ONLY !!  
    ones mask, zeros enable
    bits 31-24 interrupt enable (clear) masks for status bits 15-8
    bit 23 - message mask for status bits 7-0
    bit 22 - control mask for status bits 29-24
    bit 21 - z control bit mask for bits 21,20
    bit 20 - nmi mask for status bit 29
    bits 19-0 identical to control for write mask
*/
#define STATUS_REG              0x00000024
/*
    bit 31 idle (ro)
    bit 30 hold (ro)
    bit 29 nmi 1 halts WARP/screen refresh continues
    bit 28 lock a 1 syncs refreshes between WARP busses
    bit 27 sync 1->0 starts count (for multiple warps)
    bit 26 hld_ req
    bit 25 idle_ req
    bit 24 local (ro) 1 - processor 0 - coprocessor
    bits 23 - 16 ro interrupt pending (cleared on read)
        idle
        inst intout instruction interrupt
        msg
        vstat
        lp   local VRAM/DRAM refresh panic
        hp   host DRAM refresh panic
        ext
        int
    bits 15 - 8 rw interrupt control/clear
        idle 
        inst intout instruction interrupt
        msg
        vstat
        lp   local VRAM/DRAM refresh panic
        hp   host DRAM refresh panic
        ext
        int
    bits 7-0 rw message interrupt status
*/
#define BANK_ENABLE_REG         0x00000025
#define BANK_MASK_REG           0x00000026
#define BANK_COMPARE_REG        0x00000027
#define YMAIN_REG               0x00000028
#define YSLOPE_REG              0x00000029
#define XMAIN_REG               0x0000002a
#define XSLOPE_REG              0x0000002b
#define ZMAIN_REG               0x0000002c
#define ZSLOPE_REG              0x0000002d
#define IMAIN_REG               0x0000002e
#define ISLOPE_REG              0x0000002f
#define W1MAIN_REG              0x00000030
#define W1SLOPE_REG             0x00000031
#define W2MAIN_REG              0x00000032
#define W2SLOPE_REG             0x00000033
#define ZERROR_REG              0x00000034
#define ZORTHO_REG              0x00000035
#define IERROR_REG              0x00000036
#define IORTHO_REG              0x00000037
#define AORTHO_REG              0x00000038
#define A_MAIN_SLOPE_REG        0x00000039
#define HOST_PITCH_REG          0x0000003a
#define HOST_INSTRUCTION_REG    0x0000003b
#define HOST_BASE_REG           0x0000003c
#define HOST_OFFSET_REG         0x0000003d


/***************************************************************************
 *
 *  Macros returning Warp opcodes
 */

#define WARP_PRIM_FIELDS(n, z, m)   (n << NFIELD_LSHIFT)|(z << ZFIELD_LSHIFT)|(m << MFIELD_LSHIFT)
#define WARP_BLT_FIELDS(n, m)       (n << NFIELD_LSHIFT)|(m << MFIELD_LSHIFT)

/***************************************************************************/
/* make_ro generates the opcode for the instruction taking n, z and m fields
 * Those instr are: Vectors, Poly-vectors, Polygons
 */

#define make_ro(instr,n,z,m) (instr|(n << NFIELD_LSHIFT)|(z << ZFIELD_LSHIFT)|(m << MFIELD_LSHIFT))

/***************************************************************************/
/* blit_ro generates the opcode for the blit instructions requiring
 * n and m fields: BLTF, BLTZ, BLTFZ, BLTH
 *                 BLTFF, BLTHF, BLTZH, BLTHH, BLTFH, BLTHZ
 */

#define blit_ro(instr,n,m)    (instr|(n << NFIELD_LSHIFT)|(m << MFIELD_LSHIFT))

/***************************************************************************/
/* STORE */

#define store_ro(core_reg, store_addr) STORE|(core_reg << REG_LSHIFT)|(store_addr & 0x00ffffff)

/***************************************************************************/
/* LOAD */

#define load_short_ro(core_reg, reg_value) LOAD_SHORT|(core_reg << REG_LSHIFT)|(reg_value & 0x00ffffff)

#define load_long_ro(core_reg, load_count) LOAD_LONG|(core_reg << REG_LSHIFT)|(load_count & 0x0000007f)

/***************************************************************************/
/* STALL - with nop */

#define snop_ro()                              SNOP

/***************************************************************************/
/* CONTROL */

#define branch_ro(branch_addr)                 BRANCH|(branch_addr & 0x00ffffff)

#define call_ro(call_addr)                     CALL |(call_addr & 0x00ffffff)

#define start_ro()                             START

#define idle_ro()                              IDLE

#define clear_ro()                             CLEAR

#define flush_ro()                             FLUSH

#define wait_ro()                              WAIT

#define intout_ro()                            INTOUT


/***************************************************************************/
/* VECTORS - frame buffer */

/* One-pipe flat, 2d vectors */
#define vectf1p_ro(n, z, m)         VECTF1P | WARP_PRIM_FIELDS(n,z,m) 

/* Fast-two-pipe flat, 2d vectors */
#define vectf2pf_ro(n, z, m)        VECTF2PF | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, 2d vectors */
#define vectf2p_ro(n, z, m)         VECTF2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, 2d vectors */
#define vectf3p_ro(n, z, m)         VECTF3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, z-buffered vectors */
#define vectfz2p_ro(n, z, m)        VECTFZ2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, z-buffered vectors */
#define vectfz3p_ro(n, z, m)        VECTFZ3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe shaded, z-buffered vectors */
#define vectfzg2p_ro(n, z, m)       VECTFZG2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe shaded, z-buffered vectors */
#define vectfzg3p_ro(n, z, m)       VECTFZG3P | WARP_PRIM_FIELDS(n,z,m) 

/***************************************************************************/
/* VECTORS - host buffer */

/* One-pipe flat, 2d vectors */
#define vecth1p_ro(n, z, m)         VECTH1P | WARP_PRIM_FIELDS(n,z,m) 

/* Fast-two-pipe flat, 2d vectors */
#define vecth2pf_ro(n, z, m)        VECTH2PF | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, 2d vectors */
#define vecth2p_ro(n, z, m)         VECTH2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, 2d vectors */
#define vecth3p_ro(n, z, m)         VECTH3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, z-buffered vectors */
#define vecthz2p_ro(n, z, m)        VECTHZ2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, z-buffered vectors */
#define vecthz3p_ro(n, z, m)        VECTHZ3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe shaded, z-buffered vectors */
#define vecthzg2p_ro(n, z, m)       VECTHZG2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe shaded, z-buffered vectors */
#define vecthzg3p_ro(n, z, m)       VECTHZG3P | WARP_PRIM_FIELDS(n,z,m) 

/***************************************************************************/
/* POLY VECTORS - frame buffer */

/* One-pipe flat, 2d poly vectors */
#define pvectf1p_ro(n, z, m)        PVECTF1P | WARP_PRIM_FIELDS(n,z,m) 

/* Fast-two-pipe flat, 2d poly vectors */
#define pvectf2pf_ro(n, z, m)       PVECTF2PF | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, 2d poly vectors */
#define pvectf2p_ro(n, z, m)        PVECTF2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, 2d poly vectors */
#define pvectf3p_ro(n, z, m)        PVECTF3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, z-buffered poly vectors */
#define pvectfz2p_ro(n, z, m)       PVECTFZ2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, z-buffered poly vectors */
#define pvectfz3p_ro(n, z, m)       PVECTFZ3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe shaded, z-buffered poly vectors */
#define pvectfzg2p_ro(n, z, m)      PVECTFZG2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe shaded, z-buffered poly vectors */
#define pvectfzg3p_ro(n, z, m)      PVECTFZG3P | WARP_PRIM_FIELDS(n,z,m) 

/***************************************************************************/
/* POLY VECTORS - host buffer */

/* One-pipe flat, 2d poly vectors */
#define pvecth1p_ro(n, z, m)        PVECTH1P | WARP_PRIM_FIELDS(n,z,m) 

/* Fast-two-pipe flat, 2d poly vectors */
#define pvecth2pf_ro(n, z, m)       PVECTH2PF | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, 2d poly vectors */
#define pvecth2p_ro(n, z, m)        PVECTH2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, 2d poly vectors */
#define pvecth3p_ro(n, z, m)        PVECTH3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, z-buffered poly vectors */
#define pvecthz2p_ro(n, z, m)       PVECTHZ2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, z-buffered poly vectors */
#define pvecthz3p_ro(n, z, m)       PVECTHZ3P | WARP_PRIM_FIELDS(n,z,m)

/* Two-pipe shaded, z-buffered poly vectors */
#define pvecthzg2p_ro(n, z, m)      PVECTHZG2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe shaded, z-buffered poly vectors */
#define pvecthzg3p_ro(n, z, m)      PVECTHZG3P | WARP_PRIM_FIELDS(n,z,m) 

/***************************************************************************/
/* POLYGONS - frame buffer */

/* One-pipe flat, 2d polygons */
#define polyf1p_ro(n, z, m)         POLYF1P | WARP_PRIM_FIELDS(n,z,m) 

/* Fast-two-pipe flat, 2d polygons */
#define polyf2pf_ro(n, z, m)        POLYF2PF | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, 2d polygons */
#define polyf2p_ro(n, z, m)         POLYF2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, 2d polygons */
#define polyf3p_ro(n, z, m)         POLYF3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, z-buffered polygons */
#define polyfz2p_ro(n, z, m)        POLYFZ2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, z-buffered polygons */
#define polyfz3p_ro(n, z, m)        POLYFZ3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe shaded, z-buffered polygons */
#define polyfzg2p_ro(n, z, m)       POLYFZG2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe shaded, z-buffered polygons */
#define polyfzg3p_ro(n, z, m)       POLYFZG3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe shaded, z-buffered, alphaout polygons */
#define polyfzga2p_ro(n, z, m)      POLYFZGA2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe shaded, z-buffered, alpha-blended polygons */
#define polyfzga3p_ro(n, z, m)      POLYFZGA3P | WARP_PRIM_FIELDS(n,z,m) 

/***************************************************************************/
/* POLYGONS - host buffer */

/* One-pipe flat, 2d polygons */
#define polyh1p_ro(n, z, m)         POLYH1P | WARP_PRIM_FIELDS(n,z,m) 

/* Fast-two-pipe flat, 2d polygons */
#define polyh2pf_ro(n, z, m)        POLYH2PF | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, 2d polygons */
#define polyh2p_ro(n, z, m)         POLYH2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, 2d polygons */
#define polyh3p_ro(n, z, m)         POLYH3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe flat, z-buffered polygons */
#define polyhz2p_ro(n, z, m)        POLYHZ2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe flat, z-buffered polygons */
#define polyhz3p_ro(n, z, m)        POLYHZ3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe shaded, z-buffered polygons */
#define polyhzg2p_ro(n, z, m)       POLYHZG2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe shaded, z-buffered polygons */
#define polyhzg3p_ro(n, z, m)       POLYHZG3P | WARP_PRIM_FIELDS(n,z,m) 

/* Two-pipe shaded, z-buffered, alphaout polygons */
#define polyhzga2p_ro(n, z, m)      POLYHZGA2P | WARP_PRIM_FIELDS(n,z,m) 

/* Three-pipe shaded, z-buffered, alpha-blended polygons */
#define polyhzga3p_ro(n, z, m)      POLYHZGA3P | WARP_PRIM_FIELDS(n,z,m) 

/***************************************************************************/
/* BLTS - fixed */

/* Blt fixed to frame-buffer */
#define bltf_ro(n, m)               BLTF | WARP_BLT_FIELDS(n,m) 

/* Blt fixed to z-buffer */
#define bltz_ro(n, m)               BLTZ | WARP_BLT_FIELDS(n,m) 

/* Blt fixed to z-buffer and frame-buffer */
#define bltfz_ro(n, m)              BLTFZ | WARP_BLT_FIELDS(n,m) 

/* Blt fixed to host-buffer */
#define blth_ro(n, m)               BLTH | WARP_BLT_FIELDS(n,m) 

/***************************************************************************/
/* BLTS - screen */

/* Blt frame-buffer to frame-buffer */
#define bltff_ro(n, m)              BLTFF | WARP_BLT_FIELDS(n,m) 

/* Blt host-buffer to frame-buffer */
#define blthf_ro(n, m)              BLTHF | WARP_BLT_FIELDS(n,m) 

/* Blt z-buffer to host-buffer */
#define bltzh_ro(n, m)              BLTZH | WARP_BLT_FIELDS(n,m) 

/* Blt complex from host-buffer to screen with z-compare */
#define blthfz_ro(n, z, m)          BLTHFZ | WARP_PRIM_FIELDS(n,z,m) 

/* Blt polygonal from host-buffer to screen with z-compare */
#define pblthfz_ro(n, z, m)         PBLTHFZ | WARP_PRIM_FIELDS(n,z,m) 

/* Blt string simplex from host-buffer to screen */
#define bltss_ro(n, m, count)       BLTSS | WARP_BLT_FIELDS(n,m) | count 

/* Blt string complex from host-buffer to screen */
#define bltsc_ro(n, m, count)       BLTSC | WARP_BLT_FIELDS(n,m) | count 

/***************************************************************************/
/* BLTS - host */

/* Blt host-buffer to host-buffer */
#define blthh_ro(n, m)              BLTHH | WARP_BLT_FIELDS(n,m) 

/* Blt frame-buffer to host-buffer */
#define bltfh_ro(n, m)              BLTFH | WARP_BLT_FIELDS(n,m) 

/* Blt host-buffer to z-buffer */
#define blthz_ro(n, m)              BLTHZ | WARP_BLT_FIELDS(n,m) 

/* Blt (complex) host-buffer to host-buffer with z-compare */
#define blthhz_ro(n, z, m)          BLTHHZ | WARP_PRIM_FIELDS(n,z,m) 

/* Blt (polygonal) host-buffer to host-buffer with z-compare */
#define pblthhz_ro(n, z, m)         PBLTHHZ | WARP_PRIM_FIELDS(n,z,m) 

/***************************************************************************/


/******************************************************************************
*
*  Private functions of the library
*
******************************************************************************/
extern void out_warp_io( const int reg, const DWORD data );


#endif
