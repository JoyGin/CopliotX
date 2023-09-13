package com.joygin.copliotx.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.GestureDescription.StrokeDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkipADService extends AccessibilityService {

    private static final String TAG = "SkipADService";

    Set<String> mInputMethodApps;
    Set<String> mHomeApps;
    private AccessibilityNodeInfo mLastClickNode;

    @Override
    protected void onServiceConnected() {
        // 获取crash信息
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
                Log.e(TAG, "------------Crash(Base)--------------");
                StackTraceElement[] stackTrace = ex.getStackTrace();
                StringBuilder sb = new StringBuilder();
                sb.append("Uncaught exception: ").append(ex).append("\n");
                for (StackTraceElement element : stackTrace) {
                    sb.append(element.toString()).append("\n");
                }
                Log.e(TAG, sb.toString());
                Log.e(TAG, "----------------------------------------");
            }
        });

        mInputMethodApps = getInputMethodApps();
        mHomeApps = getHomeApps();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
//            printTargetNode(getRootInActiveWindow());
            AccessibilityNodeInfo targetNode = findTargetNode(getRootInActiveWindow());
            if (targetNode == null) {
                return;
            } else if (targetNode.equals(mLastClickNode)) {
                Log.i(TAG, "onAccessibilityEvent: 与上次点击按钮相同，跳过");
                return;
            }
            printAccessibilityNode(targetNode);
            clickNode(targetNode);
        } else {
            Log.i(TAG, "onAccessibilityEvent: " + event);
        }
    }

    private AccessibilityNodeInfo findTargetNode(AccessibilityNodeInfo root) {
        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();
        if (root != null) {
            deque.add(root);
        }
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            // find the node which is clickable and has text contains ”跳过“
            if (node.getText() != null && node.getText().toString().contains("跳过")) {
                String packageName = node.getPackageName().toString();
                if (!mInputMethodApps.contains(packageName) && !mHomeApps.contains(packageName)) {
                    return node;
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    deque.add(node.getChild(i));
                }
            }
        }
        return null;
    }

    private void printAccessibilityNode(AccessibilityNodeInfo node) {
        if (node != null) {
            Log.d(TAG, "printAccessibilityNode: node = " + node);
        }
    }

    /**
     * 点开按钮
     *
     * @param node 节点
     */
    private void clickNode(AccessibilityNodeInfo node) {
        if (node != null) {

//            // 有些情况无法实现点击
//            if (node.isClickable()) {
//                boolean result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                if (result) {
//                    Toast.makeText(this, "跳过广告", Toast.LENGTH_SHORT).show();
//                } else {
//                    Log.i(TAG, "跳过广告失败");
//                }
//            }

            Path clickPath = new Path();
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            if (rect.centerX() > 0 && rect.centerY() > 0) {
                clickPath.moveTo(rect.centerX(), rect.centerY());
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                gestureBuilder.addStroke(new StrokeDescription(clickPath, 0, 500));
                dispatchGesture(gestureBuilder.build(), null, null);
                Toast.makeText(this, "跳过广告", Toast.LENGTH_SHORT).show();
                mLastClickNode = node;
            }
        }
    }

    /**
     * 获取输入法类的应用
     *
     * @return 输入法类的应用
     */
    private Set<String> getInputMethodApps() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        List<InputMethodInfo> inputMethods = inputMethodManager.getInputMethodList();

        Set<String> resultList = new HashSet<>();
        for (InputMethodInfo inputMethod : inputMethods) {
            String packageName = inputMethod.getPackageName();
            resultList.add(packageName);
        }
        Log.i(TAG, "getInputMethodApps: " + resultList);
        return resultList;
    }

    /**
     * 获取启动器类的应用
     *
     * @return 启动器类的应用
     */
    private Set<String> getHomeApps() {
        PackageManager packageManager = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> launcherApps = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL);

        Set<String> homeApps = new HashSet<>();
        for (ResolveInfo app : launcherApps) {
            String packageName = app.activityInfo.packageName;
            homeApps.add(packageName);
        }
        Log.i(TAG, "getHomeApps: " + homeApps);
        return homeApps;
    }

    @Override
    public void onInterrupt() {
    }

    private void printTargetPackageNode(AccessibilityNodeInfo root) {
//        String packageName = "com.tencent.mtt";
        String targetPackageName = "com.netease.newsreader";
        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();
        if (root != null) {
            deque.add(root);
        }
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            if (node.getPackageName().toString().contains(targetPackageName)) {
                Log.i(TAG, "node: " + node);
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    deque.add(node.getChild(i));
                }
            }
        }
    }
}
