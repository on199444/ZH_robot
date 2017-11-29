package zh_wxassistant.com.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.resource.Resource;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.sunflower.FlowerCollector;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.model.TResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import zh_wxassistant.com.Iflytek.IflytekHelper;
import zh_wxassistant.com.R;
import zh_wxassistant.com.base.MyProvider;
import zh_wxassistant.com.communication.WxToAssistant;
import zh_wxassistant.com.mvp.presenter.Iflytek.SpeechRecognizerPresenter;
import zh_wxassistant.com.mvp.presenter.Iflytek.SpeechSynthesizerPresenter;
import zh_wxassistant.com.mvp.view.Iflytek.SpeechRecognizerListener;
import zh_wxassistant.com.mvp.view.Iflytek.SpeechSynthesizerListener;
import static zh_wxassistant.com.general.Constant.PREFER_NAME;

/**
 * Created by Fzj on 2017/10/26.
 */

public class AutoSendMsgActivity extends TakePhotoActivity implements View.OnClickListener, SpeechRecognizerListener, SpeechSynthesizerListener{
    //语音听写
    private static String TAG = AutoSendMsgActivity.class.getSimpleName();
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    private EditText mResultText;
    private ImageView mImg;
    private Toast mToast;
    //使用轻量级数据保存机制SharedPreferences记录用户操作状态
    private SharedPreferences mSharedPreferences;
    //转换器可见否
    private boolean mTranslateEnable = false;


    //语音合成
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "xiaoyan";
    //发音人条列表数组
    private String[] mCloudVoicersEntries;
    //
    private String[] mCloudVoicersValue;

    //封装科大讯飞语音听写合成功能mvp模式
    public SpeechRecognizerPresenter speechRecognizerPresenter;
    public SpeechSynthesizerPresenter speechSynthesizerPresenter;

    //Activity和AccessibilityService之间的数据传递
    private String stringValue = null;
    private static WxToAssistant wxToAssistant;
    private MyBroadcastReceiver myBroadcastReceiver;
    private Boolean isBroadcastMsg;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.e("demo","onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_autosendmsg);
        initViewAndEvent();
         /*动态方式注册广播接收者*/
        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("myBroadcastReceiver");
        this.registerReceiver(myBroadcastReceiver, filter);
        isBroadcastMsg=false;
        speechRecognizerPresenter = new SpeechRecognizerPresenter(this);
        speechSynthesizerPresenter = new SpeechSynthesizerPresenter(this);
        mSharedPreferences = getSharedPreferences(PREFER_NAME, Activity.MODE_PRIVATE);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        //语音听写的SpeechRecognizer
        mIat = new IflytekHelper(this).getVoiceWriteRecognizerIat();
        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        mIatDialog = new IflytekHelper(this).getVoiceWriteDialog();

//————————————————————————————————————————————————
        //语音合成
        mTts = new IflytekHelper(this).getSpeechSynthesisTts();
        // 云端发音人名称列表
        mCloudVoicersEntries = getResources().getStringArray(R.array.voicer_cloud_entries);
        mCloudVoicersValue = getResources().getStringArray(R.array.voicer_cloud_values);
    }

    public static void setWxToAssistantInerface(WxToAssistant inerface) {
        wxToAssistant = inerface;
    }

