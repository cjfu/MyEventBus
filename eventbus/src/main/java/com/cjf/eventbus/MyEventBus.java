package com.cjf.eventbus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyEventBus {

    private Map<Object, List<MessageBean>> map;
    private static MyEventBus instance;
    private Handler handler;
    private ExecutorService executorService;

    public synchronized static MyEventBus getInstance() {
        if (instance == null) {
            instance = new MyEventBus();
        }
        return instance;
    }

    private MyEventBus() {
        map = new HashMap<>();
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * 注册
     *
     * @param context 需要注册的对象
     */
    public void register(Object context) {
        //在使用不当的情况下，同一个对象可能会被多次注册。故先判断对象是否已被注册
        if (map.get(context) == null) {
            map.put(context, new ArrayList<MessageBean>());
        } else {
            Log.e("MyEventBus", "context is registered");
            return;
        }
        Method[] methods = context.getClass().getDeclaredMethods(); //获取对象对应类的所有方法（不包含继承的方法）
        List<MessageBean> messageBeans = map.get(context);
        for (Method method : methods) {     //遍历寻找有目标注解的方法，并保存其相关信息
            EventReceiver eventReceiver = method.getAnnotation(EventReceiver.class);    //获取目标注解
            if (eventReceiver != null) {    //eventReceiver不为空代表找到了目标注解
                //若形参只有一个，且返回值为false，则视为方法符合条件，保存至内存。（不符合条件的在反射回调时会因参数不匹配而崩溃）
                Class[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1) {
                    if(method.getReturnType() == Void.class) {
                        MessageBean messageBean = new MessageBean();
                        messageBean.setMethod(method);
                        messageBean.setThreadMode(eventReceiver.value());
                        messageBean.setType(paramTypes[0]);
                        messageBeans.add(messageBean);
                    }else {
                        Log.e("MyEventBus", "Return type must be void");
                    }
                } else {
                    Log.e("MyEventBus", "Parameter must have only one");
                }
            }
        }
    }

    /**
     * 解绑
     *
     * @param context 需要解绑的对象
     */
    public void unRegister(Object context) {
        map.remove(context);
    }

    /**
     * 消息发送
     *
     * @param message 需要发送的消息
     */
    public void post(Object message) {
        Set<Object> objects = map.keySet();
        for (Object object : objects) {
            List<MessageBean> messageBeans = map.get(object);
            if (messageBeans == null || messageBeans.size() == 0) {
                continue;
            }
            for (MessageBean messageBean : messageBeans) { //遍历所有方法
                invoke(object, message, messageBean);   //反射调用方法（内含筛选）
            }
        }
    }

    /**
     * 反射调用方法（内含筛选）
     * @param object
     * @param message
     * @param messageBean
     */
    private void invoke(final Object object, final Object message, MessageBean messageBean) {
        final Method method = messageBean.getMethod();
        if (message.getClass().isAssignableFrom(messageBean.getType())) {   //若消息类型与回调方法类型一致
            switch (messageBean.getThreadMode()) {
                case ThreadMode.POSTING:    //
                    try {
                        method.invoke(object, message);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    break;
                case ThreadMode.MAIN:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                method.invoke(object, message);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    break;
                case ThreadMode.BACKGROUND:
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                method.invoke(object, message);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    break;
            }
        }
    }
}
