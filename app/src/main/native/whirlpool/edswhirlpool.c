#include "com_sovworks_eds_crypto_hash_Whirlpool.h"

#include <memory.h>
#include "whirlpool.h"


#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"


JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_hash_Whirlpool_initContext(JNIEnv *env, jobject obj)
{
	return (jlong)malloc(sizeof(WHIRLPOOL_CTX));
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_hash_Whirlpool_freeContext(JNIEnv *env, jobject obj, jlong contextPtr)
{
	if(contextPtr!=0)
		free((void *)contextPtr);
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_hash_Whirlpool_resetDigest(JNIEnv *env, jobject obj, jlong contextPtr)
{
	memset((WHIRLPOOL_CTX *)contextPtr,0,sizeof(WHIRLPOOL_CTX));
	WHIRLPOOL_init((WHIRLPOOL_CTX *)contextPtr);
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_hash_Whirlpool_updateDigestByte(JNIEnv *env, jobject obj, jlong contextPtr, jbyte val)
{
	WHIRLPOOL_add(&val, 8, (WHIRLPOOL_CTX *)contextPtr);
}


JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_hash_Whirlpool_updateDigest(JNIEnv *env, jobject obj, jlong contextPtr, jbyteArray data, jint offset, jint length)
{
	jbyte *raw_data = (*env)->GetPrimitiveArrayCritical(env,data,NULL);
	if(raw_data == NULL)
		return;
	WHIRLPOOL_add(raw_data + offset, length*8, (WHIRLPOOL_CTX *)contextPtr);
	(*env)->ReleasePrimitiveArrayCritical(env,data,raw_data,JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_hash_Whirlpool_finishDigest(JNIEnv *env, jobject obj, jlong contextPtr, jbyteArray res)
{
	unsigned char digest[DIGESTBYTES];
	WHIRLPOOL_finalize ((WHIRLPOOL_CTX *)contextPtr, digest);
	(*env)->SetByteArrayRegion(env,res,0,DIGESTBYTES,digest);
}