    private void initViewAndEvent() {
        //录音听写
        mResultText = findViewById(R.id.autosendmsg_Text);
        findViewById(R.id.autosendmsg_setting).setOnClickListener(this);
        findViewById(R.id.autosendmsg_begain).setOnClickListener(this);
        findViewById(R.id.autosendmsg_stop).setOnClickListener(this);
        findViewById(R.id.autosendmsg_cancle).setOnClickListener(this);
        mImg=findViewById(R.id.myImg);

        //语音合成
        findViewById(R.id.speechSynthesis_person_select).setOnClickListener(this);
        findViewById(R.id.speechSynthesis_begain).setOnClickListener(this);
        findViewById(R.id.speechSynthesis_stop).setOnClickListener(this);
        findViewById(R.id.speechSynthesis_cancle).setOnClickListener(this);
        findViewById(R.id.speechSynthesis_continue).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (null == mIat) {
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            show("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        switch (view.getId()) {
            case R.id.autosendmsg_setting:
                show("设置");
                break;
            case R.id.autosendmsg_begain:
                // 移动数据分析，收集开始听写事件
                FlowerCollector.onEvent(AutoSendMsgActivity.this, "iat_recognize");
                // 清空显示内容
                mResultText.setText(" ");
                //此处用MVP模式封装了显示Dialog的听写功能，不显示的Dialog的功能暂未封装。
                speechRecognizerPresenter.beginSpeechRecognizer(mIatDialog).show();

                //清空听写结果
                /*
                * 听写对象封装要求：
                * 1.此处录音因为有可选的视图动画录音和非视图录音，因此可以在设置中点选是否显示视图录音。
                * 2.将录音功能设计为对象调用方法，只要持有录音对象在随处皆调用录音方法，并且对象可以控制停止和取消。
                * 3.尽量封装，不将任何繁杂和复杂逻辑暴露在外，使用封装基类，接口等方式。
                * 4.能够与语音合成对象挈合。
                *
                *消息传递机制：
                * 1、EventBus
                * 2、构造方法
                * 3、set get方法
                * 4、接口回调
                * 5、广播
                * 6、服务
                * */

                break;
            case R.id.autosendmsg_stop:
                show("停止听写");
                mIat.stopListening();
                break;
            case R.id.autosendmsg_cancle:
                show("取消听写");
                mIat.cancel();
                break;

            //语音合成部分————————————————————————————————————
            case R.id.speechSynthesis_person_select:
                //发音人选择，暂不改进
                showPresonSelectDialog();
                break;
            case R.id.speechSynthesis_begain:
                /***
                 * 1.此处语音合成，因此可以在设置中选择联系人。
                 * 2.将语音合成功能设计为对象调用方法，只要持有合成对象在随处皆调用语音合成方法，并且对象可以控制暂停、继续和取消功能。
                 * 3.尽量封装，不将任何繁杂和复杂逻辑暴露在外，使用封装基类，接口等方式。
                 * 4.能够与听写对象使用挈合流程不冲突。
                 */
                // speechSynthesis();
                int code = speechSynthesizerPresenter.begainSpeechSynthesizer(mTts, mResultText.getText().toString());
                if (code != ErrorCode.SUCCESS) {
                    show("语音合成失败,错误码: " + code);
                }
                break;
            case R.id.speechSynthesis_cancle:
                mTts.stopSpeaking();
                break;
            case R.id.speechSynthesis_stop:
                mTts.pauseSpeaking();
                break;
            case R.id.speechSynthesis_continue:
                mTts.resumeSpeaking();
                break;
        }
    }

    //语音合成发音人选择
    private int selectedNum = 0;

    private void showPresonSelectDialog() {
        new AlertDialog.Builder(this).setTitle("在线合成发音人选项")
                .setSingleChoiceItems(mCloudVoicersEntries, selectedNum, new DialogInterface.OnClickListener() { // 点击单选框后的处理
                    public void onClick(DialogInterface dialog, int which) { // 点击了哪一项
                        voicer = mCloudVoicersValue[which];
                        mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
                        if ("catherine".equals(voicer) || "henry".equals(voicer) || "vimary".equals(voicer)) {
                            ((EditText) findViewById(R.id.autosendmsg_Text)).setText(R.string.text_tts_source_en);
                        } else {
                            ((EditText) findViewById(R.id.autosendmsg_Text)).setText(R.string.text_tts_source);
                        }
                        selectedNum = which;
                        dialog.dismiss();
                    }
                }).show();
    }

    private void show(String str) {
        mToast.setText(str);
        mToast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

    @Override
    protected void onResume() {
        Log.e("demo","onResume");
        //移动数据统计分析
        FlowerCollector.onResume(this);
        FlowerCollector.onPageStart(TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.e("demo","onPause");
        //移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(this);
//        onDestroy();
        super.onPause();
    }

    //语音听写完毕
    @Override
    public void onSpeechRecognizerResult(String resultInfo) {
        mIatDialog.dismiss();
        mResultText.setText(resultInfo);
        mResultText.setSelection(resultInfo.length());
        /*
        * 调用语音合成框架读取内容，判断用户指令，提取关键字执行相应的操作
        * 此处
        */

        if (resultInfo.contains("发送微信")) {
            speechSynthesizerPresenter.begainSpeechSynthesizer(mTts, "好的，请说出您想说的话！");
        } else if (resultInfo.contains("你好")) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            ComponentName cmp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(cmp);
            startActivity(intent);
            //回调到AccessibilityService
            wxToAssistant.appointMsgToContacts(resultInfo);
        }else if (resultInfo.contains("点赞")){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            ComponentName cmp = new ComponentName("com.tencent.mm", "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI");
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(cmp);
            startActivity(intent);
        }else if(resultInfo.contains("发朋友圈")){
            TakePhoto takePhoto= getTakePhoto();
            takePhoto.onPickMultiple(5);
        }else if (hasTask){
            shareImage(mResultText.getText().toString(),mTResult);
        }
    }

    //TakePhoto框架
    @Override
    public void takeCancel() {
        super.takeCancel();
    }

    @Override
    public void takeFail(TResult result, String msg) {
        super.takeFail(result, msg);
    }

    private Boolean hasTask=false;
    private TResult mTResult=null;
    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);
        mTResult=result;
        hasTask= result.getImages().size()>0?true:false;
        if (mTResult!=null&&hasTask){
            speechSynthesizerPresenter.begainSpeechSynthesizer(mTts, "好的，请说出您想说的话！");
        }
    }

