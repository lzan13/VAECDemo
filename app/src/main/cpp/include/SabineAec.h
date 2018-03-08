#ifndef SABINEAEC_H
#define SABINEAEC_H

#include "Platform.h"

typedef struct SabineAec_t SabineAec;

#ifdef  __cplusplus
extern "C" {
#endif

/**
 * @brief           打开回声消除器一个实例，注意这里帧长大小强制为10ms
 * @inSampleRate    采样率，可以支持的采样率有：8000、16000、32000、48000
 * @inDelay         延迟采样数
 * @inDoNLP         0表示不执行非线性处理，1表示执行非线性处理
 * @return          返回回声消除器实例
 */
SabineAec* SabineAecOpen(OsInt32 inSampleRate,OsInt32 inDelay,OsInt32 inDoNLP);

/**
 * @brief           关闭回声消除器实例
 * @inAec           回声消除器实例
 */
void SabineAecClose(SabineAec *inAec);

/**
 * @brief           远端数据处理。注意：算法不是线程安全的
 * @inAec           回声消除器实例
 * @inRef           远端语音数据
 * @inLen           远端语音采样数
 */
void SabineAecFarProcess(SabineAec *inAec,OsInt16 *inRef,OsInt32 inLen);

/**
 * @brief           近端数据处理。注意：算法不是线程安全的
 * @ioMic           近端信号，执行完毕后，去回声后的数据也在这里返回
 * @inLen           远端语音采样数
 */
void SabineAecNearProcess(SabineAec *inAec,OsInt16 *ioMic,OsInt32 inLen);

#ifdef  __cplusplus
}
#endif

#endif