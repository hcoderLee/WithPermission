package com.lee.withpermission;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 广告模块用于申请权限的request id
 */
class RequestId {
    // 任务增加时自增, 设定初始request id为100，避免跟其他request id重复
    static AtomicInteger id = new AtomicInteger(100);
}
