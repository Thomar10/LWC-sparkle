// NIST-developed software is provided by NIST as a public service. You may
// use, copy and distribute copies of the software in any medium, provided that
// you keep intact this entire notice. You may improve, modify and create
// derivative works of the software or any portion of the software, and you may
// copy and distribute such modifications or works. Modified works should carry
// a notice stating that you changed the software and should note the date and
// nature of any such change. Please explicitly acknowledge the National
// Institute of Standards and Technology as the source of the software.
//
// NIST-developed software is expressly provided "AS IS." NIST MAKES NO
// WARRANTY OF ANY KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF
// LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST
// NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE
// UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED. NIST
// DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE USE OF THE
// SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE
// CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.
//
// You are solely responsible for determining the appropriateness of using and
// distributing the software and you assume all risks associated with its use,
// including but not limited to the risks and costs of program errors,
// compliance with applicable laws, damage to or loss of data, programs or
// equipment, and the unavailability or interruption of operation. This
// software is not intended to be used in any situation where a failure could
// cause risk of injury or damage to property. The software developed by NIST
// employees is not subject to copyright protection within the United States.

// disable deprecation for sprintf and fopen
#ifdef _MSC_VER
#define _CRT_SECURE_NO_WARNINGS
#endif

#include <stdint-gcc.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "api.h"
#include "crypto_aead.h"

#define KAT_SUCCESS 0
#define KAT_FILE_OPEN_ERROR -1
#define KAT_DATA_ERROR -3
#define KAT_CRYPTO_FAILURE -4

#define MAX_FILE_NAME 256
#define MAX_MESSAGE_LENGTH 32
#define MAX_ASSOCIATED_DATA_LENGTH 32

typedef unsigned char UChar;
typedef unsigned long long int ULLInt;

void init_buffer(UChar *buffer, ULLInt numbytes);

void fprint_bstr(FILE *fp, const char *label, const UChar *data, ULLInt length);

int generate_test_vectors(void);


