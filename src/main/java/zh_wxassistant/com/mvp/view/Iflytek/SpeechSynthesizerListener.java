package zh_wxassistant.com.mvp.view.Iflytek;

import com.iflytek.cloud.SpeechError;

/**
 * Created by Fzj on 2017/11/2.
 */

public interface SpeechSynthesizerListener {
    public void onSpeechSynthesizerProgress(int onBufferProgress,int onSpeakProgress);
    public void onSpeechSynthesizerCompleted(SpeechError error);
}
