//
// Created by Thomas Luxhøj on 02-11-2022.
//

#ifndef SPARKLE_IMPLEMENTATIONS_CRYPTO_AEAD_SCHWAEMM128128V2_ADD_ARMV7M_SMALL_CRYPTO_AEAD_H_
#define SPARKLE_IMPLEMENTATIONS_CRYPTO_AEAD_SCHWAEMM128128V2_ADD_ARMV7M_SMALL_CRYPTO_AEAD_H_

typedef unsigned char UChar;
typedef unsigned long long int ULLInt;

int crypto_aead_decrypt(UChar *m, ULLInt *mlen, UChar *nsec, const UChar *c,
                        ULLInt clen, const UChar *ad, ULLInt adlen,
                        const UChar *npub, const UChar *k);

int crypto_aead_encrypt(UChar *c, ULLInt *clen, const UChar *m, ULLInt mlen,
                        const UChar *ad, ULLInt adlen, const UChar *nsec,
                        const UChar *npub, const UChar *k);

void Initialize(uint32_t *state, const uint8_t *key, const uint8_t *nonce);

void ProcessAssocData(uint32_t *state, const uint8_t *in, size_t inlen);

void ProcessPlainText(uint32_t *state, uint8_t *out, const uint8_t *in,
                      size_t inlen);

void Finalize(uint32_t *state, const uint8_t *key);

void GenerateTag(uint32_t *state, uint8_t *tag);

#endif // SPARKLE_IMPLEMENTATIONS_CRYPTO_AEAD_SCHWAEMM128128V2_ADD_ARMV7M_SMALL_CRYPTO_AEAD_H_
