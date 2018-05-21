package com.kang.accessibilityservice;

import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

/**
 * Created by kangren on 2018/1/3.
 */

public class WeChatService extends AccessibilityService {

    //微信6.6.6红包id
    private final String RED_PACKET_ID = "com.tencent.mm:id/ad8";

    //微信6.6.6红包“开”id
    private final String RED_PACKET_OPENID = "com.tencent.mm:id/c31";

    //微信6.6.6红包“查看红包”id，用于判断红包是否被领取
    private final String IS_READ_PACKET_OPENED = "com.tencent.mm:id/ae_";

    @Override
    protected void onServiceConnected() {

    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String eventText = "";
        switch (eventType) {
//            case AccessibilityEvent.TYPE_VIEW_CLICKED:
//                eventText = "TYPE_VIEW_CLICKED";
//                break;
//            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
//                eventText = "TYPE_VIEW_FOCUSED";
//                break;
//            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
//                eventText = "TYPE_VIEW_LONG_CLICKED";
//                break;
//            case AccessibilityEvent.TYPE_VIEW_SELECTED:
//                eventText = "TYPE_VIEW_SELECTED";
//                break;
//            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
//                eventText = "TYPE_VIEW_TEXT_CHANGED";
//                break;
            //窗口状态发生变化
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                eventText = "TYPE_WINDOW_STATE_CHANGED";
                String className = event.getClassName().toString();
                AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
                if (rootInActiveWindow == null) {
                    Toast.makeText(WeChatService.this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                //聊天窗口界面
                if ("com.tencent.mm.ui.LauncherUI".equals(className)) {
                    List<AccessibilityNodeInfo> list = rootInActiveWindow.findAccessibilityNodeInfosByViewId(RED_PACKET_ID);
                    if (list == null) {
                        Logger.e("red packet list is null");
                        return;
                    }
                    int size = list.size();
                    if (size > 0) {
                        //获取最新一个红包
                        AccessibilityNodeInfo lastInfo = list.get(size - 1);
                        List<AccessibilityNodeInfo> temp = lastInfo.findAccessibilityNodeInfosByText("领取红包");
                        if (temp == null || temp.size() == 0) {
                            //模拟点击事件
                            lastInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    } else {
                        Logger.d("red packet list is empty");
                    }
                }
                //拆红包界面
                else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(className)) {
                    List<AccessibilityNodeInfo> list = rootInActiveWindow.findAccessibilityNodeInfosByViewId(RED_PACKET_OPENID);
                    if (list == null || list.size() == 0) {
                        Toast.makeText(WeChatService.this, "无法打开红包,请更新版本", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AccessibilityNodeInfo info = list.get(0);
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                break;
            //通知栏状态发生变化
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                eventText = "TYPE_NOTIFICATION_STATE_CHANGED";
                Logger.d(eventText);
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if (content.contains("[微信红包]")) {
                            //模拟打开通知栏消息，即打开微信
                            if (event.getParcelableData() != null &&
                                    event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pendingIntent.send();
                                    Logger.d("进入微信");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                break;
//            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
//                eventText = "TYPE_TOUCH_EXPLORATION_GESTURE_END";
//                break;
//            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
//                eventText = "TYPE_ANNOUNCEMENT";
//                break;
//            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
//                eventText = "TYPE_TOUCH_EXPLORATION_GESTURE_START";
//                break;
//            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
//                eventText = "TYPE_VIEW_HOVER_ENTER";
//                break;
//            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
//                eventText = "TYPE_VIEW_HOVER_EXIT";
//                break;
//            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
//                eventText = "TYPE_VIEW_SCROLLED";
//                break;
//            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
//                eventText = "TYPE_VIEW_TEXT_SELECTION_CHANGED";
//                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                eventText = "TYPE_WINDOW_CONTENT_CHANGED";
//                AccessibilityNodeInfo nodeInfo = event.getSource();
//                List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
//                if (list == null)
//                {
//                    Logger.d("list is null");
//                }
//                else
//                {
//                    for (AccessibilityNodeInfo info : list)
//                    {
//                        Logger.d(info.toString());
//                        info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    }
//                }
                break;
        }
    }

    @Override
    public void onInterrupt() {

    }
}
