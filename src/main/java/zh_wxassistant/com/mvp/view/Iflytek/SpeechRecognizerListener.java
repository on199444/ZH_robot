package zh_wxassistant.com.mvp.view.Iflytek;

import com.iflytek.cloud.SpeechError;

/**
 * Created by Fzj on 2017/11/1.
 */

public interface SpeechRecognizerListener {
    public void onSpeechRecognizerResult(String resultInfo);
    public void onSpeechRecognizerError(SpeechError error);
}
