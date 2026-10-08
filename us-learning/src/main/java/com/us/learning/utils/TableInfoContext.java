package com.us.learning.utils;

/**
 * 动态表名的线程内传递工具（day08 笔记 2.4.1.2）
 * 定时任务中计算好表名存入，MybatisPlus的动态表名插件在同一个线程内读取
 */
public class TableInfoContext {
    private static final ThreadLocal<String> TL = new ThreadLocal<>();

    public static void setInfo(String info) {
        TL.set(info);
    }

    public static String getInfo() {
        return TL.get();
    }

    public static void remove() {
        TL.remove();
    }
}