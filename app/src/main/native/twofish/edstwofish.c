
#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_crypto_blockciphers_Twofish.h"
#include <stdlib.h>
#include <malloc.h>
//#include <android/log.h>

#include <block_cipher.h>
#include "twofish.h"


//#ifdef __cplusplus
//extern "C" {
//#endif

//#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS native code", __VA_ARGS__)


int twofish_encrypt_block(const uint8_t *in, uint8_t *out, void *context)
{
	twofish_encrypt((TwofishInstance *)context,(const u4byte *)in, (u4byte *)out);
	return 0;
}

int twofish_decrypt_block(const uint8_t *in, uint8_t *out, void *context)
{
	twofish_decrypt((TwofishInstance *)context,(const u4byte *)in,(u4byte *)out);
	return 0;
}

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_blockciphers_Twofish_initContext(JNIEnv *env, jobject obj,jbyteArray key)
{
	block_cipher_interface *bci = malloc(sizeof(block_cipher_interface));
	if(bci == NULL)
		return 0;
	TwofishInstance *ctx = malloc(sizeof(TwofishInstance));
	if(ctx == NULL)
		return 0;

	bci->encrypt = twofish_encrypt_block;
	bci->decrypt = twofish_decrypt_block;
	bci->context = ctx;

	jbyte *key_data = (*env)->GetByteArrayElements(env,key,NULL);
	if(key_data == NULL)
		return 0;
	
	twofish_set_key(ctx,(const u4byte *)key_data,256);
	
	(*env)->ReleaseByteArrayElements(env,key,key_data,JNI_ABORT);
	return (jlong)bci;
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_Twofish_closeContext(JNIEnv *env, jobject obj, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	if(bci!=NULL)
	{
		memset(bci->context,0,sizeof(TwofishInstance));
		free(bci->context);
		free(bci);
	}
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_Twofish_encrypt(JNIEnv *env, jobject obj, jbyteArray data, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	jint len = (*env)->GetArrayLength(env,data);
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	twofish_encrypt_block((uint8_t *)raw_data,(uint8_t *)raw_data,bci->context);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_blockciphers_Twofish_decrypt(JNIEnv *env, jobject obj, jbyteArray data, jlong context)
{
	block_cipher_interface *bci = ((block_cipher_interface *)context);
	jint len = (*env)->GetArrayLength(env,data);
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	twofish_decrypt_block((uint8_t *)raw_data,(uint8_t *)raw_data,bci->context);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,0);
}

//#ifdef __cplusplus
//}
//#endif
