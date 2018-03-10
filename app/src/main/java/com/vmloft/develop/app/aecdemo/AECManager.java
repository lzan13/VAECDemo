package com.vmloft.develop.app.aecdemo;

/**
 * Created by lzan13 on 2018/3/6.
 */

public class AECManager {

    // 私有实例对象，
    private static AECManager instance;

    private AECManager() {}

    public static AECManager getInstance() {
        if (instance == null) {
            instance = new AECManager();
        }
        return instance;
    }

    /**
     * 开启回声消除
     *
     * @param sampleRate 音频采样率，支持: 8000/16000/32000/48000
     * @param delay 延迟采样数
     * @param doNLP 是否执行非线性处理，0 不执行， 1 执行
     */
    public void openAEC(int sampleRate, int delay, int doNLP) {
        nativeOpenAEC(sampleRate, delay, doNLP);
    }

    native void nativeOpenAEC(int sampleRate, int delay, int doNLP);

    /**
     * 关闭回升消除
     */
    public void closeAEC() {
        nativeCloseAEC();
    }

    native void nativeCloseAEC();

    /**
     * 处理远端数据
     */
    public void farProcess(short[] farBuffer) {
        nativeFarProcess(farBuffer);
    }

    native void nativeFarProcess(short[] farBuffer);

    /**
     * 处理近端数据
     */
    public void nearProcess(short[] outBuffer, short[] nearBuffer) {
        nativeNearProcess(outBuffer, nearBuffer);
    }

    native void nativeNearProcess(short[] outBuffer, short[] nearBuffer);


    /**
     * --------------------- 声音降噪处理 --------------------
     */

    /**
     * 开启声音降噪处理
     *
     * @param sampleRate 音频采样率，支持: 8000/16000/32000/48000
     * @param channels 声道数：1表示单声道、2表示立体声
     * @param level 降噪等级取值范围:0.0 ~ 2.0、超出范围进行饱和处理；
     */
    public void openANS(int sampleRate, int channels, float level) {
        nativeOpenANS(sampleRate, channels, level);
    }

    native void nativeOpenANS(int sampleRate, int channels, float level);

    /**
     * 关闭声音降噪处理
     */
    public void closeANS() {
        nativeCloseANS();
    }

    native void nativeCloseANS();

    /**
     * 声音降噪处理
     *
     * @param inBuffer 需要处理的数据
     * @param outBuffer 处理后的数据
     */
    public void ansProcess(short[] inBuffer, short[] outBuffer) {
        nativeANSProcess(inBuffer, outBuffer);
    }

    native void nativeANSProcess(short[] inBuffer, short[] outBuffer);
}
