package com.dd.eventbusdemo;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncPoster  implements Runnable{

    private  Subscription subscription;
    private Object  object;

    private final ExecutorService executorService= Executors.newCachedThreadPool();


    AsyncPoster(Subscription subscription,Object object) {
        this.subscription =subscription;
        this.object =object;
    }

    public void enqueue(Subscription subscription, Object event) {
        AsyncPoster  poster = new AsyncPoster(subscription,event);
        //用线程池执行
        executorService.execute(poster);
    }


    @Override
    public void run() {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }


}
