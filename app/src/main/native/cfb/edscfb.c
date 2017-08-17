

#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_crypto_modes_CFB.h"
#include <stdlib.h>
#include <android/log.h>
#include "cfb.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EDS (native code edscfb)", __VA_ARGS__);
#ifdef DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS (native code edscfb)", __VA_ARGS__);
#else
#define LOGD(...)
#endif

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_modes_CFB_initContext(JNIEnv *env, jobject obj)
{
    cfb_context *ctx = malloc(sizeof(cfb_context));
    if(ctx == NULL)
        return 0;
    memset(ctx,0,sizeof(cfb_context));
    return (jlong)ctx;
}

#include <mode_hdr.h>

#define CFB_BLOCK_SIZE 16

static void cfb_encrypt_buffer(const block_cipher_interface *cipher, uint8_t *buffer, size_t len, uint8_t ivec[CFB_BLOCK_SIZE], int *used)
{
	unsigned int n = *used;

    while (n && len) 
    {
        *buffer = ivec[n] ^= *buffer;
        buffer++;
        --len;
        n = (n + 1) % CFB_BLOCK_SIZE;
    }

    while (len >= CFB_BLOCK_SIZE) 
    {
    	cipher->encrypt(ivec, ivec, cipher->context);
        for (; n < CFB_BLOCK_SIZE; n += sizeof(size_t)) 
        {
            *(size_t *)(buffer + n) =
                *(size_t *)(ivec + n) ^= *(size_t *)(buffer + n);
        }
        len -= CFB_BLOCK_SIZE;
        buffer += CFB_BLOCK_SIZE;
        n = 0;
    }
    if (len) 
    {
        cipher->encrypt(ivec, ivec, cipher->context);
        while (len--) 
        {
            buffer[n] = ivec[n] ^= buffer[n];
            ++n;
        }
    }
    *used = n;    
}

static void cfb_decrypt_buffer(const block_cipher_interface *cipher, uint8_t *buffer, size_t len, uint8_t ivec[CFB_BLOCK_SIZE], int *used)
{
	unsigned int n = *used;

    while (n && len) 
    {
        unsigned char c;
        *buffer = ivec[n] ^ (c = *buffer);
        buffer++;
        ivec[n] = c;
        --len;
        n = (n + 1) % CFB_BLOCK_SIZE;
    }
    while (len >= CFB_BLOCK_SIZE) 
    {
        cipher->encrypt(ivec, ivec, cipher->context);
        for (; n < CFB_BLOCK_SIZE; n += sizeof(size_t)) 
        {
            size_t t = *(size_t *)(buffer + n);
            *(size_t *)(buffer + n) = *(size_t *)(ivec + n) ^ t;
            *(size_t *)(ivec + n) = t;
        }
        len -= CFB_BLOCK_SIZE;
        buffer += CFB_BLOCK_SIZE;
        n = 0;
    }
    if (len) 
    {
        cipher->encrypt(ivec, ivec, cipher->context);
        while (len--) 
        {
            unsigned char c;
            buffer[n] = ivec[n] ^ (c = buffer[n]);
            ivec[n] = c;
            ++n;
        }
    }
    *used = n;
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

static void attach_ciphers_to_tail(cfb_context *ctx,block_cipher_interface *cipher)
{
	ctx->ciphers_tail = attach_ciphers(ctx->ciphers_tail,cipher);
	if(ctx->ciphers_head == NULL)
		ctx->ciphers_head = ctx->ciphers_tail;
}

void cfb_encrypt(cfb_context *context, uint8_t *data, int offset, int length, uint8_t *iv)
{
    uint8_t current_iv[CFB_BLOCK_SIZE];
    int used;
    cipher_node *cn = context->ciphers_head;  

    while(cn!=NULL)
    {
    	memcpy(current_iv, iv, CFB_BLOCK_SIZE);
        used = 0;
        cfb_encrypt_buffer(cn->cipher, (uint8_t *)data + offset, length, current_iv, &used);               
        cn = cn->next;
    }
    memcpy(iv, current_iv, CFB_BLOCK_SIZE);
}

void cfb_decrypt(cfb_context *context, uint8_t *data, int offset, int length, uint8_t *iv)
{
    uint8_t current_iv[CFB_BLOCK_SIZE];
    int used;
    cipher_node *cn = context->ciphers_head;  

    while(cn!=NULL)
    {
    	memcpy(current_iv, iv, CFB_BLOCK_SIZE);
        used = 0;
        cfb_decrypt_buffer(cn->cipher, (uint8_t *)data + offset, length, current_iv, &used);               
        cn = cn->next;
    }
    memcpy(iv, current_iv, CFB_BLOCK_SIZE);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{

//#ifdef NOBFUSCATE/
//	update_glob_mod();
//#endif
	return JNI_VERSION_1_2;
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_modes_CFB_attachNativeCipher(JNIEnv *env, jobject obj, jlong context, jlong cipherPtr)
{
	cfb_context *ctx = (cfb_context *)context;
	attach_ciphers_to_tail(ctx,(block_cipher_interface *)cipherPtr);
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_modes_CFB_encrypt(JNIEnv *env, jobject obj, jbyteArray data, jint offset, jint length, jbyteArray iv, jlong context)
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
    cfb_encrypt((cfb_context *)context,(uint8_t *)raw_data, offset, length, (uint8_t *)raw_iv);
    (*env)->ReleasePrimitiveArrayCritical(env,iv,raw_iv,0);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);	
	return 0;
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_modes_CFB_decrypt(JNIEnv *env, jobject obj, jbyteArray data, jint offset, jint length, jbyteArray iv, jlong context)
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
    cfb_decrypt((cfb_context *)context,(uint8_t *)raw_data, offset, length, (uint8_t *)raw_iv);
    (*env)->ReleasePrimitiveArrayCritical(env,iv,raw_iv,0);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
	return 0;
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_modes_CFB_closeContext(JNIEnv *env, jobject obj, jlong context)
{
	if((void *)context!=NULL)
	{
		free_cipher_list(((cfb_context *)context)->ciphers_head);
		free((void *)context);
	}
}
