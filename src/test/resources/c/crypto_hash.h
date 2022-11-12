//
// Created by Thomas Luxhøj on 02-11-2022.
//


#include <stdint.h>

typedef unsigned char UChar;
typedef unsigned long long int ULLInt;

int crypto_hash(UChar *out, const UChar *in, ULLInt inlen);

void Initialize(uint32_t *state, const uint8_t *key, const uint8_t *nonce);

void ProcessMessage(uint32_t *state, const uint8_t *in, size_t inlen);

void Finalize(uint32_t *state, const uint8_t *key);


