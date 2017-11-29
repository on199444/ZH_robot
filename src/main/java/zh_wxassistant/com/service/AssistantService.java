package zh_wxassistant.com.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import zh_wxassistant.com.activity.AutoSendMsgActivity;
import zh_wxassistant.com.app.ZhWxAssistantApplication;
import zh_wxassistant.com.communication.WxToAssistant;

/**
 * Created by Fzj on 2017/10/26.
 */

public class AssistantService extends AccessibilityService implements WxToAssistant {
    private static final String TAG = "demo";
    //用于存储开红包的HashMap集合对象
    private HashMap<AccessibilityNodeInfo, Boolean> hashMapParents = new HashMap<>();
    /**
     * 当启动服务的时候就会被调用
     */
    private AutoSendMsgActivity autoSendMsgActivity;
    private String mCurrenClassName = null;
    //用于接口回调处记录指定发送的用户名和内容
    private String mNewMsg =" ";
    private String mAppointContactsMsg = null;
    private KeyguardManager.KeyguardLock kl;//键盘锁管理类
    private Boolean islocked = false;

    @Override
    protected void onServiceConnected() {
        try {
            /*这里使用反射静态时刻加载类，静态类静态变量会在编译时刻加载所以只要当服务开启setInerface
              的回调方法会率先加载进入预执行状态。利用此特性结合接口回调
              等同于当前的service类与AutoSendMsgActivity事先签订好了回调协约
              注意点：区分编译与运行
              编译时刻加载类是静态加载类，运行时刻加载类是动态加载类
             */
            autoSendMsgActivity = (AutoSendMsgActivity) Class.forName("zh_wxassistant.com.activity.AutoSendMsgActivity").newInstance();
            //avtivity向service定制消息机制（反射）
            autoSendMsgActivity.setWxToAssistantInerface(AssistantService.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onServiceConnected();
    }

    /**
     * 监听窗口变化的回调
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        mCurrenClassName = event.getClassName().toString();
        Log.e("demo", "当前类名: " + mCurrenClassName);
        switch (eventType) {
            //当通知栏发生改变时，处理各种消息类型
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.e("demo", "通知栏状态改变");
                //解锁
                if (isScreenLocked()) {wakeAndUnlock();}
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        mNewMsg = text.toString();
                        //这里需要设置一个flag记住当前的信息类型？
                        if (mNewMsg.contains("[微信红包]") || mNewMsg.contains("[图片]") || mNewMsg.contains("[语音]")) {
                            //模拟打开通知栏消息，即打开微信
                            openWx(event);
                        } else {
                            //如果是文本消息的话通过广播发送到App内进行朗读
                            Intent mWxIntent = new Intent(ZhWxAssistantApplication.getContextObject(), AutoSendMsgActivity.class);
                            mWxIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(mWxIntent);
                            //延时发送广播,以免Activity接收不到
                            Timer mTimer = new Timer();
                            TimerTask mTimerTask = new TimerTask() {
                                public void run() {
                                    sendBroadcast(new Intent("myBroadcastReceiver").putExtra("msg", mNewMsg));
                                }
                            };
                            mTimer.schedule(mTimerTask, 2000);
                        }
                    }
                }
                break;
            //当窗口的状态发生改变时
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.e("demo", "窗口事件改变");
                if (mCurrenClassName.equals("com.tencent.mm.ui.LauncherUI")) {
                    //在最近的联系人item下找到指定名字的联系人，发送消息
                  if (mNewMsg.contains("[微信红包]")) {
                        //进入聊天窗口，点击最后一个红包
                        getLastPacket();
                    } else if (mNewMsg.contains("[图片]")) {
                        //去点击最后一张图片
                        quitPacket("com.tencent.mm:id/ad7");
                    } else if (mNewMsg.contains("[语音]")) {
                        //去点击最后一个语音
                        quitPacket("com.tencent.mm:id/aeq");
                    }else if (mAppointContactsMsg.equals("朋友圈")){
                        Log.e("demo","进来");
                        findComp("com.tencent.mm:id/c2z");
                    }else if (mAppointContactsMsg != null){
                        appointContactsSendMsg(mAppointContactsMsg,"com.tencent.mm:id/aoj");
                    }

                } else if (mCurrenClassName.equals("com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f")) {
                    //开红包窗口
                    Log.e("demo", "开红包");
                    openPacket("com.tencent.mm:id/brt");
                } else if (mCurrenClassName.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    //红包详情窗口退出红包
                    Log.e("demo", "退出红包");
                    quitPacket("com.tencent.mm:id/hj");
                } else if (mCurrenClassName.equals("com.tencent.mm.plugin.sns.ui.En_424b8e16")) {
                    Log.e(TAG, "朋友圈");

                }
                break;
        }
    }

    private void openWx(AccessibilityEvent event) {
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
                Log.e("demo", "进入微信");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断指定的应用是否在前台运行
     */
    private boolean isAppForeground(String packageName) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        //当前包名不为空，当前应用是微信应用
        if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(packageName)) {
            return true;
        }
        return false;
    }

