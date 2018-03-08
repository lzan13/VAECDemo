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
    public void openAEC(int sampleRate, int frameSize, int delay, int doNLP) {
        nativeOpenAEC(sampleRate, frameSize, delay, doNLP);
    }

    native void nativeOpenAEC(int sampleRate, int frameSize, int delay, int doNLP);

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

    public void bufferProcess(short[] buffer, short[] micBuffer, short[] accBuffer) {
        nativeBufferProcess(buffer, micBuffer, accBuffer);
    }

    native void nativeBufferProcess(short[] buffer, short[] micBuffer, short[] accBuffer);

}
