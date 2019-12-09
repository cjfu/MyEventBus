package com.cjf.eventbus;

import java.lang.reflect.Method;

class MessageBean {
    private Method method;
    private Class type;
    private int threadMode;

    Method getMethod() {
        return method;
    }

    void setMethod(Method method) {
        this.method = method;
    }

    Class getType() {
        return type;
    }

    void setType(Class type) {
        this.type = type;
    }

    int getThreadMode() {
        return threadMode;
    }

    void setThreadMode(int threadMode) {
        this.threadMode = threadMode;
    }
}
