

#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_crypto_modes_CTR.h"
#include <stdlib.h>
#include <android/log.h>
#include "ctr.h"


#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EDS (native code edsctr)", __VA_ARGS__);
#ifdef DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS (native code edsctr)", __VA_ARGS__);
#else
#define LOGD(...)
#endif

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_modes_CTR_initContext(JNIEnv *env, jobject obj)
{
    ctr_context *ctx = malloc(sizeof(ctr_context));
    if(ctx == NULL)
        return 0;
    memset(ctx,0,sizeof(ctr_context));
    return (jlong)ctx;
}

#include <mode_hdr.h>

#define CTR_BLOCK_SIZE 16

static void ctr_inc(unsigned char *counter)
{
    u32 n = CTR_BLOCK_SIZE, c = 1;

    do {
        --n;
        c += counter[n];
        counter[n] = (u8)c;
        c >>= 8;
    } while (n);
}

static void ctr_inc_aligned(unsigned char *counter)
{
    size_t *data, c, d, n;
    const union {
        long one;
        char little;
    } is_endian = {
            1
    };

    if (is_endian.little || ((size_t)counter % sizeof(size_t)) != 0) {
        ctr_inc(counter);
        return;
    }

    data = (size_t *)counter;
    c = 1;
    n = CTR_BLOCK_SIZE / sizeof(size_t);
    do {
        --n;
        d = data[n] += c;
        /* did addition carry? */
        c = ((d - c) ^ d) >> (sizeof(size_t) * 8 - 1);
    } while (n);
}

/*
 * The input encrypted as though 128bit counter mode is being used.  The
 * extra state information to record how much of the 128bit block we have
 * used is contained in *num, and the encrypted counter is kept in
 * ecount_buf.  Both *num and ecount_buf must be initialised with zeros
 * before the first call to ctr_proc_buffer(). This algorithm assumes
 * that the counter is in the x lower bits of the IV (ivec), and that the
 * application has full control over overflow and the rest of the IV.  This
 * implementation takes NO responsibility for checking that the counter
 * doesn't overflow into the rest of the IV when incremented.
 */
void ctr_proc_buffer(const unsigned char *in, unsigned char *out,
                           size_t len,
                           unsigned char ivec[CTR_BLOCK_SIZE],
                           unsigned char ecount_buf[CTR_BLOCK_SIZE], unsigned int *num,
                           block_func block, void *context)
{
    unsigned int n = *num;

    while (n && len)
    {
        *(out++) = *(in++) ^ ecount_buf[n];
        --len;
        n = (n + 1) % CTR_BLOCK_SIZE;
    }

    while (len >= CTR_BLOCK_SIZE)
    {
        (*block) (ivec, ecount_buf, context);
        ctr_inc_aligned(ivec);
        for (n = 0; n < CTR_BLOCK_SIZE; n += sizeof(size_t))
            *(size_t *)(out + n) =
                    *(size_t *)(in + n) ^ *(size_t *)(ecount_buf + n);
        len -= CTR_BLOCK_SIZE;
        out += CTR_BLOCK_SIZE;
        in += CTR_BLOCK_SIZE;
        n = 0;
    }
    if (len) {
        (*block) (ivec, ecount_buf, context);
        ctr_inc_aligned(ivec);
        while (len--) {
            out[n] = in[n] ^ ecount_buf[n];
            ++n;
        }
    }
    *num = n;
}

static void ctr_encrypt_buffer(const block_cipher_interface *cipher, uint8_t *buffer, size_t len, uint8_t ivec[CTR_BLOCK_SIZE], unsigned int *used)
{
    unsigned char ecount_buf[CTR_BLOCK_SIZE];
    memset(ecount_buf, 0, sizeof(ecount_buf));

    ctr_proc_buffer(buffer, buffer, len, ivec, ecount_buf, used, cipher->encrypt, cipher->context );
}

static void ctr_decrypt_buffer(const block_cipher_interface *cipher, uint8_t *buffer, size_t len, uint8_t ivec[CTR_BLOCK_SIZE], unsigned int *used)
{
    unsigned char ecount_buf[CTR_BLOCK_SIZE];
    memset(ecount_buf, 0, sizeof(ecount_buf));

    ctr_proc_buffer(buffer, buffer, len, ivec, ecount_buf, used, cipher->encrypt, cipher->context );
}

