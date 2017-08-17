
#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_crypto_blockciphers_AES.h"
#include <stdlib.h>
#include <malloc.h>
//#include <android/log.h>

#include <block_cipher.h>
#include "aes.h"

//#ifdef __cplusplus
//extern "C" {
//#endif

//#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS native code", __VA_ARGS__)

typedef struct
{
	aes_encrypt_ctx encrypt_context;
	aes_decrypt_ctx decrypt_context;
}
aes_context;

int aes_encrypt_block(const uint8_t *in, uint8_t *out, void *context)
{
	return aes_encrypt(in,out,&((aes_context *)context)->encrypt_context);
}

int aes_decrypt_block(const uint8_t *in, uint8_t *out, void *context)
{
	return aes_decrypt(in,out,&((aes_context *)context)->decrypt_context);
}

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_blockciphers_AES_initContext(JNIEnv *env, jobject obj,jbyteArray key)
{
	block_cipher_interface *bci = malloc(sizeof(block_cipher_interface));
	if(bci == NULL)
			return 0;
	aes_context *ctx = malloc(sizeof(aes_context));
	if(ctx == NULL)
		return 0;
	jint len = (*env)->GetArrayLength(env,key);

	bci->encrypt = aes_encrypt_block;
	bci->decrypt = aes_decrypt_block;
	bci->context = ctx;

	jbyte *key_data = (*env)->GetByteArrayElements(env,key,NULL);
	if(key_data == NULL)
		return 0;
	aes_encrypt_key(key_data, len, &ctx->encrypt_context);
	aes_decrypt_key(key_data, len, &ctx->decrypt_context);
	(*env)->ReleaseByteArrayElements(env,key,key_data,JNI_ABORT);
	return (jlong)bci;
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_AES_closeContext(JNIEnv *env, jobject obj, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	if(bci!=NULL)
	{
		memset(bci->context,0,sizeof(aes_context));
		free(bci->context);
		free(bci);
	}
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_AES_encrypt(JNIEnv *env, jobject obj, jbyteArray data, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	jint len = (*env)->GetArrayLength(env,data);
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	aes_encrypt((unsigned char *)raw_data,(unsigned char *)raw_data,&((aes_context *)bci->context)->encrypt_context);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_AES_decrypt(JNIEnv *env, jobject obj, jbyteArray data, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	jint len = (*env)->GetArrayLength(env,data);
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	aes_decrypt((unsigned char *)raw_data,(unsigned char *)raw_data,&((aes_context *)bci->context)->decrypt_context);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
}

//#ifdef __cplusplus
//}
//#endif