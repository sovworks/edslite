
#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_crypto_blockciphers_GOST.h"
#include <stdlib.h>
#include <malloc.h>
//#include <android/log.h>

#include <block_cipher.h>
#include "gost89.h"


//#ifdef __cplusplus
//extern "C" {
//#endif

//#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS native code", __VA_ARGS__)


int GOST_encrypt_block(const uint8_t *in, uint8_t *out, void *context)
{
	gostcrypt((gost_ctx *)context,(const byte *)in, (byte *)out);
	return 0;
}

int GOST_decrypt_block(const uint8_t *in, uint8_t *out, void *context)
{
	gostdecrypt((gost_ctx *)context,(const byte *)in, (byte *)out);
	return 0;
}

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_blockciphers_GOST_initContext(JNIEnv *env, jobject obj,jbyteArray key)
{
	block_cipher_interface *bci = malloc(sizeof(block_cipher_interface));
	if(bci == NULL)
		return 0;
	gost_ctx *ctx = malloc(sizeof(gost_ctx));
	if(ctx == NULL)
		return 0;
	jclass cls = (*env)->GetObjectClass(env, obj);
	jfieldID fid = (*env)->GetFieldID(env, cls, "_useTestSubstMask", "Z");
	if(fid == NULL)
		return 0;
	jboolean useTSM = (*env)->GetBooleanField(env, obj, fid);
	//gost_init(ctx, &GostR3411_94_TestParamSet);
	gost_init(ctx, useTSM ? &GostR3411_94_TestParamSet : NULL);

	bci->encrypt = GOST_encrypt_block;
	bci->decrypt = GOST_decrypt_block;
	bci->context = ctx;

	jbyte *key_data = (*env)->GetByteArrayElements(env,key,NULL);
	if(key_data == NULL)
		return 0;
	gost_key(ctx, (const byte *)key_data);
	(*env)->ReleaseByteArrayElements(env,key,key_data,JNI_ABORT);
	return (jlong)bci;
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_GOST_closeContext(JNIEnv *env, jobject obj, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	if(bci!=NULL)
	{
		gost_destroy((gost_ctx *)bci->context);
		memset(bci->context,0,sizeof(gost_ctx));
		free(bci->context);
		free(bci);
	}
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_GOST_encrypt(JNIEnv *env, jobject obj, jbyteArray data, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	jint len = (*env)->GetArrayLength(env,data);
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	GOST_encrypt_block((uint8_t *)raw_data,(uint8_t *)raw_data,bci->context);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_GOST_decrypt(JNIEnv *env, jobject obj, jbyteArray data, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	jint len = (*env)->GetArrayLength(env,data);
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	GOST_decrypt_block((uint8_t *)raw_data,(uint8_t *)raw_data,bci->context);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
}

//#ifdef __cplusplus
//}
//#endif
