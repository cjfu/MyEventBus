# 背景

Android开发中，EventBus是目前比较流行的消息总线框架。在当下流行的模块化、组件化中也扮演着重要作用。相信其原理小伙伴们也都了解。它的核心逻辑其实仅需百余行代码即可写出。今天我们就来手撸一次EventBus简易版框架。

# 用法
在手撸框架之前，我们需要先定义一下框架的用法。这里我们仿EventBus的用法，详情如下：
1、在对象初始化时需要注册EventBus，而在对象销毁时则需要解绑。示例：

```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getInstance().register(this);
    }
```

```java
    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unRegister(this);
    }
```

2、EventBus接收消息的方法使用特定注解标识，形参不限，方法名不限。示例：

```java
    @EventReceiver()
    public void getMessage1(String str){
        ···
    }
```
3、EventBus消息处理方法可以通过注解来使其执行在主线程或子线程，或不切换线程。示例：

```java
    @EventReceiver(ThreadMode.MAIN)
    public void getMessage2(User user){
        ···
    }
```
4、EventBus消息可以在任意地点使用post方法发送，且已注册EventBus的对象对应注解的方法可以触发回调。示例：

```java
	EventBus.getInstance().post(user);
```
# 思路
1、我们可以通过自定义注解来标识并找到一个类中的方法，在post方法中可通过反射来回调它。
2、我们可以在内存中维护一个变量，用来存储注册了EventBus的对象及其类信息、需要回调的方法及回调方式。（由于注册与解绑是以对象为单位，所以内存中的数据要以**对象-其它数据**的格式来存储。方便解绑。）
3、鉴于EventBus使用场景无需多个EventBus对象，故使用单例模式来设计该架构。

整体思路：EventBus的register方法用来向内存中保存注册者的 **实例-(类信息、回调方式、回调方法)** 映射信息。post方法中接收到需要发送的消息后，遍历内存中存储的所有方法，若方法形参与消息类型一致，则通过反射进行回调。而unRegister方法，则仅需将内存中对应实例的方法信息清除掉即可。
# 实现
开始实现，首先，我们定义一个注解，及其值定义：

```java
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
```

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventReceiver {
    int value() default ThreadMode.POSTING;
}
```
上述自定义注解中，`@Retention(RetentionPolicy.RUNTIME)`规定了注解在运行时保留，`@Target(ElementType.METHOD)`规定了注解只能用在方法上。关于自定义注解的更多细节，不了解的可以先学习学习。

接下来我们来定义内存中那个存储的方法数据结构。
首先，我们需要一个对象-其它信息的结构，最合适的就是一个Map。故结构可定义为：
`private Map<Object, List<MessageManager>> map;`
其次，我们我们通过反射调用一个方法，需要知道方法的类信息、方法信息以及自定义注解中方法运行的线程。在此我们定义一个JavaBean，名为MessageBean：

```java
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
```
接下来，我们要开始写我们的EventBus核心类了。
EventBus类为单例类，其中包含了`getInstance（）`、`register(Object context)`、`unRegister(Object context)`、`post(Object message)`四个对外方法：

```java
public class EventBus {

    private Map<Object, List<MessageBean>> map;
    ···

    public synchronized static EventBus getInstance() {
        ···
    }

    private MyEventBus() {
        map = new HashMap<>();
        ···
    }

    /**
     * 注册
     * @param context 需要注册的对象
     */
    public void register(Object context) {
        ···
    }

    /**
     * 解绑
     * @param context 需要解绑的对象
     */
    public void unRegister(Object context) {
        ···
    }

    /**
     * 消息发送
     * @param message 需要发送的消息
     */
    public void post(Object message) {
        ···
    }
}
```
首先我们来实现`getInstance（）`方法获取单例对象：

```java
	private static EventBus instance;
	
    public synchronized static EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }
```
接下来，我们来实现`register(Object context)`方法。该方法需要取到Object 中有目标注解的方法及相关信息，并保存至map中：

```java
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
```
上述代码中额外判断了方法的形参数量及返回值类型。我们规定形参只能有一个，且返回值必须为void。这样可以避免用户滥用注解后，导致的程序崩溃。

相比于注册方法，解绑方法`unRegister(Object context)`则显得简单许多，仅需移除map中对象对应的数据即可：

```java
    /**
     * 解绑
     *
     * @param context 需要解绑的对象
     */
    public void unRegister(Object context) {
        map.remove(context);
    }
```
最后就是消息发送`post(Object message)`了。这个方法需要遍历所有内存中的方法，并在通过反射回调：

```java
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
```
在这里我们分离出了一个`invoke`方法。该方法需要在消息类型与回调方法参数类型一致的情况下进行回调。并且回调时进行执行线程的选择：

```java
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
```
可以看到，当不需要切换线程时，我们直接反射回调即可。
需要在主线程回调时，我们在主线程Handler中进行回调。Handler在构造方法中定义：

```java
    private Handler handler;

    private MyEventBus() {
    	···
        handler = new Handler(Looper.getMainLooper());
    }
```
而在子线程中执行的时候，我们使用了线程池：

```java
    private ExecutorService executorService;

    private MyEventBus() {
    	···
        executorService = Executors.newCachedThreadPool();
    }
```

至此，完工。数一下代码行数，也就百来行。
# 总结
当前手撸的EventBus只是一个简易版。真正的EventBus内部进行了大量的兼容性代码，以及粘性事件等多种消息功能。
手撸框架可以让人理解知名架构设计者的架构思想，也可以让自己的思维能力及架构能力更加清晰。成长的路还很长，共勉。
手撸EventBus源码地址：
