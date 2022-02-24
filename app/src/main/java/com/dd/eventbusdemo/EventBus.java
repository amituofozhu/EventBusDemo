package com.dd.eventbusdemo;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBus {


    static volatile EventBus defaultInstance;


    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    private final Map<Object, List<Class<?>>> typesBySubscriber;


    public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }


    private EventBus() {
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
    }


    public void register(Object object) {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        //1 解析所有方法封装成SubscriberMethod的集合
        Class<?> objClass = object.getClass();
        //1.1获取方法
        Method[] methods = objClass.getDeclaredMethods();
        //1.2遍历所有的方法
        for (Method method : methods) {
            //1.3 判断是不是public类型的方法，因为反射不是public类型的可能获取不到
            if (Modifier.PUBLIC != 0) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                //1.4 获取方法中的参数，如果有带两个参数的抛出异常
                if(parameterTypes.length==1){
                    //1.5 获取到带有Subscribe的方法
                    Subscribe subscribe = method.getAnnotation(Subscribe.class);
                    if(subscribe!=null){
                        //1.6 封装到SubscriberMethod对象中
                        SubscriberMethod subscriberMethod =
                                new SubscriberMethod(method,parameterTypes[0],subscribe.threadMode(),subscribe.priority(),subscribe.sticky());
                        subscriberMethods.add(subscriberMethod);
                    }

                }

            }


        }

        //2 存放到subscriptionsByEventType中
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
            Class<?> eventType = subscriberMethod.eventType;
            Subscription newSubscription = new Subscription(object, subscriberMethod);
            CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
            if (subscriptions == null) {
                subscriptions = new CopyOnWriteArrayList<>();
                subscriptionsByEventType.put(eventType, subscriptions);
            } else {
                if (subscriptions.contains(newSubscription)) {
                    throw new EventBusException("Subscriber " + object.getClass() + " already registered to event "
                            + eventType);
                }
            }

            //判断优先级
            int size = subscriptions.size();
            for (int i = 0; i <= size; i++) {
                if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                    subscriptions.add(i, newSubscription);
                    break;
                }
            }

            List<Class<?>> subscribedEvents = typesBySubscriber.get(object);
            if (subscribedEvents == null) {
                subscribedEvents = new ArrayList<>();
                typesBySubscriber.put(object, subscribedEvents);
            }
            subscribedEvents.add(eventType);

        }

    }


    //执行
    public void post(Object object) {
        // 这里先获取当前线程的状态 postingState是放在了ThreadLocal中去的
        // 如果是false则设置为主线程

        //1 遍历所有subscriptionsByEventType的数据，找到对应的方法执行invoke()方法
        Class<?> eventClass = object.getClass();
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventClass);
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                //2 找到符合的执行方法
                postToSubscription(subscription,object);
            }
        }


    }



    private void postToSubscription(Subscription subscription,Object event) {

        ThreadMode threadMode = subscription.subscriberMethod.threadMode;
        boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
        AsyncPoster poster = new AsyncPoster(subscription,event);

        switch (threadMode){
            case POSTING:
                invokeMethod(subscription,event);
                break;

            case MAIN:
                //判断是不是在主线程中
                if(isMainThread){
                    invokeMethod(subscription,event);
                }else {
                    //切换到主线程中
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            invokeMethod(subscription,event);
                        }
                    });

                }

                break;

            case ASYNC:

            case BACKGROUND:

                if(!isMainThread){
                    invokeMethod(subscription,event);
                }else {
                    poster.enqueue(subscription,event);
                }

                break;


            default:
                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);

        }


    }


    public void invokeMethod(Subscription subscription, Object event) {

        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    //移除
    public void unregister(Object object) {
        List<Class<?>> subscribedTypes = typesBySubscriber.get(object);
        if (subscribedTypes != null) {
            for (Class<?> eventType : subscribedTypes) {
                //subscriptionsByEventType 中的数据也要移除
                removeObject(object, eventType);
            }
            typesBySubscriber.remove(object);
        }
    }


    private void removeObject(Object object, Class<?> eventType) {

        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == object) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }

    }





}
