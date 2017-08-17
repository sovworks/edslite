#ifndef _CTR_H
#define _CTR_H

#include <block_cipher.h>

typedef signed char s8;
typedef unsigned char u8;

#if UINT_MAX >= 4294967295UL

typedef signed short s16;
typedef signed int s32;
typedef unsigned short u16;
typedef unsigned int u32;

#define ONE32   0xffffffffU

#else

typedef signed int s16;
typedef signed long s32;
typedef unsigned int u16;
typedef uint32_t u32;

#define ONE32   0xffffffffUL

#endif

typedef struct
{
    block_cipher_interface *cipher;    
    void *next;
    void *prev;
} cipher_node;

typedef struct
{
    cipher_node *ciphers_head;
    cipher_node *ciphers_tail;
} ctr_context;

void ctr_encrypt(ctr_context *context, uint8_t *data, int offset, int length, uint8_t *iv);
void ctr_decrypt(ctr_context *context, uint8_t *data, int offset, int length, uint8_t *iv);


#endif