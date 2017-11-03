package zh_wxassistant.com.mvp.presenter.Iflytek;

import android.os.Bundle;

import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import zh_wxassistant.com.R;
import zh_wxassistant.com.mvp.view.Iflytek.SpeechSynthesizerListener;

/**
 * Created by Fzj on 2017/11/2.
 */

public class SpeechSynthesizerPresenter {
    private int bufferProgress=0;
    private int speakProgress=0;
    private int resultCode;
    private SpeechSynthesizerListener speechSynthesizerListener;

    public SpeechSynthesizerPresenter(SpeechSynthesizerListener speechSynthesizerListener) {
        this.speechSynthesizerListener = speechSynthesizerListener;
    }

    public int begainSpeechSynthesizer(SpeechSynthesizer mTts,String text){
        resultCode= mTts.startSpeaking(text, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {

            }

            @Override
            public void onBufferProgress(int percent, int b, int e, String info) {
                bufferProgress=percent;
                speechSynthesizerListener.onSpeechSynthesizerProgress(bufferProgress, speakProgress);
            }
            @Override
            public void onSpeakProgress(int percent, int b, int e) {
                speakProgress=percent;
                speechSynthesizerListener.onSpeechSynthesizerProgress(bufferProgress, speakProgress);
            }
            @Override
            public void onSpeakPaused() {

            }

            @Override
            public void onSpeakResumed() {

            }

            @Override
            public void onCompleted(SpeechError speechError) {
                speechSynthesizerListener.onSpeechSynthesizerCompleted(speechError);
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {
                // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
                // 若使用本地能力，会话id为null
                //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                //		Log.d(TAG, "session id =" + sid);
                //	}
            }
        });
        return resultCode;
    }
}
