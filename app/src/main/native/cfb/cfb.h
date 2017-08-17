#ifndef _CFB_H
#define _CFB_H

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
} cfb_context;

void cfb_encrypt(cfb_context *context, uint8_t *data, int offset, int length, uint8_t *iv);
void cfb_decrypt(cfb_context *context, uint8_t *data, int offset, int length, uint8_t *iv);


#endif