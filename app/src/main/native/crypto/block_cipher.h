#ifndef BLOCK_CIPHER_H
#define BLOCK_CIPHER_H

typedef int (*block_func)(const uint8_t *in, uint8_t *out, void *context);
#define block_encryptor block_func
#define block_decryptor block_func

typedef struct
{
	block_decryptor decrypt;
	block_encryptor encrypt;
	void *context;
} block_cipher_interface;


#endif
