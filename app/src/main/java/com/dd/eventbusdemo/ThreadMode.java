package com.dd.eventbusdemo;

public enum ThreadMode {

    /**
     * Subscriber will be called in the same thread, which is posting the event. This is the default. Event delivery
     * implies the least overhead because it avoids thread switching completely. Thus this is the recommended mode for
     * simple tasks that are known to complete is a very short time without requiring the main thread. Event handlers
     * using this mode must return quickly to avoid blocking the posting thread, which may be the main thread.
     */
    //如果在子线程发送，那就在子线程中执行。在哪个线程中发送事件，就在哪个线程中执行
    POSTING,

    /**
     * Subscriber will be called in Android's main thread (sometimes referred to as UI thread). If the posting thread is
     * the main thread, event handler methods will be called directly. Event handlers using this mode must return
     * quickly to avoid blocking the main thread.
     */
    //在主线程中执行
    MAIN,

    /**
     * Subscriber will be called in a background thread. If posting thread is not the main thread, event handler methods
     * will be called directly in the posting thread. If the posting thread is the main thread, EventBus uses a single
     * background thread, that will deliver all its events sequentially. Event handlers using this mode should try to
     * return quickly to avoid blocking the background thread.
     */
    //在子线程中执行。如果发送在主线程中，那么调用线程池中的子线程来执行，否则直接在子线程中执行
    BACKGROUND,

    /**
     * Event handler methods are called in a separate thread. This is always independent from the posting thread and the
     * main thread. Posting events never wait for event handler methods using this mode. Event handler methods should
     * use this mode if their execution might take some time, e.g. for network access. Avoid triggering a large number
     * of long running asynchronous handler methods at the same time to limit the number of concurrent threads. EventBus
     * uses a thread pool to efficiently reuse threads from completed asynchronous event handler notifications.
     */

    //异步线程：无论发布事件执行在主线程还是在子线程中。都利用一个异步线程来执行订阅方法
    ASYNC

}
