#ifndef EDS_JNIUTIL_H
#define EDS_JNIUTIL_H

#include <jni.h>

#define CACHE_METHOD(VN, CN, SIGN) VN = (*env)->GetMethodID(env,CN,SIGN);\
if(VN == NULL) return JNI_ERR
#define CACHE_STATIC_METHOD(VN, CN, SIGN) VN = (*env)->GetStaticMethodID(env,CN,SIGN);\
if(VN == NULL) return JNI_ERR
#define CACHE_FIELD(VN, CN, SIGN) VN = (*env)->GetFieldID(env,CN,SIGN);\
if(VN == NULL) return JNI_ERR


JNIEnv *get_env();
void detach_thread(JNIEnv *env);

JNIEnv *init_jni(JavaVM *jvm);
void clear_jni();
int call_jni_object_func(JNIEnv *env,jobject instance,jmethodID method,jobject *result,...);
int call_jni_int_func(JNIEnv *env,jobject instance,jmethodID method,jint *result,...);
int call_jni_long_func(JNIEnv *env,jobject instance,jmethodID method,jlong *result,...);
int call_jni_void_func(JNIEnv *env,jobject instance,jmethodID method,...);
int call_jni_static_void_func(JNIEnv *env, jclass cls, jmethodID method,...);
int call_jni_static_int_func(JNIEnv *env,jclass cls,jmethodID method,jint *result,...);
#endif //EDS_JNIUTIL_H