static void free_cipher_list(cipher_node *ciphers_head)
{
	if(ciphers_head!=NULL)
	{
		if(ciphers_head->next!=NULL)
			free_cipher_list((cipher_node *)ciphers_head->next);
		free(ciphers_head);
	}
}

static cipher_node *attach_ciphers(cipher_node *tail,block_cipher_interface *cipher)
{
	cipher_node *cp = (cipher_node *)malloc(sizeof(cipher_node));
	cp->next = NULL;
	cp->prev = tail;
	if(tail!=NULL)
		tail->next = cp;
	cp->cipher = cipher;
	return cp;
}

static void attach_ciphers_to_tail(ctr_context *ctx,block_cipher_interface *cipher)
{
	ctx->ciphers_tail = attach_ciphers(ctx->ciphers_tail,cipher);
	if(ctx->ciphers_head == NULL)
		ctx->ciphers_head = ctx->ciphers_tail;
}

void ctr_encrypt(ctr_context *context, uint8_t *data, int offset, int length, uint8_t *iv)
{
    uint8_t current_iv[CTR_BLOCK_SIZE];
    unsigned int used;
    cipher_node *cn = context->ciphers_head;  

    while(cn!=NULL)
    {
    	memcpy(current_iv, iv, CTR_BLOCK_SIZE);
        used = 0;
        ctr_encrypt_buffer(cn->cipher, data + offset, (size_t) length, current_iv, &used);
        cn = cn->next;
    }
    memcpy(iv, current_iv, CTR_BLOCK_SIZE);
}

void ctr_decrypt(ctr_context *context, uint8_t *data, int offset, int length, uint8_t *iv)
{
    uint8_t current_iv[CTR_BLOCK_SIZE];
    unsigned int used;
    cipher_node *cn = context->ciphers_head;  

    while(cn!=NULL)
    {
    	memcpy(current_iv, iv, CTR_BLOCK_SIZE);
        used = 0;
        ctr_decrypt_buffer(cn->cipher, data + offset, (size_t) length, current_iv, &used);
        cn = cn->next;
    }
    memcpy(iv, current_iv, CTR_BLOCK_SIZE);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
	return JNI_VERSION_1_2;
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_modes_CTR_attachNativeCipher(JNIEnv *env, jobject obj, jlong context, jlong cipherPtr)
{
	ctr_context *ctx = (ctr_context *)context;
	attach_ciphers_to_tail(ctx,(block_cipher_interface *)cipherPtr);
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_modes_CTR_encrypt(JNIEnv *env, jobject obj, jbyteArray data, jint offset, jint length, jbyteArray iv, jlong context)
{
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return -1;
	jbyte *raw_iv = (*env)->GetPrimitiveArrayCritical(env,iv,NULL);
	if(raw_iv == NULL)
	{
		(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
		return -1;
	}
    ctr_encrypt((ctr_context *)context,(uint8_t *)raw_data, offset, length, (uint8_t *)raw_iv);
    (*env)->ReleasePrimitiveArrayCritical(env,iv,raw_iv,0);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);	
	return 0;
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_modes_CTR_decrypt(JNIEnv *env, jobject obj, jbyteArray data, jint offset, jint length, jbyteArray iv, jlong context)
{	
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return -1;
	jbyte *raw_iv = (*env)->GetPrimitiveArrayCritical(env,iv,NULL);
	if(raw_iv == NULL)
	{
		(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
		return -1;
	}
    ctr_decrypt((ctr_context *)context,(uint8_t *)raw_data, offset, length, (uint8_t *)raw_iv);
    (*env)->ReleasePrimitiveArrayCritical(env,iv,raw_iv,0);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
	return 0;
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_modes_CTR_closeContext(JNIEnv *env, jobject obj, jlong context)
{
	if((void *)context!=NULL)
	{
		free_cipher_list(((ctr_context *)context)->ciphers_head);
		free((void *)context);
	}
}
