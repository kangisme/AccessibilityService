package com.kang.accessibilityservice;

import java.util.Collections;
import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

/**
 * Created by kangren on 2018/1/3.
 */

public class WeChatService extends AccessibilityService {

    //微信6.6.6红包id
    private static final String RED_PACKET_ID = "com.tencent.mm:id/ad8";

    //微信6.6.6红包“开”id
    private static final String RED_PACKET_OPENID = "com.tencent.mm:id/c31";
    //中间页面，拆红包页面
    private static final String LUCKY_MONEY_RECEIVE_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    //红包详情页面
    private static final String LUCKY_MONEY_DETAIL_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    //微信6.6.6红包详情页面返回箭头id
    private static final String BACK_TO_LAUNCHER = "com.tencent.mm:id/i1";
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
        switch (eventType) {
            //通知栏状态发生变化
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                 handleNotification(event);
                 break;
            //窗口状态发生变化
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
                //拆红包界面
                if (LUCKY_MONEY_RECEIVE_UI.equals(className)) {
                    Logger.d("拆红包界面");
                    AccessibilityNodeInfo nodeInfo = event.getSource();
                    List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(RED_PACKET_OPENID);
                    if (list.isEmpty()) {
                        Toast.makeText(WeChatService.this, "无法打开红包,请更新版本", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AccessibilityNodeInfo info = list.get(0);
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    nodeInfo.recycle();
                }
                else if (LUCKY_MONEY_DETAIL_UI.equals(className)) {
                    Logger.d("红包详情界面");
                    AccessibilityNodeInfo sourceInfo = event.getSource();
                    List<AccessibilityNodeInfo> list = sourceInfo.findAccessibilityNodeInfosByViewId(BACK_TO_LAUNCHER);
                    if (!list.isEmpty()) {
                        list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    sourceInfo.recycle();
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                AccessibilityNodeInfo nodeInfo = event.getSource();
                List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(RED_PACKET_ID);
                if (list.isEmpty()) {
                    Logger.d("no red packet");
                } else {
                    //大于1红包，包括已领取的，从最新的红包开始领取
                    if (list.size() > 1) {
                        Collections.reverse(list);
                    }
                    for (AccessibilityNodeInfo temp : list) {
                        List<AccessibilityNodeInfo> infoList = temp.findAccessibilityNodeInfosByText("领取红包");
                        if (!infoList.isEmpty()) {
                            temp.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
                nodeInfo.recycle();
                break;
        }
    }

    /**
     * 处理通知消息
     * @param event
     */
    private void handleNotification(AccessibilityEvent event) {
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
    }

    @Override
    public void onInterrupt() {

    }
}
