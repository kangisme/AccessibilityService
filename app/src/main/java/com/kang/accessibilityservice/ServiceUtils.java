package com.kang.accessibilityservice;

import java.util.List;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;

import com.orhanobut.logger.Logger;

/**
 * Created by kangren on 2018/1/4.
 */

public class ServiceUtils {

    //检查服务是否开启
    public static boolean isServiceEnabled(Context context) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

        if (accessibilityManager == null) {
            Logger.e("accessibilityManager is null");
            return false;
        }

        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            String a = info.getId();
            if (info.getId().contains("com.kang.accessibilityservice/.WeChatService")) {
                return true;
            }
        }
        return false;
    }
}
