
#pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#pragma GCC diagnostic ignored "-Wpointer-to-int-cast"

#include "com_sovworks_eds_crypto_LocalEncryptedFileXTS.h"

#include <stdlib.h>
//#include <stdio.h>
#include <fcntl.h>
#include <android/log.h>
#include <block_cipher.h>
#include "../xts/xts.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EDS (native code localxts)", __VA_ARGS__);
#ifdef DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "EDS (native code localxts)", __VA_ARGS__);
#else
#define LOGD(...)
#endif

#define BUFFER_SIZE (8*1024)

typedef struct
{
    uint8_t buffer[BUFFER_SIZE];
    int is_buffer_empty, is_buffer_mod;
    off64_t current_position;    
    xts_context *xts;
    int fd;
} context_t;

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_LocalEncryptedFileXTS_initContext(
    JNIEnv *env, 
    jclass cls, 
    jstring pathString, 
    jboolean readOnly, 
    jlong xtsContextPointer)
{
    const char *path = (*env)->GetStringUTFChars(env, pathString, NULL);
    if (path == NULL)
        return 0;
    int fd = open(path,readOnly ? O_RDONLY : (O_RDWR | O_CREAT));
    if(fd<0)
    {
        (*env)->ReleaseStringUTFChars(env, pathString, path);
        return 0;
    }

    context_t *ctx = malloc(sizeof(context_t));
    if(ctx == NULL)
    {
        (*env)->ReleaseStringUTFChars(env, pathString, path);
        close(fd);
        return 0;
    }
    memset(ctx,0,sizeof(context_t));
    ctx->xts = (xts_context *)xtsContextPointer;
    ctx->xts->allow_skip = 0;
    ctx->fd = fd;
    ctx->is_buffer_empty = 1;
    return (jlong)ctx;
}

static int get_position_in_buffer(context_t *ctx)
{
    return ctx->current_position % BUFFER_SIZE;
}

static off64_t get_current_buffer_position(context_t *ctx)
{
    return ctx->current_position - (ctx->current_position % BUFFER_SIZE);
}

static int write_buffer(context_t *ctx)
{
    if(lseek64(ctx->fd,get_current_buffer_position(ctx), SEEK_SET)<0)
        return -1;
    int length = BUFFER_SIZE;
    int offset = 0;
    while (length > 0) 
    {           
        int bytes_written = write(ctx->fd,ctx->buffer + offset,length);
        if(bytes_written<0)
            return -1;        
        length -= bytes_written;
        offset += bytes_written;        
    }    
    return 0;
}

static int read_buffer(context_t *ctx)
{
    if(lseek64(ctx->fd,get_current_buffer_position(ctx), SEEK_SET)<0)
        return -1;
    int length = BUFFER_SIZE;
    int offset = 0;
    while (length > 0) 
    {           
        int bytes_read = read(ctx->fd,ctx->buffer + offset,length);
        if(bytes_read<0)
            return -1;        
        if(bytes_read == 0)
            break;
        length -= bytes_read;
        offset += bytes_read;        
    }
    if(length>0)
        memset(ctx->buffer + offset,0,length);  

    return offset;
}

static off64_t get_current_sector_index(context_t *ctx)
{
    return get_current_buffer_position(ctx) / XTS_SECTOR_SIZE;
}

static void decrypt_buffer(context_t *ctx)
{
    xts_decrypt(ctx->xts,ctx->buffer,0,BUFFER_SIZE,get_current_sector_index(ctx));
}

static void encrypt_buffer(context_t *ctx)
{
    xts_encrypt(ctx->xts,ctx->buffer,0,BUFFER_SIZE,get_current_sector_index(ctx));
}

static int fill_buffer(context_t *ctx)
{
    ctx->is_buffer_mod = 0;
    int bytes_read = read_buffer(ctx);
    if(bytes_read<0)
        return -1;
    decrypt_buffer(ctx);    
     ctx->is_buffer_empty = 0;
    return bytes_read;
}

