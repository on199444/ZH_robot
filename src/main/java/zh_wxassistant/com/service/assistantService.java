package zh_wxassistant.com.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Fzj on 2017/10/26.
 */

public class assistantService extends AccessibilityService  {
    public static  String transInfo=" ";
    private String contactsName=" ";
    //用于存储开红包的集合对象
    private List<AccessibilityNodeInfo> parents;
    private HashMap<AccessibilityNodeInfo,Boolean> hashMapParents=new HashMap<>();
    /**
     * 当启动服务的时候就会被调用
     */
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        parents = new ArrayList<>();
    }

    /**
     * 监听窗口变化的回调
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        Log.e("demo", "当前类名: " + className+transInfo);
        switch (eventType) {
            //当通知栏发生改变时
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.e("demo", "通知栏状态改变"+transInfo);
                break;
            //当窗口的状态发生改变时
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.e("demo", "窗口事件改变"+transInfo);
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    //进入聊天窗口，点击最后一个红包
                    Log.e("demo", "点击红包"+transInfo);
                    //找到特定的联系人，然后发送打招呼
                    contactsName=transInfo.substring(0,transInfo.length()-3);
                    Log.e("demo", "指定联系人："+contactsName);
                    inputClick1("com.tencent.mm:id/ak1");
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f")) {
                    //开红包窗口
                    Log.e("demo", "开红包"+transInfo);
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    //红包详情窗口退出红包
                    Log.e("demo", "退出红包"+transInfo);
                }
                break;
        }
    }

    /**
     * 通过ID获取控件，并进行模拟点击
     *
     * @param text
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void inputClick1(String text) {
//        pressBackButton();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(text);
            for (AccessibilityNodeInfo item : list) {
                if (item.getText().toString().equals(contactsName)){
                    Log.e("demo", contactsName);
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

//                Log.e("demo", clickId + "退出红包" + item.isClickable());
//                item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                pressBackButton();
                //  back2Home();
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
            return findEditText(rootNode," "+transInfo);
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
                }
            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo.findAccessibilityNodeInfosByText("Send");
                //英文版
                if (liste != null && liste.size() > 0) {
                    for (AccessibilityNodeInfo n : liste) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
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
       Log.w("dome","改变一点东西奥术大师多！");
    }
}
