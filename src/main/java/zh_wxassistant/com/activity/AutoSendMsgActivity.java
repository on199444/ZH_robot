package zh_wxassistant.com.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.sunflower.FlowerCollector;
import java.util.HashMap;
import java.util.LinkedHashMap;
import zh_wxassistant.com.Iflytek.IflytekHelper;
import zh_wxassistant.com.R;
import zh_wxassistant.com.service.assistantService;
import zh_wxassistant.com.util.JsonParser;
import static zh_wxassistant.com.general.Constant.PREFER_NAME;

/**
 * Created by Fzj on 2017/10/26.
 */

public class AutoSendMsgActivity extends Activity implements View.OnClickListener {
    //语音听写
    private static String TAG = AutoSendMsgActivity.class.getSimpleName();
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResultsMap = new LinkedHashMap<String, String>();
    private EditText mResultText;
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

    // 缓冲进度
    private int mPercentForBuffering = 0;
    // 播放进度
    private int mPercentForPlaying = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_autosendmsg);
        initViewAndEvent();
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

    private void initViewAndEvent() {
        //录音听写
        mResultText = findViewById(R.id.autosendmsg_Text);
        findViewById(R.id.autosendmsg_setting).setOnClickListener(this);
        findViewById(R.id.autosendmsg_begain).setOnClickListener(this);
        findViewById(R.id.autosendmsg_stop).setOnClickListener(this);
        findViewById(R.id.autosendmsg_cancle).setOnClickListener(this);

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
                mResultText.setText(null);
                //清空听写结果
                mIatResultsMap.clear();
                boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
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
                if (isShowDialog) {
                    // 显示听写对话框
                    mIatDialog.setListener(new RecognizerDialogListener() {
                        @Override
                        public void onResult(RecognizerResult recognizerResult, boolean b) {
                            //解析json
                            String info = JsonParser.parseIatResult(recognizerResult, mIatResultsMap);
                            mResultText.setText(info);
                            mResultText.setSelection(info.length());
                            //调用语音合成框架读取内容，判断用户指令，提取关键字执行相应的操作
                            if (info.contains("发送微信")) {
                                mResultText.setText("好的，请入录您想说的话！");
                                speechSynthesis();
                                // speechSynthesis();
                            } else if (info.contains("你好")) {
//                                mResultText.setText("我听不懂你说的话！");
//                                speechSynthesis();
                                assistantService.transInfo = info;
                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                ComponentName cmp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
                                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.setComponent(cmp);
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onError(SpeechError error) {
                            if (mTranslateEnable && error.getErrorCode() == 14002) {
                                show(error.getPlainDescription(true) + "\n请确认是否已开通翻译功能");
                            } else {
                                show(error.getPlainDescription(true));
                            }
                        }
                    });
                    mIatDialog.show();
                    show(getString(R.string.text_begin));
                } else {
                    // 不显示听写对话框
                    int ret = mIat.startListening(new RecognizerListener() {
                        @Override
                        public void onVolumeChanged(int i, byte[] bytes) {
                            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
                            // 若使用本地能力，会话id为null
                            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                            //		Log.d(TAG, "session id =" + sid);
                            //	}
                        }

                        @Override
                        public void onBeginOfSpeech() {
                            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
                            show("开始说话");
                        }


                        @Override
                        public void onEndOfSpeech() {
                            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
                            show("结束说话");
                        }

                        @Override
                        public void onResult(RecognizerResult results, boolean b) {
                            Log.d(TAG, results.getResultString());
                            String info = JsonParser.parseIatResult(results, mIatResultsMap);
                            mResultText.setText(info);
                            mResultText.setSelection(info.length());
                        }

                        @Override
                        public void onError(SpeechError error) {
                            // Tips：
                            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
                            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
                            if (mTranslateEnable && error.getErrorCode() == 14002) {
                                show(error.getPlainDescription(true) + "\n请确认是否已开通翻译功能");
                            } else {
                                show(error.getPlainDescription(true));
                            }
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
                    if (ret != ErrorCode.SUCCESS) {
                        show("听写失败,错误码：" + ret);
                    } else {
                        show(getString(R.string.text_begin));
                    }
                }
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
                speechSynthesis();
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

    private void speechSynthesis() {
        // 移动数据分析，收集开始合成事件
        FlowerCollector.onEvent(this, "tts_play");
        int code = mTts.startSpeaking(mResultText.getText().toString(), mTtsListener);
        if (code != ErrorCode.SUCCESS) {
            show("语音合成失败,错误码: " + code);
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

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            show("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            show("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            show("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
         //   合成进度
            mPercentForBuffering = percent;
            show(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            show(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                show("播放完成");
                mIatDialog.show();
            } else if (error != null) {
                show(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };


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
        //移动数据统计分析
        FlowerCollector.onResume(this);
        FlowerCollector.onPageStart(TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        //移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(this);
        super.onPause();
    }
}
