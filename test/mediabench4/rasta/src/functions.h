
/* Function prototypes for Rasta */

/***********************************************************************

 (C) 1995 US West and International Computer Science Institute,
     All rights reserved
 U.S. Patent Numbers 5,450,522 and 5,537,647

 Embodiments of this technology are covered by U.S. Patent
 Nos. 5,450,522 and 5,537,647.  A limited license for internal
 research and development use only is hereby granted.  All other
 rights, including all commercial exploitation, are retained unless a
 license has been granted by either US West or International Computer
 Science Institute.

***********************************************************************/

/* Basic analysis routines */
struct fvec *get_win(struct param *,int);

struct fvec *powspec(const struct param *, struct fvec *);

struct fvec *audspec(const struct param *, struct fvec *);

struct fvec *comp_noisepower(struct fhistory *, const struct
                             param *, struct fvec *);

void comp_Jah(struct param *pptr, struct map_param *mptr,
              int *mapset);  

struct fmat *cr_map_source_data(const struct param *pptr,
                                const struct map_param *mptr,
                                const struct fvec *ras_nl_aspectrum);

void do_mapping(const struct param *pptr,
                const struct map_param *mptr,
                const struct fmat *map_s_ptr, 
                int mapset, struct fvec *ras_nl_aspectrum);

struct fvec *nl_audspec(const struct param *, struct fvec *);

struct fvec *rasta_filt(struct fhistory *, const struct param *,
                        struct fvec *); 

struct fvec *get_delta(int);

struct fvec *get_integ(const struct param *);

struct fvec *inverse_nonlin(const struct param *, struct fvec *);

struct fvec *post_audspec(const struct param *, struct fvec *);

struct fvec *lpccep(const struct param *, struct fvec *);

void auto_to_lpc(const struct param *pptr, struct fvec *, 
                 struct fvec *, float *);

void lpc_to_cep(const struct param *pptr, struct fvec *, 
                struct fvec *);

int fft_pow(float *, float *, long, long);

/* Matrix - vector arithmetic */
void fmat_x_fvec(struct fmat *, struct fvec *, struct fvec *);

void norm_fvec(struct fvec *, float);

/* Allocation and checking */
struct fvec *alloc_fvec(int);

struct svec *alloc_svec(int);

struct fmat *alloc_fmat(int, int);

void fvec_check(char *, struct fvec *, int);

void fvec_copy(char *, struct fvec *, struct fvec *);

/* I/O */
struct fvec *get_bindata(FILE *, struct fvec *, struct param *);

struct fvec *get_ascdata(FILE *, struct fvec *);

struct fvec* get_online_bindata(struct fhistory*, struct param*);

struct fvec* get_abbotdata(struct fhistory*, struct param*);

void abbot_write_eos(struct param*);

void print_vec(const struct param *pptr, FILE *, struct fvec *, int);

float swapbytes_float(float f);

void binout_vec(const struct param* pptr, FILE *, struct fvec *);

FILE *open_out(struct param *);

void write_out(struct param *, FILE *, struct fvec *);

void fvec_HPfilter(struct fhistory*, struct param*, struct fvec*);

void load_history(struct fhistory *, const struct param *);

void save_history(struct fhistory *, const struct param *);

/* debugging aids */
void show_args(struct param *);

void show_param(struct fvec *, struct param *);

void show_vec(const struct param *pptr, struct fvec *);

void get_comline(struct param *, int, char **);

void check_args(struct param *);

void usage(char *);

