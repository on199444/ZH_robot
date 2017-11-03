package zh_wxassistant.com.mvp.presenter.Iflytek;

import android.util.Log;

import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import java.util.HashMap;
import java.util.LinkedHashMap;

import zh_wxassistant.com.mvp.view.Iflytek.SpeechRecognizerListener;
import zh_wxassistant.com.util.JsonParser;

/**
 * Created by Fzj on 2017/11/1.
 */

public class SpeechRecognizerPresenter {
    private HashMap<String, String> mIatResultsMap = new LinkedHashMap<String, String>();
    private SpeechRecognizerListener speechRecognizerListener;
    private String info;
    public SpeechRecognizerPresenter(SpeechRecognizerListener speechRecognizerListener) {
        this.speechRecognizerListener = speechRecognizerListener;
    }

    public  RecognizerDialog beginSpeechRecognizer(RecognizerDialog mIatDialog){
        mIatDialog.setListener(new RecognizerDialogListener() {
            @Override
            public void onResult(RecognizerResult recognizerResult, boolean b) {
                Log.e("demo","听写成功！");
                mIatResultsMap.clear();
                //解析json
                speechRecognizerListener.onSpeechRecognizerResult(JsonParser.parseIatResult(recognizerResult, mIatResultsMap).toString());
            }

            @Override
            public void onError(SpeechError error) {
                Log.e("demo","听写失败！");
                speechRecognizerListener.onSpeechRecognizerError(error);
            }
        });
        return mIatDialog;
    }
}
