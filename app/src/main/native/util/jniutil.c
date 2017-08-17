
#include <stdio.h>

#include "jniutil.h"

#ifdef DEBUG
#include <android/log.h>
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS (native code fuseeds)", __VA_ARGS__);
#else
#define LOGD(...)
#endif

#define EDS_ERROR_CLASSNAME "com/sovworks/eds/exceptions/NativeError"

#define ERRNO_SIG "errno","I"

JavaVM *Jvm = NULL;
static jclass NativeError;
static jfieldID Errno;

JNIEnv *get_env()
{
    JNIEnv *env;
    if(Jvm == NULL)
        return NULL;
    int res = (*Jvm)->AttachCurrentThread(Jvm,&env,NULL);
    if(res < 0)
    {
        LOGD("AttachCurrentThread failed");
        return NULL;
    }
    return env;
}

void detach_thread(JNIEnv *env)
{
    if (env && (*env)->ExceptionOccurred(env))
    {
        (*env)->ExceptionDescribe(env);
    }
    (*Jvm)->DetachCurrentThread(Jvm);
}

static jint cache_methods(JNIEnv *env)
{
    CACHE_FIELD(Errno, NativeError, ERRNO_SIG);
    return JNI_OK;
}

static jint cache_classes_common(JNIEnv *env)
{
    jclass cls = (*env)->FindClass(env,EDS_ERROR_CLASSNAME);
    if (cls == NULL)
        return JNI_ERR;

    NativeError = (*env)->NewGlobalRef(env,cls);
    (*env)->DeleteLocalRef(env, cls);
    if (NativeError == NULL)
        return JNI_ERR;

    if(cache_methods(env) == JNI_ERR)
        return JNI_ERR;
    return JNI_OK;
}

static void clean_classes_cache_common(JNIEnv *env)
{
    (*env)->DeleteGlobalRef(env,NativeError);
}

static int check_exc(JNIEnv *env)
{
    jthrowable exc;
    int errcode = 0;
    exc = (*env)->ExceptionOccurred(env);
    if(exc!=NULL)
    {
        (*env)->ExceptionClear(env);
        if((*env)->IsInstanceOf(env,exc,NativeError))
            errcode = (*env)->GetIntField(env,exc,Errno);
        else
            errcode = -1;
    }
    return errcode;
}

int call_jni_object_func(JNIEnv *env,jobject instance,jmethodID method,jobject *result,...)
{
    va_list args;
    jobject res;
    va_start(args,result);
    res = (*env)->CallObjectMethodV(env,instance,method,args);
    va_end(args);
    if(result!=NULL)
        *result = res;
    return check_exc(env);
}

int call_jni_int_func(JNIEnv *env,jobject instance,jmethodID method,jint *result,...)
{
    va_list args;
    jint res;
    va_start(args,result);
    res = (*env)->CallIntMethodV(env,instance,method,args);
    va_end(args);
    if(result!=NULL)
        *result = res;
    return check_exc(env);
}

int call_jni_long_func(JNIEnv *env,jobject instance,jmethodID method,jlong *result,...)
{
    va_list args;
    jlong res;
    va_start(args,result);
    res = (*env)->CallLongMethodV(env,instance,method,args);
    va_end(args);
    if(result!=NULL)
        *result = res;
    return check_exc(env);
}

int call_jni_void_func(JNIEnv *env,jobject instance,jmethodID method,...)
{
    va_list args;
    va_start(args,method);
    (*env)->CallVoidMethodV(env,instance,method,args);
    va_end(args);
    return check_exc(env);
}


int call_jni_static_void_func(JNIEnv *env, jclass cls, jmethodID method,...)
{
    va_list args;
    va_start(args,method);
    (*env)->CallStaticVoidMethodV(env, cls,method,args);
    va_end(args);

    return check_exc(env);
}

int call_jni_static_int_func(JNIEnv *env,jclass cls,jmethodID method,jint *result,...)
{
    va_list args;
    jint res;
    va_start(args,result);
    res = (*env)->CallStaticIntMethodV(env,cls,method,args);
    va_end(args);
    if(result!=NULL)
        *result = res;
    return check_exc(env);
}

JNIEnv *init_jni(JavaVM *jvm)
{
    Jvm = jvm;
    JNIEnv *env = get_env();
    if(!env)
        return NULL;
    if(cache_classes_common(env) != JNI_OK)
    {
        //detach_thread();
        return NULL;
    }
    return env;
}

void clear_jni()
{
    JNIEnv *env = get_env();
    clean_classes_cache_common(env);
    Jvm = NULL;
}