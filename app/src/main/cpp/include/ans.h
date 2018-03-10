#ifndef ANC_H
#define ANC_H

#include "Platform.h"

typedef struct ANS ANS;

#ifdef  __cplusplus
extern "C" {
#endif

/**
 * @brief       打开降噪算法
 * @inFs        采样率
 * @inChannels  声道数：1表示单声道、2表示立体声
 * @inLevel     降噪等级取值范围:0.0 ~ 2.0、超出范围进行饱和处理；
 * @return      返回降噪算法实例，算法固定使用20ms的帧长
 */
OS_EXTERN ANS* IcmDenoiseOpen(OsInt32 inFs,OsInt32 inChannels,OsFloat inLevel);
/**
 * @brief       关闭降噪算法
 * @inANS       降噪算法实例
 */
OS_EXTERN void IcmDenoiseClose(ANS *inANS);
/**
 * @brief       降噪处理
 * @inANS       降噪算法实例
 * @inPcm       输入数据
 * @ioOut       降噪处理后的数据
 */
OS_EXTERN void IcmDenoiseProcess(ANS *inANS,OsInt16 *inPcm,OsInt16 *ioOut);

#ifdef  __cplusplus
}
#endif

#endif