static int flush_buffer(context_t *ctx)
{
    ctx->is_buffer_empty = 1;
    ctx->is_buffer_mod = 0;
    encrypt_buffer(ctx);
    return write_buffer(ctx)<0 ? -1 : 0;
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_LocalEncryptedFileXTS_write(
    JNIEnv *env, 
    jclass cls, 
    jlong context, 
    jbyteArray buf, 
    jint offset,
    jint length)
{    
    jbyte *data = (*env)->GetPrimitiveArrayCritical(env,buf,NULL);
    if(data == NULL)
        return -1;

    int res = 0;
    context_t *ctx = (context_t *)context;
    while (length > 0) 
    {        
		if(ctx->is_buffer_empty && fill_buffer(ctx)<0)
		{
			res = -1;
			break;
		}
        int buf_pos = get_position_in_buffer(ctx);
        int avail_space = BUFFER_SIZE - buf_pos;
        int bytes_written = avail_space < length ? avail_space : length;
        memcpy(ctx->buffer + buf_pos, data + offset, bytes_written);
        ctx->is_buffer_mod = 1; 
        if(avail_space == bytes_written && flush_buffer(ctx)<0)
        {
            res = -1;
            break;
        }
        length -= bytes_written;
        offset += bytes_written;
        ctx->current_position += bytes_written;
    }
    (*env)->ReleasePrimitiveArrayCritical(env,buf,data,0);
    return res;
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_LocalEncryptedFileXTS_read(
    JNIEnv *env, 
    jclass cls, 
    jlong context, 
    jbyteArray buf, 
    jint offset,
    jint length)
{    
    jbyte *data = (*env)->GetPrimitiveArrayCritical(env,buf,NULL);
    if(data == NULL)
        return -1;

    context_t *ctx = (context_t *)context;
    int bytes_to_read = length;
    while (bytes_to_read > 0)
    {
        if(ctx->is_buffer_empty && fill_buffer(ctx)<0)
        {
        	length = -1;
        	break;
        }

        int buf_pos = get_position_in_buffer(ctx);
        int avail_space = BUFFER_SIZE - buf_pos;
        int bytes_read = avail_space < bytes_to_read ? avail_space : bytes_to_read;
        memcpy(data + offset, ctx->buffer + buf_pos, bytes_read);
        if(avail_space == bytes_read)
        {
        	if(ctx->is_buffer_mod && flush_buffer(ctx)<0)
        	{
        		length = -1;
        		break;
        	}
        	else
        		ctx->is_buffer_empty = 1;
        }
        bytes_to_read -= bytes_read;
        offset += bytes_read;
        ctx->current_position += bytes_read;        
    }
    (*env)->ReleasePrimitiveArrayCritical(env,buf,data,0);
    return length;
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_LocalEncryptedFileXTS_seek(
    JNIEnv *env, 
    jclass cls, 
    jlong context, 
    jlong position)
{
    context_t *ctx = (context_t *)context;
    if(!ctx->is_buffer_empty)
    {
        int64_t d = position - get_current_buffer_position(ctx);
        if(d<0 || d>=BUFFER_SIZE)
        {
            if(ctx->is_buffer_mod)
                flush_buffer(ctx);
            ctx->is_buffer_empty = 1;
        }
    }
    ctx->current_position = position;
}

JNIEXPORT jint JNICALL Java_com_sovworks_eds_crypto_LocalEncryptedFileXTS_ftruncate(
    JNIEnv *env, 
    jclass cls, 
    jlong context, 
    jlong newLength)
{
    context_t *ctx = (context_t *)context;
    return ftruncate (ctx->fd, newLength);
}

JNIEXPORT jlong JNICALL Java_com_sovworks_eds_crypto_LocalEncryptedFileXTS_getPosition(
    JNIEnv *env, 
    jclass cls, 
    jlong context)
{
    return ((context_t *)context)->current_position;
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_LocalEncryptedFileXTS_flush(
    JNIEnv *env, 
    jclass cls, 
    jlong context)
{
    context_t *ctx = (context_t *)context;
    if(ctx->is_buffer_mod)
        flush_buffer(ctx);
    fsync(ctx->fd);
}

JNIEXPORT void JNICALL Java_com_sovworks_eds_crypto_LocalEncryptedFileXTS_close(
    JNIEnv *env, 
    jclass cls, 
    jlong context)
{
    context_t *ctx = (context_t *)context;
    if(ctx->is_buffer_mod)
        flush_buffer(ctx);
    close(ctx->fd);
    memset(ctx,0,sizeof(context_t));
    free(ctx);    
}