    public void click(View view) throws Exception{
        Intent intent = new Intent(Intent.ACTION_MAIN);
        ComponentName cmp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(cmp);
        startActivity(intent);
        //回调到AccessibilityService
        wxToAssistant.appointMsgToContacts(mResultText.getText().toString());
    }

    private void shareImage(String kdescription, TResult result) {
        ArrayList<Uri> imageUris = new ArrayList<>();
        File file = null;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI");
        intent.setComponent(comp);
        Log.w("SDIMG","现在的版本"+Build.VERSION.SDK_INT);
        //此处为适配7.0做准备,目前不能使用，使用FileProvider获取Uri虽然有效但是并不能用Intent直接传递到微信
        if (false){
            // 适配android7.0 ，不能直接访问原路径,需要对intent 授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType( MyProvider.getUriForFile(this,"fileprovider",new File(result.getImages().get(0).getOriginalPath())),"image/*");
            for (int i = 0; i < result.getImages().size(); i++) {
                file = new File(result.getImages().get(i).getOriginalPath());
                imageUris.add(MyProvider.getUriForFile(this,"ZH_fileprovider",file));
            }
        }else{
          intent.setDataAndType(Uri.fromFile(new File(result.getImages().get(0).getOriginalPath())),"image/*");
            for (int i = 0; i < result.getImages().size(); i++) {
                file = new File(result.getImages().get(i).getOriginalPath());
                imageUris.add(Uri.fromFile(file));
            }
        }
        intent.putExtra(Intent.EXTRA_STREAM, imageUris);
        intent.putExtra("Kdescription", kdescription);
        hasTask=false;
        mTResult=null;
        this.startActivity(intent);
    }

    @Override
    public void onSpeechRecognizerError(SpeechError error) {

    }

    //语音合成进度
    @Override
    public void onSpeechSynthesizerProgress(int onBufferProgress, int onSpeakProgress) {
        show(String.format(getString(R.string.tts_toast_format), onBufferProgress, onSpeakProgress));
    }

    //语音合成播发完毕
    @Override
    public void onSpeechSynthesizerCompleted(SpeechError error) {
        if (!isBroadcastMsg){
            speechRecognizerPresenter.beginSpeechRecognizer(mIatDialog).show();
        }else{
            isBroadcastMsg=false;
        }
        if (error == null) {
            show("播放完成");
        } else if (error != null) {
            show(error.getPlainDescription(true));
        }
    }

    public  class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            isBroadcastMsg=true;
            intent.getStringExtra("msg");
            mResultText.setText(intent.getStringExtra("msg"));
            speechSynthesizerPresenter.begainSpeechSynthesizer(mTts, mResultText.getText().toString());
        }
    }
}
