
#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_fs_util_FDRandomAccessIO.h"

#include <stdlib.h>
//#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/stat.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EDS (native code fdraio)", __VA_ARGS__);
#ifdef DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS (native code fdraio)", __VA_ARGS__);
#else
#define LOGD(...)
#endif

#define BUFFER_SIZE (8*1024)

JNIEXPORT jint JNICALL Java_com_sovworks_eds_fs_util_FDRandomAccessIO_write(
    JNIEnv *env, 
    jclass cls, 
    jint fd, 
    jbyteArray buf, 
    jint offset,
    jint length)
{    
	int res = 0;
    jbyte *data = (*env)->GetPrimitiveArrayCritical(env,buf,NULL);
    if(data == NULL)
        return -1;
    while (length > 0) 
    {           
        int bytes_written = write(fd,data + offset,length);
        if(bytes_written<0)
        {
        	res = -1;
        	break;
        }
        length -= bytes_written;
        offset += bytes_written;        
    } 
    (*env)->ReleasePrimitiveArrayCritical(env,buf,data,0);
    return res;
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_fs_util_FDRandomAccessIO_read(
    JNIEnv *env, 
    jclass cls, 
    jint fd, 
    jbyteArray buf, 
    jint offset,
    jint length)
{    
    jbyte *data = (*env)->GetPrimitiveArrayCritical(env,buf,NULL);
    if(data == NULL)
        return -1;
    int res = read(fd,data + offset,length);    
    (*env)->ReleasePrimitiveArrayCritical(env,buf,data,0);
    return res;
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_fs_util_FDRandomAccessIO_seek(
    JNIEnv *env, 
    jclass cls, 
    jint fd, 
    jlong position)
{
    lseek64(fd, position, SEEK_SET);
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_fs_util_FDRandomAccessIO_ftruncate(
    JNIEnv *env, 
    jclass cls, 
    jint fd, 
    jlong newLength)
{
    return ftruncate64 (fd, newLength);
}

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_fs_util_FDRandomAccessIO_getSize(
	JNIEnv *env, 
	jclass cls, 
	jint fd)
{
	struct stat64 buf;
	if(fstat64 (fd, &buf))
		return -1;
	return buf.st_size;
}

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_fs_util_FDRandomAccessIO_getPosition(
    JNIEnv *env, 
    jclass cls, 
    jint fd)
{
    return lseek64(fd, 0, SEEK_CUR);
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_fs_util_FDRandomAccessIO_flush(
    JNIEnv *env, 
    jclass cls, 
    jint fd)
{
    fsync(fd);
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_fs_util_FDRandomAccessIO_close(
    JNIEnv *env, 
    jclass cls, 
    jint fd)
{
    close(fd);
}