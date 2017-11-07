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
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import zh_wxassistant.com.activity.AutoSendMsgActivity;
import zh_wxassistant.com.app.ZhWxAssistantApplication;
import zh_wxassistant.com.communication.AssistantToWx;
import zh_wxassistant.com.communication.WxToAssistant;
import zh_wxassistant.com.general.Constant;

/**
 * Created by Fzj on 2017/10/26.
 */

public class AssistantService extends AccessibilityService implements WxToAssistant {
    //用于存储开红包的HashMap集合对象
    private HashMap<AccessibilityNodeInfo,Boolean> hashMapParents=new HashMap<>();
    //消息传递机制
    private static AssistantToWx mAssistantToWx;
    /**
     * 当启动服务的时候就会被调用
     */
    @Override
    protected void onServiceConnected() {
        try {
            /*这里使用反射静态时刻加载类，静态类静态变量会在编译时刻加载所以只要当服务开启setInerface
              的回调方法会率先加载进入预执行状态。利用此特性结合接口回调
              等同于当前的service类与AutoSendMsgActivity事先签订好了回调协约
              注意点：区分编译与运行
              编译时刻加载类是静态加载类，运行时刻加载类是动态加载类
             */
                Class c= Class.forName("zh_wxassistant.com.activity.AutoSendMsgActivity");
                AutoSendMsgActivity c1=(AutoSendMsgActivity)c.newInstance();
                c1.setWxToAssistantInerface(AssistantService.this);
                //
                mAssistantToWx=c1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onServiceConnected();
    }

//    //用于向Activity形成订阅关系
//    public static void setAssistantToWxInerface(AssistantToWx assistantToWx){
//        mAssistantToWx=assistantToWx;
//    }
    /**
     * 监听窗口变化的回调
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        Log.e("demo", "当前类名: " + className);
        switch (eventType) {
            //当通知栏发生改变时
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.e("demo", "通知栏状态改变");
                if (isScreenLocked()){
                    wakeAndUnlock();//解锁
                }
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if (content.contains("[微信红包]")) {
                            //模拟打开通知栏消息，即打开微信
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
                        }else if (content.contains("[图片]")){

                        }else if (content.contains("[语音]")){

                        }else{
                         //文本消息
                            Boolean b=isAppForeground(Constant.PackAgeName);
                            Log.e("demo","是否在前台："+b);
                            Intent mWxIntent=new Intent(ZhWxAssistantApplication.getContextObject(),AutoSendMsgActivity.class);
                            mWxIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mWxIntent.putExtra("WxMsg",content);
                            startActivity(mWxIntent);
//                            mAssistantToWx.AtoWMsg(content);
                            //形成订阅关系

                        }
                    }
                }
                break;
            //当窗口的状态发生改变时
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.e("demo", "窗口事件改变");
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    //进入聊天窗口，点击最后一个红包
                    Log.e("demo", "点击红包");
                    //找到特定的联系人，然后发送打招呼
                    if (mName!=null&&mTextContent!=null){
                        Log.e("demo", "指定联系人："+mName);
                        autoSendMsg("com.tencent.mm:id/ak1");
                    }
                    getLastPacket();
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f")) {
                    //开红包窗口
                    Log.e("demo", "开红包");
                    openPacket("com.tencent.mm:id/brt");
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    //红包详情窗口退出红包
                    Log.e("demo", "退出红包");
                    quitPacket("com.tencent.mm:id/hg");
                }
                break;
        }
    }


    /**
     * 判断指定的应用是否在前台运行
     *
     * @param packageName
     * @return
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
    private KeyguardManager.KeyguardLock kl;//键盘锁管理类
    private Boolean islocked=false;
    private void wakeAndUnlock() {
        islocked=true;
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

    //
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
        Iterator iter=hashMapParents.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry entry= (Map.Entry) iter.next();
            AccessibilityNodeInfo redPacket= (AccessibilityNodeInfo) entry.getKey();
            Boolean redPacketState= (Boolean) entry.getValue();
            //为true时是没开过的红包否则为打开过的，红包开过之后设置为false
            if (redPacketState){
                redPacket.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                //红包被打开后，标记为false
                hashMapParents.put(redPacket,false);
            }
        }
    }

    /**
     * 回归函数遍历每一个节点，并将含有"领取红包"存进List中
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
                            hashMapParents.put(parent,true);
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
        pressBackButton();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            for (AccessibilityNodeInfo item : list) {
                AccessibilityNodeInfo parent = item;
                while (parent!=null){
                    if (parent.isClickable()){
                        Log.e("demo", clickId + "退出红包" + item.isClickable());
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                    parent=parent.getParent();
                }

            }
        }
    }

    /**
     * 通过ID获取控件，并进行模拟点击
     *
     * @param text
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void autoSendMsg(String text) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(text);
            for (AccessibilityNodeInfo item : list) {
                //在聊天列表项找到指定的联系人
                if (item.getText().toString().equals(mName)){
                    AccessibilityNodeInfo parent = item;
                    while (parent!=null){
                        if (parent.isClickable()){
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (fill()){
                                send();
                            }
                            break;
                        }
                        parent=parent.getParent();
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
            return findEditText(rootNode," "+mTextContent);
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
                    mName=null;
                    mTextContent=null;
                }
            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo.findAccessibilityNodeInfosByText("Send");
                //英文版
                if (liste != null && liste.size() > 0) {
                    for (AccessibilityNodeInfo n : liste) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        mName=null;
                        mTextContent=null;
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
        android.util.Log.d("maptrix", "root class=" + rootNode.getClassName() + ","+ rootNode.getText()+","+count);
        //遍历子ViewGroup
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                android.util.Log.d("maptrix", "nodeinfo = null");
                continue;
                //没有拿到对象结束本次循环开启下次循环
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

            //判断是否是输入框
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
    private void pressBackButton(){
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("input keyevent " + KeyEvent.KEYCODE_BACK);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {
       Log.w("dome","停止AssistantService服务");
    }

    //利用接口回调拿到用户信息
    private String mName=null;
    private String mTextContent=null;
    @Override
    public void name(String name) {
        this.mName=name;
        Log.e("demo","从activity传过来的数值类型："+mName);
    }

    @Override
    public void msgContent(String textContent) {
        this.mTextContent=textContent;
        Log.e("demo","从activity传过来的字符串类型："+mTextContent);
    }
}
