package zh_wxassistant.com.Iflytek;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import zh_wxassistant.com.R;
import static zh_wxassistant.com.general.Constant.PREFER_NAME;

/**
 * Created by Fzj on 2017/10/27.
 */

public  class  IflytekHelper implements InitListener {
    public static final String TAG="Iflytek";
    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private SpeechRecognizer mIat;//语音听写对象
    private SpeechSynthesizer mTts;//语音合成对象
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    public IflytekHelper(Context mContext) {
        this.mContext = mContext;
        mSharedPreferences = mContext.getSharedPreferences(PREFER_NAME, Activity.MODE_PRIVATE);
    }

    //无录音视图的听写对象
    public  SpeechRecognizer getVoiceWriteRecognizerIat(){
        mIat= SpeechRecognizer.createRecognizer(mContext, IflytekHelper.this);
        setIatParam(mIat);
        return  mIat;
    };

    //有录音视图的听写对象
    public RecognizerDialog getVoiceWriteDialog(){
        return   new RecognizerDialog(mContext, IflytekHelper.this);
    };

    //初始化音频合成对象
    public SpeechSynthesizer getSpeechSynthesisTts(){
        mTts=SpeechSynthesizer.createSynthesizer(mContext, IflytekHelper.this);
        setTtsParam(mTts);
        return mTts;
    }

    private void setTtsParam(SpeechSynthesizer mTts) {
            // 清空参数
            mTts.setParameter(SpeechConstant.PARAMS, null);
            // 根据合成引擎设置相应参数
            if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
                mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
                // 设置在线合成发音人
                mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
                //设置合成语速
                mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
                //设置合成音调
                mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
                //设置合成音量
                mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
            } else {
                mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
                // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
                mTts.setParameter(SpeechConstant.VOICE_NAME, "");
                /**
                 * TODO 本地合成不设置语速、音调、音量，默认使用语记设置
                 * 开发者如需自定义参数，请参考在线合成参数设置
                 */
            }
            //设置播放器音频流类型
            mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "1"));
            // 设置播放合成音频打断音乐播放，默认为true
            mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

            // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
            // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
            mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
        }



    private void setIatParam(SpeechRecognizer mIat) {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

       Boolean mTranslateEnable = mSharedPreferences.getBoolean( mContext.getString(R.string.pref_key_translate), false );
        if( mTranslateEnable ){
            Log.i( TAG, "translate enable" );
            mIat.setParameter( SpeechConstant.ASR_SCH, "1" );
            mIat.setParameter( SpeechConstant.ADD_CAP, "translate" );
            mIat.setParameter( SpeechConstant.TRS_SRC, "its" );
        }
        //普通话
        String lag = mSharedPreferences.getString("iat_language_preference",
                "mandarin");
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
            mIat.setParameter(SpeechConstant.ACCENT, null);

            if( mTranslateEnable ){
                mIat.setParameter( SpeechConstant.ORI_LANG, "en" );
                mIat.setParameter( SpeechConstant.TRANS_LANG, "cn" );
            }
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);

            if( mTranslateEnable ){
                mIat.setParameter( SpeechConstant.ORI_LANG, "cn" );
                mIat.setParameter( SpeechConstant.TRANS_LANG, "en" );
            }
        }

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }
    @Override
    public void onInit(int code) {
       // Log.d(TAG, "SpeechRecognizer init() code = " + code);
        if (code != ErrorCode.SUCCESS) {
           // show("初始化失败，错误码：" + code);
        }
    }

}
