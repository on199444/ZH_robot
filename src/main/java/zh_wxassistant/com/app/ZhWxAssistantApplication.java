package zh_wxassistant.com.app;

import android.app.Application;
import android.os.Handler;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;



/**
 * Created by Fzj on 2017/10/26.
 */

public class ZhWxAssistantApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化科大讯飞SDK
        SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID +"=59ed63b2");
    }
}
