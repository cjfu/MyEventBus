package com.cjf.eventbus;

public class ThreadMode {
    /**
     * 不切换线程，与post所在线程一致
     */
    public static final int POSTING = 0;
    /**
     * 始终执行在主线程
     */
    public static final int MAIN = 1;
    /**
     * 始终执行在子线程
     */
    public static final int BACKGROUND = 2;
}
