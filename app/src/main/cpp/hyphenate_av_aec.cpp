#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "SabineAec.h"

#define LOG_TAG    "VAEC_JNI"
#undef LOG
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG_TAG,__VA_ARGS__)

extern "C" {

static SabineAec *aecInstance = NULL;
static OsInt16 *refBuffer = NULL;
static OsInt16 *micBuffer = NULL;
static OsInt16 bufferSize = 80;

pthread_mutex_t mutex;

/**
 * 打开回声消除，并分配所需内存
 * @param env
 * @param instance
 * @param sampleRate 音频采样率，支持: 8000/16000/32000/48000
 * @param delay 延迟采样数
 * @param doNLP 是否执行非线性处理，0 不执行， 1 执行
 */
JNIEXPORT void JNICALL
Java_com_vmloft_develop_app_aecdemo_AECManager_nativeOpenAEC(
        JNIEnv *env, jobject instance,
        jint sampleRate,
        jint frameSize,
        jint delay,
        jint doNLP) {
    // 初始化线程锁对象，对应 pthread_mutex_destroy
    pthread_mutex_init(&mutex, NULL);

    bufferSize = frameSize;
    aecInstance = SabineAecOpen(sampleRate, delay, doNLP);
    refBuffer = (OsInt16 *) malloc(sizeof(OsInt16) * bufferSize);
    micBuffer = (OsInt16 *) malloc(sizeof(OsInt16) * bufferSize);
    memset(refBuffer, 0, bufferSize * sizeof(OsInt16));
    memset(micBuffer, 0, bufferSize * sizeof(OsInt16));


}
/**
 * 关闭回声消除
 * @param env
 * @param instance
 */
JNIEXPORT void JNICALL
Java_com_vmloft_develop_app_aecdemo_AECManager_nativeCloseAEC(JNIEnv *env, jobject instance) {
    if (aecInstance) {
        SabineAecClose(aecInstance);
        aecInstance = NULL;
    }
    // 释放分配的内存空间
    free(refBuffer);
    free(micBuffer);

    // 销毁线程锁对象，对应 pthread_mutext_init
    pthread_mutex_destroy(&mutex);
}

/**
 * 处理回声消除
 * @param env
 * @param instance
 * @param buffer 接收消除后的数据
 * @param micRes 麦克风数据
 * @param accRes 元数据
 */
JNIEXPORT void JNICALL
Java_com_vmloft_develop_app_aecdemo_AECManager_nativeBufferProcess(
        JNIEnv *env, jobject instance, jshortArray buffer, jshortArray micRes, jshortArray accRes) {
    env->GetShortArrayRegion(micRes, 0, bufferSize, micBuffer);
    env->GetShortArrayRegion(accRes, 0, bufferSize, refBuffer);

    SabineAecFarProcess(aecInstance, refBuffer, bufferSize);
    SabineAecNearProcess(aecInstance, micBuffer, bufferSize);

    env->SetShortArrayRegion(buffer, 0, bufferSize, micBuffer);
}

/**
 * 处理远端数据，会后边消除本地音频回声做准备
 * @param env
 * @param instance
 * @param farBuffer
 */
JNIEXPORT void JNICALL
Java_com_vmloft_develop_app_aecdemo_AECManager_nativeFarProcess(
        JNIEnv *env, jobject instance, jshortArray farBuffer) {
    env->GetShortArrayRegion(farBuffer, 0, bufferSize, refBuffer);
    pthread_mutex_lock(&mutex);
    SabineAecFarProcess(aecInstance, refBuffer, bufferSize);
    LOGD("Far process ref[0]=%d,ref[1]=%d,ref[2]=%d",refBuffer[0],refBuffer[1],refBuffer[2]);
    pthread_mutex_unlock(&mutex);
}
/**
 * 处理本地音频数据，并返回处理后的数据
 * @param env
 * @param instance
 * @param outBuffer
 * @param nearBuffer
 */
JNIEXPORT void JNICALL
Java_com_vmloft_develop_app_aecdemo_AECManager_nativeNearProcess(
        JNIEnv *env, jobject instance, jshortArray outBuffer, jshortArray nearBuffer) {
    env->GetShortArrayRegion(nearBuffer, 0, bufferSize, micBuffer);
    pthread_mutex_lock(&mutex);
    SabineAecNearProcess(aecInstance, micBuffer, bufferSize);
    LOGD("Far process mic[0]=%d,mic[1]=%d,mic[2]=%d",micBuffer[0],micBuffer[1],micBuffer[2]);
    pthread_mutex_unlock(&mutex);
    env->SetShortArrayRegion(outBuffer, 0, bufferSize, micBuffer);
}
}