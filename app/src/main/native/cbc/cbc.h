#ifndef _CBC_H
#define _CBC_H

#include <block_cipher.h>

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
    size_t file_block_size;
} cbc_context;

void cbc_encrypt(cbc_context *context, uint8_t *data, int offset, int length, uint8_t *iv, unsigned char incrementIV);
void cbc_decrypt(cbc_context *context, uint8_t *data, int offset, int length, uint8_t *iv, unsigned char incrementIV);


#endif