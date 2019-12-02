package com.cjf.eventbus;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBus {

    private Map<Object, List<MessageManager>> map;
    private static EventBus instance;
    private Handler handler;
    private ExecutorService executorService;

    public synchronized static EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    private EventBus() {
        map = new HashMap<>();
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    public void register(Object context) {
        if (map.get(context) == null) {
            map.put(context, new ArrayList<MessageManager>());
        }
        Method[] methods = context.getClass().getDeclaredMethods();
        List<MessageManager> messageManagers = map.get(context);
        for (Method method : methods) {
            EventReceiver eventReceiver = method.getAnnotation(EventReceiver.class);
            if (eventReceiver != null) {
                MessageManager messageManager = new MessageManager();
                messageManager.setMethod(method);
                messageManager.setThreadMode(eventReceiver.value());
                Class[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1) {
                    messageManager.setType(paramTypes[0]);
                }
                messageManagers.add(messageManager);
            }
        }
    }

    public void unRegister(Object context) {
        map.remove(context);
    }

    public void post(Object message) {
        Set<Object> objects = map.keySet();
        for (Object object : objects) {
            List<MessageManager> messageManagers = map.get(object);
            if (messageManagers == null || messageManagers.size() == 0) {
                continue;
            }
            for (MessageManager messageManager : messageManagers) {
                try {
                    invoke(object,message, messageManager);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void invoke(final Object object, final Object message, MessageManager messageManager) throws InvocationTargetException, IllegalAccessException {
        final Method method = messageManager.getMethod();
        if (message.getClass().isAssignableFrom(messageManager.getType())) {
            switch (messageManager.getThreadMode()) {
                case ThreadMode.POSTING:
                    try {
                        method.invoke(object,message);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    break;
                case ThreadMode.MAIN:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                method.invoke(object,message);
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
                                method.invoke(object,message);
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