    /**
     * 系统是否在锁屏状态
     */
    private boolean isScreenLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.inKeyguardRestrictedInputMode();
    }

    private void wakeAndUnlock() {
        islocked = true;
        //获取电源管理器对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        //点亮屏幕
        wl.acquire(1000);
        //得到键盘锁管理器对象
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("");
        //解锁
        kl.disableKeyguard();
    }

    private void release() {
        if (islocked && kl != null) {
            android.util.Log.d("maptrix", "release the lock");
            //重新启用键盘锁对象
            kl.reenableKeyguard();
            //重置
            islocked = false;
        }
    }

    /**
     * 获取List中最后一个红包，并进行模拟点击
     */
    private void getLastPacket() {
        //获得当前根窗口的所有元素
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
        Iterator iter = hashMapParents.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            AccessibilityNodeInfo redPacket = (AccessibilityNodeInfo) entry.getKey();
            Boolean redPacketState = (Boolean) entry.getValue();
            //为true时是没开过的红包否则为打开过的，红包开过之后设置为false
            if (redPacketState) {
                redPacket.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                //红包被打开后，标记为false
                hashMapParents.put(redPacket, false);
            }
        }
    }

    /**
     * 回归函数遍历每一个节点，并将含有"领取红包"存进List中
     *
     * @param info
     */
    public void recycle(AccessibilityNodeInfo info) {
        //到这里判断一下是否是viewGroup类型的父控件，否则开始记录红包
        if (info.getChildCount() == 0) {
            if (info.getText() != null) {
                //先判断此红包是否可以领取，凡是为领取的红包皆加入map集合中并标记为true
                if ("领取红包".equals(info.getText().toString())) {
                    //“获取红包”文本的父控件，用作点击事件
                    AccessibilityNodeInfo parent = info.getParent();
                    while (parent != null) {
                        //当父级节点不为空且可点击时依次加入集合，这样所有可领取的红包都加入了集合
                        if (parent.isClickable()) {
//                            parents.add(parent);
                            hashMapParents.put(parent, true);
                            //加入集合
                            break;
                        }
                        //？
                        parent = parent.getParent();
                    }
                }
            }
        } else {
            //还有下级子节点的情况，先把子节点先取出来针对聊天界面为父容器viewGroup类型拥有子View的组件
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    //递归，遍历调用viewGroup里面的子控件
                    recycle(info.getChild(i));
                }
            }
        }
    }

    /**
     * 通过ID获取控件，并进行模拟点击
     *
     * @param clickId
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void openPacket(String clickId) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            for (AccessibilityNodeInfo item : list) {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        private void quitPacket(String clickId) {
//        pressBackButton();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            for (AccessibilityNodeInfo item : list) {
                AccessibilityNodeInfo parent = item;
                while (parent != null) {
                    if (parent.isClickable()) {
                        Log.e("demo", clickId + "退出红包" + item.isClickable());
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        }
    }

    private void findComp(String clickId){
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            for (AccessibilityNodeInfo item : list) {
                AccessibilityNodeInfo parent = item;
                Log.e("demo",parent.getText().toString());
                while (parent != null) {
                    if (parent.isClickable()) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        }
    }

    /**
     * 通过ID获取控件，并进行模拟点击
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void appointContactsSendMsg(String contacts, String  clickid) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            //在聊天列表项找到指定的联系人,这里今后应该改进为全局查找而不局限于当前的用户界面
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickid);
            for (AccessibilityNodeInfo item : list) {
                if (contacts.contains(item.getText().toString())) {
                    AccessibilityNodeInfo parent = item;
                    while (parent != null) {
                        if (parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (fill()) {
                                send();
                            }
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        }
    }

    /*
     * 准备发送信息，将消息提取然后粘贴在EditText的输入框内
     * */
    @SuppressLint("NewApi")
    private boolean fill() {
        //进入到窗口通过AccessAbalityService得到根视图树
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            return findEditText(rootNode, " " + mAppointContactsMsg);
        }
        return false;
    }

    /**
     * 寻找窗体中的“发送”按钮，并且点击。
     */
    @SuppressLint("NewApi")
    private void send() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("发送");
            //中文版
            if (list != null && list.size() > 0) {
                for (AccessibilityNodeInfo n : list) {
                    n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    //成功回应消息后清空接受用户名和消息
                    mAppointContactsMsg = null;
                }
            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo.findAccessibilityNodeInfosByText("Send");
                //英文版
                if (liste != null && liste.size() > 0) {
                    for (AccessibilityNodeInfo n : liste) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        mAppointContactsMsg = null;
                    }
                }
            }
            pressBackButton();
        }
    }

    /*
   * @param rootNode 传入的聊天根视图树
   * @param content  回复的文本内容
   * */
    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();
        android.util.Log.d("maptrix", "root class=" + rootNode.getClassName() + "," + rootNode.getText() + "," + count);
        //遍历子ViewGroup
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            //没有拿到对象结束本次循环开启下次循环
            if (nodeInfo == null) {continue;
            }
            android.util.Log.d("maptrix", "class=" + nodeInfo.getClassName());
            android.util.Log.e("maptrix", "ds=" + nodeInfo.getContentDescription());
            //拿到对象后获取内容描述
