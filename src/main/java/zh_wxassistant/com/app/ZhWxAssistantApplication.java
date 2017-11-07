package zh_wxassistant.com.app;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;



/**
 * Created by Fzj on 2017/10/26.
 */

public class ZhWxAssistantApplication extends Application {
    public static Context getContextObject() {
        return mContext;
    }

    public static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化科大讯飞SDK
        SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID +"=59ed63b2");
        mContext=getApplicationContext();
    }
}
