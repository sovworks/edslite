#ifndef _XTS_H
#define _XTS_H

#include <block_cipher.h>

#define XTS_SECTOR_SIZE 512

typedef struct
{
    block_cipher_interface *cipherA;
    block_cipher_interface *cipherB;
    void *next;
    void *prev;
} cipher_pair;

typedef struct
{
    cipher_pair *ciphers_head;
    cipher_pair *ciphers_tail;
    int allow_skip;
} xts_context;

void xts_encrypt(xts_context *context, uint8_t *data, int offset, int length, uint64_t start_sector_index);
void xts_decrypt(xts_context *context, uint8_t *data, int offset, int length, uint64_t start_sector_index);


#endif