//            if(nodeInfo.getContentDescription() != null){
//                //根据字符匹配消息内容拿到对应的索引
//                int nindex = nodeInfo.getContentDescription().toString().indexOf(name);
//                int cindex = nodeInfo.getContentDescription().toString().indexOf(scontent);
//                android.util.Log.e("maptrix", "nindex=" + nindex + " cindex=" +cindex);
//                if(nindex != -1){
//                    itemNodeinfo = nodeInfo;
//                    android.util.Log.i("maptrix", "find node info");
//                }
//            }

            //聊天界面只有一个输入框判断是否是输入框
            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                android.util.Log.i("maptrix", "==================");
                Bundle arguments = new Bundle();
                //1在遍历节点文本时使用哪个运动粒度的参数
                // 2用于逐词遍历节点文本的运动粒度位
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                //参数是否在粒度上移动扩展选择
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
                //获取当前组件的焦点
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                //创建新文本消息
                ClipData clip = ClipData.newPlainText("label", content);
                //启动全局剪贴板服务
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                //利用AccessAbilityService粘贴到剪贴板上
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }
            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }
        return false;
    }

    /*
    * 回到桌面
    * */
    private void back2Home() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    /**
     * 模拟back按键
     */
    private void pressBackButton() {
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("input keyevent " + KeyEvent.KEYCODE_BACK);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {
        Log.w("dome", "停止AssistantService服务");
    }

    //利用接口回调拿到用户信息,截取信息的前4个字为用户名
    @Override
    public void appointMsgToContacts(String info) {
        this.mAppointContactsMsg = info;
        Log.e("demo", "从activity传过来的字符串类型：" + mAppointContactsMsg);
    }
}