int main(int argc, char *argv[]) {
    uint32_t state[8];
    uint8_t *bufferNonce;
    uint8_t *bufferKey;
    FILE *fileptr;
    long filelen;

    fileptr = fopen("nonce", "rb");
    fseek(fileptr, 0, SEEK_END);
    filelen = ftell(fileptr);
    rewind(fileptr);

    bufferNonce = (uint8_t *) malloc(filelen * sizeof(uint8_t));
    fread(bufferNonce, filelen, 1, fileptr);

    fclose(fileptr);

    FILE *fileptrKey;
    long filelenKey;

    fileptrKey = fopen("key", "rb");
    fseek(fileptrKey, 0, SEEK_END);
    filelenKey = ftell(fileptrKey);
    rewind(fileptrKey);

    bufferKey = (uint8_t *) malloc(filelenKey * sizeof(uint8_t));
    fread(bufferKey, filelenKey, 1, fileptrKey);
    fclose(fileptrKey);

    if (strcmp(argv[1], "initialize") == 0) {
        Initialize(state, bufferKey, bufferNonce);
    }

    if (strcmp(argv[1], "associate") == 0) {
        Initialize(state, bufferKey, bufferNonce);
        uint8_t *associateBuffer;
        FILE *associateFilePtr;
        long associateLength;

        associateFilePtr = fopen("associate", "rb");
        fseek(associateFilePtr, 0, SEEK_END);
        associateLength = ftell(associateFilePtr);
        rewind(associateFilePtr);

        associateBuffer = (uint8_t *) malloc(associateLength * sizeof(uint8_t));
        fread(associateBuffer, associateLength, 1, associateFilePtr);
        fclose(associateFilePtr);
        ProcessAssocData(state, associateBuffer, associateLength);
    }
    if (strcmp(argv[1], "encrypt") == 0) {
        Initialize(state, bufferKey, bufferNonce);
        uint8_t *associateBuffer;
        FILE *associateFilePtr;
        long associateLength;

        associateFilePtr = fopen("associate", "rb");
        fseek(associateFilePtr, 0, SEEK_END);
        associateLength = ftell(associateFilePtr);
        rewind(associateFilePtr);

        associateBuffer = (uint8_t *) malloc(associateLength * sizeof(uint8_t));
        fread(associateBuffer, associateLength, 1, associateFilePtr);
        fclose(associateFilePtr);
        ProcessAssocData(state, associateBuffer, associateLength);

        uint8_t *messageBuffer;
        FILE *messageFilePtr;
        long messageLength;

        messageFilePtr = fopen("message", "rb");
        fseek(messageFilePtr, 0, SEEK_END);
        messageLength = ftell(messageFilePtr);
        rewind(messageFilePtr);

        messageBuffer = (uint8_t *) malloc(messageLength * sizeof(uint8_t));
        fread(messageBuffer, messageLength, 1, messageFilePtr);
        fclose(messageFilePtr);

        // No tag for this test.
        int cipherLength = messageLength + CRYPTO_ABYTES;
        uint8_t cipher[cipherLength];
        // Zero out to be sure
        for (int i = 0; i < cipherLength; i++) {
            cipher[i] = 0;
        }

        ProcessPlainText(state, cipher, messageBuffer, messageLength);
        FILE *writeCipher = fopen("cipher", "wb");
        if (writeCipher) {
            fwrite(cipher, cipherLength, 1, writeCipher);
        }
        fclose(writeCipher);
    }
    if (strcmp(argv[1], "finalize") == 0) {
        Initialize(state, bufferKey, bufferNonce);
        uint8_t *associateBuffer;
        FILE *associateFilePtr;
        long associateLength;

        associateFilePtr = fopen("associate", "rb");
        fseek(associateFilePtr, 0, SEEK_END);
        associateLength = ftell(associateFilePtr);
        rewind(associateFilePtr);

        associateBuffer = (uint8_t *) malloc(associateLength * sizeof(uint8_t));
        fread(associateBuffer, associateLength, 1, associateFilePtr);
        fclose(associateFilePtr);
        ProcessAssocData(state, associateBuffer, associateLength);

        uint8_t *messageBuffer;
        FILE *messageFilePtr;
        long messageLength;

        messageFilePtr = fopen("message", "rb");
        fseek(messageFilePtr, 0, SEEK_END);
        messageLength = ftell(messageFilePtr);
        rewind(messageFilePtr);

        messageBuffer = (uint8_t *) malloc(messageLength * sizeof(uint8_t));
        fread(messageBuffer, messageLength, 1, messageFilePtr);
        fclose(messageFilePtr);
        // Cipher is anyway unused
        uint8_t cipher[48];

        ProcessPlainText(state, cipher, messageBuffer, messageLength);
        Finalize(state, bufferKey);
    }
    if (strcmp(argv[1], "generateTag") == 0) {
        Initialize(state, bufferKey, bufferNonce);
        uint8_t *associateBuffer;
        FILE *associateFilePtr;
        long associateLength;

        associateFilePtr = fopen("associate", "rb");
        fseek(associateFilePtr, 0, SEEK_END);
        associateLength = ftell(associateFilePtr);
        rewind(associateFilePtr);

        associateBuffer = (uint8_t *) malloc(associateLength * sizeof(uint8_t));
        fread(associateBuffer, associateLength, 1, associateFilePtr);
        fclose(associateFilePtr);
        ProcessAssocData(state, associateBuffer, associateLength);

        uint8_t *messageBuffer;
        FILE *messageFilePtr;
        long messageLength;

        messageFilePtr = fopen("message", "rb");
        fseek(messageFilePtr, 0, SEEK_END);
        messageLength = ftell(messageFilePtr);
        rewind(messageFilePtr);

        messageBuffer = (uint8_t *) malloc(messageLength * sizeof(uint8_t));
        fread(messageBuffer, messageLength, 1, messageFilePtr);
        fclose(messageFilePtr);
        int cipherLength = messageLength + CRYPTO_ABYTES;
        uint8_t cipher[cipherLength];
        // Zero out to be sure
        for (int i = 0; i < cipherLength; i++) {
            cipher[i] = 0;
        }

        ProcessPlainText(state, cipher, messageBuffer, messageLength);
        Finalize(state, bufferKey);
        GenerateTag(state, (cipher + messageLength));
        FILE *writeCipher = fopen("cipher", "wb");
        if (writeCipher) {
            fwrite(cipher, cipherLength, 1, writeCipher);
        }
        fclose(writeCipher);
    }
    // crypto_aead_encrypt
    if (strcmp(argv[1], "fullFunction") == 0) {
        Initialize(state, bufferKey, bufferNonce);
        uint8_t *associateBuffer;
        FILE *associateFilePtr;
        long associateLength;

        associateFilePtr = fopen("associate", "rb");
        fseek(associateFilePtr, 0, SEEK_END);
        associateLength = ftell(associateFilePtr);
        rewind(associateFilePtr);

        associateBuffer = (uint8_t *) malloc(associateLength * sizeof(uint8_t));
        fread(associateBuffer, associateLength, 1, associateFilePtr);
        fclose(associateFilePtr);
        ProcessAssocData(state, associateBuffer, associateLength);

        uint8_t *messageBuffer;
        FILE *filemessageFilePtrtrMsg;
        long messageLength;

        filemessageFilePtrtrMsg = fopen("message", "rb");
        fseek(filemessageFilePtrtrMsg, 0, SEEK_END);
        messageLength = ftell(filemessageFilePtrtrMsg);
        rewind(filemessageFilePtrtrMsg);

        messageBuffer = (uint8_t *) malloc(messageLength * sizeof(uint8_t));
        fread(messageBuffer, messageLength, 1, filemessageFilePtrtrMsg);
        fclose(filemessageFilePtrtrMsg);
        int cipherLength = messageLength + CRYPTO_ABYTES;
        uint8_t cipher[cipherLength];
        // Zero out to be sure
        for (int i = 0; i < cipherLength; i++) {
            cipher[i] = 0;
        }
        ULLInt clen;
        crypto_aead_encrypt(cipher, &clen, messageBuffer, messageLength, associateBuffer,
                            associateLength, NULL, bufferNonce, bufferKey);
        FILE *writeCipher = fopen("cipher", "wb");
        if (writeCipher) {
            fwrite(cipher, cipherLength, 1, writeCipher);
        }
        fclose(writeCipher);
    }
    if (strcmp(argv[1], "encryptAndDecrypt") == 0) {
        Initialize(state, bufferKey, bufferNonce);
        uint8_t *associateBuffer;
        FILE *associateFilePtr;
        long associateLength;

        associateFilePtr = fopen("associate", "rb");
        fseek(associateFilePtr, 0, SEEK_END);
        associateLength = ftell(associateFilePtr);
        rewind(associateFilePtr);

        associateBuffer = (uint8_t *) malloc(associateLength * sizeof(uint8_t));
        fread(associateBuffer, associateLength, 1, associateFilePtr);
        fclose(associateFilePtr);
        ProcessAssocData(state, associateBuffer, associateLength);

        uint8_t *messageBuffer;
        FILE *messageFilePtr;
        long messageLength;

        messageFilePtr = fopen("message", "rb");
        fseek(messageFilePtr, 0, SEEK_END);
        messageLength = ftell(messageFilePtr);
        rewind(messageFilePtr);

        messageBuffer = (uint8_t *) malloc(messageLength * sizeof(uint8_t));
        fread(messageBuffer, messageLength, 1, messageFilePtr);
        fclose(messageFilePtr);

        int cipherLength = messageLength + CRYPTO_ABYTES;
        uint8_t cipher[cipherLength];
        // Zero out to be sure
        for (int i = 0; i < cipherLength; i++) {
            cipher[i] = 0;
        }
        ULLInt clen;
        crypto_aead_encrypt(cipher, &clen, messageBuffer, messageLength, associateBuffer,
                            associateLength, NULL, bufferNonce, bufferKey);
        uint8_t message[messageLength];
        // Zero out to be sure
        for (int i = 0; i < messageLength; i++) {
            message[i] = 0;
        }
        ULLInt mlen;
        crypto_aead_decrypt(message, &mlen, NULL, cipher, clen, associateBuffer,
                            associateLength, bufferNonce, bufferKey);

        FILE *writeMessage = fopen("messageBack", "wb");
        if (writeMessage) {
            fwrite(message, messageLength, 1, writeMessage);
        }
        fclose(writeMessage);
    }

    FILE *f = fopen("schwaemmState", "wt");
    for (int i = 0; i < 8; ++i) {
        fprintf(f, "%d\n", state[i]);
    }
    fclose(f);
}


