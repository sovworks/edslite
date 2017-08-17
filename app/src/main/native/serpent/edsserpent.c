
#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_crypto_blockciphers_Serpent.h"
#include <stdlib.h>
#include <malloc.h>
//#include <android/log.h>

#include <block_cipher.h>
#include "serpent.h"

#define SERPENT_SHEDULED_KEY_SIZE 140*4

//#ifdef __cplusplus
//extern "C" {
//#endif

//#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS native code", __VA_ARGS__)

int serpent_encrypt_block(const uint8_t *in, uint8_t *out, void *context)
{
	serpent_encrypt(in,out,(uint8_t *)context);
	return 0;
}

int serpent_decrypt_block(const uint8_t *in, uint8_t *out, void *context)
{
	serpent_decrypt(in,out,(uint8_t *)context);
	return 0;
}

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_blockciphers_Serpent_initContext(JNIEnv *env, jobject obj,jbyteArray key)
{
	block_cipher_interface *bci = malloc(sizeof(block_cipher_interface));
	if(bci == NULL)
		return 0;
	uint8_t *ctx = malloc(SERPENT_SHEDULED_KEY_SIZE);
	if(ctx == NULL)
		return 0;

	bci->encrypt = serpent_encrypt_block;
	bci->decrypt = serpent_decrypt_block;
	bci->context = ctx;

	jbyte *key_data = (*env)->GetByteArrayElements(env,key,NULL);
	if(key_data == NULL)
		return 0;
	serpent_set_key(key_data,32,ctx);
	(*env)->ReleaseByteArrayElements(env,key,key_data,JNI_ABORT);
	return (jlong)bci;
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_Serpent_closeContext(JNIEnv *env, jobject obj, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	if(bci!=NULL)
	{
		memset(bci->context,0,SERPENT_SHEDULED_KEY_SIZE);
		free(bci->context);
		free(bci);
	}
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_Serpent_encrypt(JNIEnv *env, jobject obj, jbyteArray data, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	jint len = (*env)->GetArrayLength(env,data);
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	serpent_encrypt((uint8_t *)raw_data,(uint8_t *)raw_data,(uint8_t *)bci->context);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_Serpent_decrypt(JNIEnv *env, jobject obj, jbyteArray data, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	jint len = (*env)->GetArrayLength(env,data);
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	serpent_decrypt((uint8_t *)raw_data,(uint8_t *)raw_data,(uint8_t *)bci->context);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
}

//#ifdef __cplusplus
//}
//#endif
