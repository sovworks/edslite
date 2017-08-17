#ifndef SERPENT_H
#define SERPENT_H

#include <stdint.h>

void serpent_set_key(const uint8_t userKey[], int keylen, uint8_t *ks);
void serpent_encrypt(const uint8_t *inBlock, uint8_t *outBlock, uint8_t *ks);
void serpent_decrypt(const uint8_t *inBlock,  uint8_t *outBlock, uint8_t *ks);

#endif 