/*
int main(void) {
  int ret;

  ret = generate_test_vectors();
  if (ret != KAT_SUCCESS) {
    fprintf(stderr, "test vector generation failed with code %d\n", ret);
  }

  return ret;
}
 */

int generate_test_vectors(void) {
    FILE *fp;
    char fileName[MAX_FILE_NAME];
    UChar key[CRYPTO_KEYBYTES];
    UChar nonce[CRYPTO_NPUBBYTES];
    UChar msg[MAX_MESSAGE_LENGTH];
    UChar msg2[MAX_MESSAGE_LENGTH];
    UChar ad[MAX_ASSOCIATED_DATA_LENGTH];
    UChar ct[MAX_MESSAGE_LENGTH + CRYPTO_ABYTES];
    ULLInt clen, mlen2;
    ULLInt mlen, adlen;
    int count = 1;
    int func_ret, ret_val = KAT_SUCCESS;

    init_buffer(key, sizeof(key));
    init_buffer(nonce, sizeof(nonce));
    init_buffer(msg, sizeof(msg));
    init_buffer(ad, sizeof(ad));

    sprintf(fileName, "LWC_AEAD_KAT_%d_%d.txt", (CRYPTO_KEYBYTES * 8),
            (CRYPTO_NPUBBYTES * 8));
    printf("File we look in = LWC_AEAD_KAT_%d_%d.txt \n", (CRYPTO_KEYBYTES * 8),
           (CRYPTO_NPUBBYTES * 8));
    if ((fp = fopen(fileName, "w")) == NULL) {
        fprintf(stderr, "Couldn't open <%s> for write\n", fileName);
        return KAT_FILE_OPEN_ERROR;
    }

    for (mlen = 0; (mlen <= MAX_MESSAGE_LENGTH) && (ret_val == KAT_SUCCESS);
         mlen++) {

        for (adlen = 0; adlen <= MAX_ASSOCIATED_DATA_LENGTH; adlen++) {

            fprintf(fp, "Count = %d\n", count++);
            fprint_bstr(fp, "Key = ", key, CRYPTO_KEYBYTES);
            fprint_bstr(fp, "Nonce = ", nonce, CRYPTO_NPUBBYTES);
            fprint_bstr(fp, "PT = ", msg, mlen);
            fprint_bstr(fp, "AD = ", ad, adlen);
            if (count == 3) {
                printf("adlen = %llu\n", adlen);
                printf("ad: %d\n", ad[0]);
                printf("ad: %d\n", ad[1]);
            }

            func_ret = crypto_aead_encrypt(ct, &clen, msg, mlen, ad, adlen, NULL,
                                           nonce, key);
            if (func_ret != 0) {
                fprintf(fp, "crypto_aead_encrypt returned <%d>\n", func_ret);
                ret_val = KAT_CRYPTO_FAILURE;
                break;
            }

            fprint_bstr(fp, "CT = ", ct, clen);
            fprintf(fp, "\n");

            func_ret = crypto_aead_decrypt(msg2, &mlen2, NULL, ct, clen, ad, adlen,
                                           nonce, key);
            if (func_ret != 0) {
                fprintf(fp, "crypto_aead_decrypt returned <%d>\n", func_ret);
                ret_val = KAT_CRYPTO_FAILURE;
                break;
            }

            if (mlen != mlen2) {
                fprintf(fp, "crypto_aead_decrypt returned bad 'mlen': Got <%llu>, \
                expected <%llu>\n",
                        mlen2, mlen);
                ret_val = KAT_CRYPTO_FAILURE;
                break;
            }

            if (memcmp(msg, msg2, ((size_t) mlen))) {
                fprintf(fp, "crypto_aead_decrypt did not recover the plaintext\n");
                ret_val = KAT_CRYPTO_FAILURE;
                break;
            }
        }
    }

    fclose(fp);
    return ret_val;
}

void fprint_bstr(FILE *fp, const char *label, const UChar *data,
                 ULLInt length) {
    ULLInt i;

    fprintf(fp, "%s", label);
    for (i = 0; i < length; i++)
        fprintf(fp, "%02X", data[i]);
    fprintf(fp, "\n");
}

void init_buffer(UChar *buffer, ULLInt numbytes) {
    ULLInt i;

    for (i = 0; i < numbytes; i++)
        buffer[i] = (UChar) i;
}
