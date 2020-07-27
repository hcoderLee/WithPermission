package com.lee.withpermission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

/**
 * 封装需要某些权限才能执行的任务
 */
abstract public class WithPermissionTask {
    private String[] permissions;

    protected WithPermissionTask(@NonNull String[] permissions) {
        this.permissions = permissions;
    }

    public String[] getPermissions() {
        return permissions;
    }

    /**
     * 需要申请的权限是否全部授予
     *
     * @param context
     * @return
     */
    boolean isGrant(Context context) {
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * permission 是否包含所有task需要用到的权限
     *
     * @param permissions 权限数组
     * @return
     */
    boolean hasSamePermissions(@NonNull String[] permissions) {
        if (this.permissions.length != permissions.length) {
            return false;
        }

        for (int i = 0; i < permissions.length; i++) {
            if (!permissions[i].equals(this.permissions[i])) {
                return false;
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void requestPermission(Activity activity, int requestCode) {
        activity.requestPermissions(permissions, requestCode);
    }

    /**
     * 需要申请到权限才能调用的代码，授权成功后调用
     */
    protected abstract void onGrant();

    /**
     * 用户拒绝权限时调用
     */
    protected void onDenied() {
    }
}
