package com.cjf.eventbus;

import java.lang.reflect.Method;

public class MessageManager {
    Method method;
    Class type;
    int threadMode;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public int getThreadMode() {
        return threadMode;
    }

    public void setThreadMode(int threadMode) {
        this.threadMode = threadMode;
    }
}
