package com.lee.withpermission;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class PermissionManager {
    private static final String TAG = "MS-PermissionManager";
    // 缓存需要相关权限才能执行的任务，当用户授权后执行
    private static volatile Map<Integer, WithPermissionTask> permissionTasks = new TreeMap<>();
    private static Handler handler;
    // 缓存队列请依次求权限的流程是否被打断（连续申请权限导致授权流程被打断）
    private static boolean isBlock;
    // 当前任务队列执行期间被拒绝的权限，队列中的任务依次请求权限，如果有被拒绝，则不重新申请，直到任务队列全部请求完，清空此缓存
    // 目的是不连续弹出授权窗请求已被拒绝的权限
    private static Set<String> requestDenied = new HashSet<>();

    private static Handler getMainHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    /**
     * 授权回调接口， 在UI线程执行
     *
     * @param activity
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public static void onRequestPermissionsResult(final Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 当前授权被打断
        boolean isCurrentBlock = permissions.length == 0;
        // 是否为缓存队列中的请求，requestCode与permissions必须完全一致
        boolean isTaskRequest = permissionTasks.containsKey(requestCode) && permissionTasks.get(requestCode).hasSamePermissions(permissions);

        // 过滤非缓存队列的申请
        if (!isTaskRequest) {
            // 如果当前缓存队列依次申请权限的流程被阻塞，重新开始申请权限的流程
            if (isBlock && !isCurrentBlock) {
                isBlock = false;
                // 为缓存队列中的下个任务请求权限
                scheduleNextRequest(activity);
            }
            return;
        }

        // 处理缓存队列中的请求
        final WithPermissionTask task = permissionTasks.get(requestCode);

        // 连续申请权限（授权弹窗还未消失时再次申请权限）导致授权取消
        if (isCurrentBlock) {
            // 缓存队列依次申请权限的流程被阻塞
            isBlock = true;
            permissionTasks.remove(requestCode);
            // 需要将任务重新添加到队列，等待申请权限
            permissionTasks.put(RequestId.id.incrementAndGet(), task);
            return;
        }

        boolean isGrant = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                isGrant = false;
                // 缓存队列执行期间被拒绝的权限
                requestDenied.add(permissions[i]);
            }
        }

        if (isGrant) {
            Log.d(TAG, "execute task " + requestCode);
            // 已授权，执行任务
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    task.onGrant();
                }
            });
        } else {
            // 未获取授权
            Log.d(TAG, "cannot execute task " + requestCode + ", permission denied");
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    task.onDenied();
                }
            });
        }

        // 不管是否授权成功，都移除任务
        permissionTasks.remove(requestCode);
        // 为缓存队列中的下个任务请求权限
        scheduleNextRequest(activity);
    }

    /**
     * 从缓存队列中取出一个任务，并请求权限
     * 如任务包含缓存的被拒绝的权限，则不请求权限，直接执行task.onDenied()
     */
    private static void scheduleNextRequest(final Activity activity) {
        Iterator<Map.Entry<Integer, WithPermissionTask>> perIter = permissionTasks.entrySet().iterator();
        while (perIter.hasNext()) {
            final Map.Entry<Integer, WithPermissionTask> perEntry = perIter.next();
            final int nextRequestCode = perEntry.getKey();
            final WithPermissionTask nextTask = perEntry.getValue();

            // 当前任务包含缓存的被拒绝权限，尝试请求下个任务的权限
            if (hasDined(nextTask)) {
                Log.d(TAG, "cannot execute task " + nextRequestCode + ", permission denied");
                nextTask.onDenied();
                perIter.remove();
                continue;
            }

            // 下一个任务请求权限
            getMainHandler().post(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    Log.d(TAG, "request permission, requestCode: " + nextRequestCode);
                    nextTask.requestPermission(activity, nextRequestCode);
                }
            });
            return;
        }

        // 任务队列全部请求完，清空被拒绝权限的缓存，确保下次能正常申请
        requestDenied.clear();
        Log.d(TAG, "clear denied permissions");
    }

    /**
     * 当前任务是否包含缓存的被拒绝权限
     *
     * @param task
     * @return
     */
    private static boolean hasDined(WithPermissionTask task) {
        for (String p : task.getPermissions()) {
            if (requestDenied.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 确保task一定在获得授权后执行
     *
     * @param activity
     * @param task     需要权限才能执行的任务
     */
    public static void withPermission(Activity activity, WithPermissionTask task) {
        // Android 6.0 以下不处理动态权限，直接执行任务
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            task.onGrant();
            return;
        }

        // 如果已授予权限，则直接执行任务
        if (task.isGrant(activity)) {
            Log.d(TAG, "task already has permissions, execute directly");
            task.onGrant();
            return;
        }

        if (activity == null) {
            Log.d(TAG, "activity is not valid, request permission fail");
            return;
        }

        // 未授权限
        int requestCode = RequestId.id.getAndIncrement();
        boolean needRequest = permissionTasks.isEmpty();
        // 加入等待队列中
        permissionTasks.put(requestCode, task);
        Log.d(TAG, "add task: " + requestCode + " to queue");
        if (needRequest) {
            // 缓存队列为空，直接请求权限
            Log.d(TAG, "request permission, requestCode: " + requestCode);
            task.requestPermission(activity, requestCode);
        }
    }
}
