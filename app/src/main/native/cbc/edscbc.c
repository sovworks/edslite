

#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_crypto_modes_CBC.h"
#include <stdlib.h>
#include <android/log.h>
#include "cbc.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EDS (native code edscbc)", __VA_ARGS__);
#ifdef DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS (native code edscbc)", __VA_ARGS__);
#else
#define LOGD(...)
#endif

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_modes_CBC_initContext(JNIEnv *env, jobject obj)
{
    cbc_context *ctx = malloc(sizeof(cbc_context));
    if(ctx == NULL)
        return 0;
    memset(ctx,0,sizeof(cbc_context));
    jclass cls = (*env)->GetObjectClass(env, obj);
	jfieldID fid = (*env)->GetFieldID(env, cls, "_fileBlockSize", "I");
	if(fid == NULL)
		return 0;
	ctx->file_block_size = (*env)->GetIntField(env, obj, fid);

    return (jlong)ctx;
}

#include <mode_hdr.h>

#define CBC_BLOCK_SIZE 16

static void cbc_encrypt_block(const block_cipher_interface *cipher, uint8_t *buffer, size_t len, uint8_t ivec[CBC_BLOCK_SIZE])
{
	size_t n;
	const uint8_t *iv = ivec;

	while (len) 
	{
		for(n=0; n<CBC_BLOCK_SIZE && n<len; ++n)
			buffer[n] = buffer[n] ^ iv[n];
		for(; n<CBC_BLOCK_SIZE; ++n)
			buffer[n] = iv[n];
		cipher->encrypt(buffer, buffer, cipher->context);
		iv = buffer;
		if (len<=CBC_BLOCK_SIZE) break;
		len -= CBC_BLOCK_SIZE;
		buffer  += CBC_BLOCK_SIZE;		
	}
	memcpy(ivec,iv,CBC_BLOCK_SIZE);
}

static void cbc_decrypt_block(const block_cipher_interface *cipher, uint8_t *buffer, size_t len, uint8_t ivec[CBC_BLOCK_SIZE])
{
	uint8_t c;
	uint8_t tmp[CBC_BLOCK_SIZE];
	size_t n;

	while (len) 
	{		
		cipher->decrypt(buffer, tmp, cipher->context);
		for(n=0; n<CBC_BLOCK_SIZE && n<len; ++n) 
		{
			c = buffer[n];
			buffer[n] = tmp[n] ^ ivec[n];
			ivec[n] = c;
		}
		if (len<=CBC_BLOCK_SIZE) 
		{
			for (; n<CBC_BLOCK_SIZE; ++n)
				ivec[n] = buffer[n];
			break;
		}
		len -= CBC_BLOCK_SIZE;
		buffer  += CBC_BLOCK_SIZE;
	}
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

static void attach_ciphers_to_tail(cbc_context *ctx,block_cipher_interface *cipher)
{
	ctx->ciphers_tail = attach_ciphers(ctx->ciphers_tail,cipher);
	if(ctx->ciphers_head == NULL)
		ctx->ciphers_head = ctx->ciphers_tail;
}

static void increment_iv(uint8_t *iv)
{
	int i;
	for(i=0;i<CBC_BLOCK_SIZE;i++)
		if(++iv[i] & 0xFF)
			break;
}

static void memcpy_swap(void *to, const void *from, size_t size)
{
    int i;
    for(i=0;i<size;i++)
        ((unsigned char *)to)[size - i - 1] = ((unsigned char *)from)[i];
}

void cbc_encrypt(cbc_context *context, uint8_t *data, int offset, int length, uint8_t *iv, unsigned char incrementIV)
{
    uint8_t *cur, current_iv[CBC_BLOCK_SIZE], block_iv[CBC_BLOCK_SIZE];
    cipher_node *cn = context->ciphers_head;  
    size_t file_block_size = incrementIV ? context->file_block_size : length;
    int left, incr;

    while(cn!=NULL)
    {
    	memcpy(current_iv, iv, CBC_BLOCK_SIZE);
        for(cur = data + offset,left = length, incr = file_block_size; left > 0; cur += incr, left -= incr )
        {
            memcpy(block_iv, current_iv, CBC_BLOCK_SIZE);
        	if(left < file_block_size)
        		incr = left;        	
            cbc_encrypt_block(cn->cipher, cur, incr, block_iv);
			if(incrementIV)
				increment_iv(current_iv);
        }   
        cn = cn->next;
    }
	memcpy(iv, block_iv, CBC_BLOCK_SIZE);
}

void cbc_decrypt(cbc_context *context, uint8_t *data, int offset, int length, uint8_t *iv, unsigned char incrementIV)
{
    uint8_t *cur, current_iv[CBC_BLOCK_SIZE], block_iv[CBC_BLOCK_SIZE];
    cipher_node *cn = context->ciphers_tail;  
    size_t file_block_size = incrementIV ? context->file_block_size : length;
    int left, incr;

    while(cn!=NULL)
    {
        memcpy(current_iv, iv, CBC_BLOCK_SIZE);
        for(cur = data + offset,left = length, incr = file_block_size; left > 0; cur += incr, left -= incr )
        {
            memcpy(block_iv, current_iv, CBC_BLOCK_SIZE);
        	if(left < file_block_size)
        		incr = left;        	
            cbc_decrypt_block(cn->cipher, cur, incr, block_iv);
			if(incrementIV)
				increment_iv(current_iv);
        }       
        cn = cn->prev;
    }
	memcpy(iv, block_iv, CBC_BLOCK_SIZE);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{

//#ifdef NOBFUSCATE/
//	update_glob_mod();
//#endif
	return JNI_VERSION_1_2;
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_modes_CBC_attachNativeCipher(JNIEnv *env, jobject obj, jlong context, jlong cipherPtr)
{
	cbc_context *ctx = (cbc_context *)context;
	attach_ciphers_to_tail(ctx,(block_cipher_interface *)cipherPtr);
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_modes_CBC_encrypt(JNIEnv *env, jobject obj, jbyteArray data, jint offset, jint length, jbyteArray iv, jlong context, jboolean incrementIV)
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
    cbc_encrypt((cbc_context *)context,(uint8_t *)raw_data, offset, length, (uint8_t *)raw_iv, incrementIV);
    (*env)->ReleasePrimitiveArrayCritical(env,iv,raw_iv,0);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);	
	return 0;
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_modes_CBC_decrypt(JNIEnv *env, jobject obj, jbyteArray data, jint offset, jint length, jbyteArray iv, jlong context, jboolean incrementIV)
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
    cbc_decrypt((cbc_context *)context,(uint8_t *)raw_data, offset, length, (uint8_t *)raw_iv, incrementIV);
    (*env)->ReleasePrimitiveArrayCritical(env,iv,raw_iv,0);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
	return 0;
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_modes_CBC_closeContext(JNIEnv *env, jobject obj, jlong context)
{
	if((void *)context!=NULL)
	{
		free_cipher_list(((cbc_context *)context)->ciphers_head);
		free((void *)context);
	}
}