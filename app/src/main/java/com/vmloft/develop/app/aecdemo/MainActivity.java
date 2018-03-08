package com.vmloft.develop.app.aecdemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    // 加载  aec so 库
    static {
        System.loadLibrary("hyphenate_av_aec");
    }

    public final String TAG = this.getClass().getSimpleName();

    //保存文件路径
    public final String PATH = Environment.getExternalStorageDirectory() + "/VAECDemo/";
    public final String MIC_FILE = PATH + "TestMic.pcm";
    public final String AEC_FILE = PATH + "TestAec.pcm";

    private int sampleRate = 16000;
    private final int frameSize = sampleRate/100; // 10 milliseconds duration
    private int bufferSizeInBytes = frameSize * 2;

    private int delay = 150;
    private int doNLP = 1;

    private boolean isPlaying = false;
    private boolean isAEC = false;
    private AudioRecord aRecord;

    byte[] farByteBuffer = new byte[bufferSizeInBytes];
    short[] farBuffer = new short[bufferSizeInBytes / 2];
    byte[] nearByteBuffer = new byte[bufferSizeInBytes];
    short[] nearBuffer = new short[bufferSizeInBytes / 2];
    short[] outBuffer = new short[bufferSizeInBytes / 2];

    private Button startAECBtn, stopAECBtn, playAECBtn, playNearBtn, stopPlayBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initAudioRecord();
    }

    private void init() {
        startAECBtn = findViewById(R.id.btn_start_aec);
        stopAECBtn = findViewById(R.id.btn_stop_aec);
        playAECBtn = findViewById(R.id.btn_play_aec);
        playNearBtn = findViewById(R.id.btn_play_near);
        stopPlayBtn = findViewById(R.id.btn_stop_play);


        startAECBtn.setOnClickListener(viewListener);
        stopAECBtn.setOnClickListener(viewListener);
        playAECBtn.setOnClickListener(viewListener);
        playNearBtn.setOnClickListener(viewListener);
        stopPlayBtn.setOnClickListener(viewListener);
    }

    private void initAudioRecord() {
        if (aRecord != null) {
            aRecord.stop();
            aRecord.release();
            aRecord = null;
        }
        aRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
    }

    private View.OnClickListener viewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.btn_start_aec:
                startAEC();
                break;
            case R.id.btn_stop_aec:
                stopAEC();
                break;
            case R.id.btn_play_aec:
                playAEC();
                break;
            case R.id.btn_play_near:
                playMic();
                break;
            case R.id.btn_stop_play:
                stopPlay();
                break;
            }
        }
    };

    /**
     * 开始回声消除
     */
    private void startAEC() {
        if (isAEC) {
            return;
        }
        isAEC = true;
        startFarThread();
        startMicThread();
    }

    /**
     * 开启输入远端数据接口，模拟输入对方说话数据
     */
    private void startFarThread() {
        if (isPlaying) {
            return;
        }
        isPlaying = true;
        // 开启回声消除
        AECManager.getInstance().openAEC(sampleRate, frameSize, delay, doNLP);

        new Thread(new Runnable() {
            @Override
            public void run() {

                // 读取本地资源
                BufferedInputStream farStream = new BufferedInputStream(
                        getResources().openRawResource(R.raw.audio_long16));

                AudioTrack aTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeInBytes, AudioTrack.MODE_STREAM);
                aTrack.play();
                int len = -1;
                try {
                    long frameDuration = 1000 * bufferSizeInBytes / 2 / sampleRate;
                    long n = 0;
                    long startTime = System.currentTimeMillis();
                    while (isAEC && (len = farStream.read(farByteBuffer)) > 0) {
                        ByteBuffer.wrap(farByteBuffer)
                                  .order(ByteOrder.LITTLE_ENDIAN)
                                  .asShortBuffer()
                                  .get(farBuffer);
                        AECManager.getInstance().farProcess(farBuffer);

                        // 最关键的是将解码后的数据，从缓冲区写入到AudioTrack对象中
                        aTrack.write(farByteBuffer, 0, len);
                        long elapse = System.currentTimeMillis() - startTime;
                        long elapseStd = n * frameDuration;
                        if (elapse < elapseStd) {
                            Log.d(TAG, "Thread sleep " + (elapseStd - elapse));
                            Thread.sleep(elapseStd - elapse);
                        }
                        ++n;
                    }

                    // 最后别忘了关闭并释放资源
                    aTrack.stop();
                    aTrack.release();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 开启采集麦克风数据并处理线程，
     */
    private void startMicThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "startMicThread");
                // 开始录制，采集麦克风数据
                aRecord.startRecording();
                try {
                    File pathDir = new File(PATH);
                    if (!pathDir.exists()) {
                        pathDir.mkdir();
                    }
                    File micFile = new File(MIC_FILE);
                    micFile.createNewFile();
                    FileOutputStream micStream = new FileOutputStream(micFile);

                    File aecFile = new File(AEC_FILE);
                    aecFile.createNewFile();
                    FileOutputStream aecStream = new FileOutputStream(aecFile);
                    int offsetInBytes = 0;
                    while (isAEC) {
                        long oldTime = System.currentTimeMillis();
                        int bytesRead = aRecord.read(nearByteBuffer, offsetInBytes, bufferSizeInBytes-offsetInBytes);
                        offsetInBytes += bytesRead;
                        if(offsetInBytes < bufferSizeInBytes ){
                            continue;
                        }
                        offsetInBytes = 0;
                        ByteBuffer.wrap(nearByteBuffer)
                                  .order(ByteOrder.LITTLE_ENDIAN)
                                  .asShortBuffer()
                                  .get(nearBuffer);

                        AECManager.getInstance().nearProcess(outBuffer, nearBuffer);
                        Log.d(TAG, "AEC cost time: " + (System.currentTimeMillis() - oldTime));
                        byte[] aecData = shortArrayToByteArry(outBuffer);
                        micStream.write(nearByteBuffer);
                        aecStream.write(aecData);
                    }
                    micStream.close();
                    aecStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 停止回声消除
     */
    private void stopAEC() {
        Log.d(TAG, "stopAEC");
        if (isPlaying) {
            isPlaying = false;
        }
        if (isAEC) {
            isAEC = false;
        }
        if (aRecord != null) {
            aRecord.stop();
            aRecord.release();
            aRecord = null;
        }
        AECManager.getInstance().closeAEC();
    }

    /**
     * 播放过滤后的本地麦克风音频
     */
    private void playAEC() {
        Log.d(TAG, "playAEC");
        try {
            BufferedInputStream stream = new BufferedInputStream(
                    new FileInputStream(new File(AEC_FILE)));
            playData(stream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放采集到的音频
     */
    private void playMic() {
        Log.d(TAG, "playMic");
        try {
            BufferedInputStream stream = new BufferedInputStream(
                    new FileInputStream(new File(MIC_FILE)));
            playData(stream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void playData(final BufferedInputStream stream) {
        if (isPlaying) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                isPlaying = true;
                try {
                    AudioTrack aTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                            sampleRate, AudioTrack.MODE_STREAM);
                    byte[] buffer = new byte[4096];
                    int len = -1;
                    aTrack.play();
                    while (isPlaying && (len = stream.read(buffer)) != -1) {
                        // 最关键的是将解码后的数据，从缓冲区写入到AudioTrack对象中
                        aTrack.write(buffer, 0, len);
                    }
                    // 最后别忘了关闭并释放资源
                    aTrack.stop();
                    aTrack.release();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    isPlaying = false;
                }
            }
        }).start();
    }

    private void stopPlay() {
        isPlaying = false;
    }

    /**
     * Short 转 Byte
     *
     * @param data
     * @return
     */
    public static byte[] shortArrayToByteArry(short[] data) {
        byte[] byteVal = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            byteVal[i * 2] = (byte) (data[i] & 0xff);
            byteVal[i * 2 + 1] = (byte) ((data[i] & 0xff00) >> 8);
        }
        return byteVal;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isPlaying) {
            isPlaying = false;
        }
        stopAEC();
    }


}
