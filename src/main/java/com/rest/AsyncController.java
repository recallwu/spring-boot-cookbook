package com.rest;

import com.api.LongTermTask;
import com.service.LongTimeAsyncCallService;
import com.util.RequestHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


/**
 * Created by MyWorld on 2016/12/5.
 */
@RestController
public class AsyncController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncController.class);

    @Autowired
    private LongTimeAsyncCallService longTimeAsyncCallService;

    /**
     * 19:47:06.248 DEBUG [http-nio-9999-exec-1][org.springframework.web.servlet.DispatcherServlet] Last-Modified value for [/deferred] is: -1
     * 19:47:06.254 INFO  [http-nio-9999-exec-1][com.rest.AsyncController] /deferred 调用！thread id is:Thread[http-nio-9999-exec-1,5,main]
     * 19:47:06.255 INFO  [http-nio-9999-exec-1][com.service.LongTimeAsyncCallService] 完成此任务需要:3秒,Thread[http-nio-9999-exec-1,5,main]
     * 19:47:06.260 DEBUG [http-nio-9999-exec-1][org.springframework.web.context.request.async.WebAsyncManager] Concurrent handling starting for GET [/deferred]
     * 19:47:06.261 DEBUG [http-nio-9999-exec-1][org.springframework.web.servlet.DispatcherServlet] Leaving response open for concurrent processing
     * 19:47:06.262 DEBUG [http-nio-9999-exec-1][org.springframework.boot.web.filter.OrderedRequestContextFilter] Cleared thread-bound request context: org.apache.catalina.connector.RequestFacade@7a582546
     * //此处的停顿，是因为LongTimeAsyncCallService有sleep 3 s的操作。因为2s就超时了，另外1s就没有等
     * 19:47:08.721 INFO  [http-nio-9999-exec-2][com.rest.AsyncController] 异步调用执行超时！thread id is:Thread[http-nio-9999-exec-2,5,main]
     * 19:47:08.722 DEBUG [http-nio-9999-exec-2][org.springframework.web.context.request.async.WebAsyncManager] Concurrent result value [异步调用执行超时] - dispatching request to resume processing
     * 19:47:08.732 DEBUG [http-nio-9999-exec-2][org.springframework.boot.web.filter.OrderedRequestContextFilter] Bound request context to thread: com.global.filter.URLRewriteRequestWrapper@1c1a0b83
     * 19:47:08.733 INFO  [http-nio-9999-exec-2][com.global.filter.AddExtraToParamsFilter] com.global.filter.AddExtraToParamsFilter
     * 19:47:08.733 INFO  [http-nio-9999-exec-2][com.global.filter.RewriteServletPathFilter] com.global.filter.RewriteServletPathFilter
     * 19:47:08.733 DEBUG [http-nio-9999-exec-2][org.springframework.web.servlet.DispatcherServlet] DispatcherServlet with name 'dispatcherServlet' resumed processing GET request for [/deferred]
     * 19:47:08.734 DEBUG [http-nio-9999-exec-2][org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping] Looking up handler method for path /deferred
     * 19:47:08.734 DEBUG [http-nio-9999-exec-2][org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping] Returning handler method [public org.springframework.web.context.request.async.DeferredResult<java.lang.String> com.rest.AsyncController.asyncTask()]
     * 19:47:08.734 DEBUG [http-nio-9999-exec-2][org.springframework.beans.factory.support.DefaultListableBeanFactory] Returning cached instance of singleton bean 'asyncController'
     * 19:47:08.734 DEBUG [http-nio-9999-exec-2][org.springframework.web.servlet.DispatcherServlet] Last-Modified value for [/deferred] is: -1
     * 19:47:08.735 DEBUG [http-nio-9999-exec-2][org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter] Found concurrent result value [异步调用执行超时]
     * 19:47:08.763 DEBUG [http-nio-9999-exec-2][org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor] Written [异步调用执行超时] as "text/plain" using [org.springframework.http.converter.StringHttpMessageConverter@57eea9]
     * 19:47:08.764 DEBUG [http-nio-9999-exec-2][org.springframework.web.servlet.DispatcherServlet] Null ModelAndView returned to DispatcherServlet with name 'dispatcherServlet': assuming HandlerAdapter completed request handling
     * 19:47:08.764 DEBUG [http-nio-9999-exec-2][org.springframework.web.servlet.DispatcherServlet] Successfully completed request
     * 19:47:08.764 DEBUG [http-nio-9999-exec-2][org.springframework.boot.web.filter.OrderedRequestContextFilter] Cleared thread-bound request context: com.global.filter.URLRewriteRequestWrapper@1c1a0b83
     * 19:47:09.256 INFO  [pool-1-thread-1][com.rest.AsyncController] 异步调用执行完成, thread id is:Thread[pool-1-thread-1,5,main]
     *
     * @return 异步调用执行超时
     */
    @RequestMapping(value = "/deferred", method = RequestMethod.GET)
    public DeferredResult<String> asyncTask() {
        DeferredResult<String> deferredResult = new DeferredResult<>(2000L);//超时时间设置为2S
        LOGGER.info("{} 调用！thread id is:{}", RequestHolder.getRequestFacade().getRequestURI(), Thread.currentThread());
        longTimeAsyncCallService.makeRemoteCallAndUnknownWhenFinish(new LongTermTask() {
            @Override
            public void callback(Object result) {
                LOGGER.info("异步调用执行完成, thread id is:{}", Thread.currentThread());
                deferredResult.setResult(result.toString());
            }
        });

        deferredResult.onTimeout(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("异步调用执行超时！thread id is:{}", Thread.currentThread());
                deferredResult.setResult("异步调用执行超时");
            }
        });

        return deferredResult;
    }

    @RequestMapping(value = "/webAsyncTask", method = RequestMethod.GET)
    public WebAsyncTask longTimeTask() {
        LOGGER.info("{} 被调用 thread id is:{} ", RequestHolder.getRequestFacade().getRequestURI(), Thread.currentThread());
        Callable<String> callable = () -> {
            TimeUnit.SECONDS.sleep(3);//假设是一些长时间任务
            LOGGER.info("执行成功 thread id is:{}", Thread.currentThread());
            return "执行成功";
        };

        WebAsyncTask<String> asyncTask = new WebAsyncTask<>(2000, callable);//时间 的单位是ms
        /**
         * 超时了，“执行成功”这句话还会打印？
         * 超时归超时，超时并不会打断正常执行流程，
         * 但注意，出现超时后我们给客户端返回了“超时”的结果，那接下来即便正常处理流程成功，
         * 客户端也收不到正常处理成功所产生的结果了，这带来的问题就是：客户端看到了“超时”，
         * 实际上操作到底有没有成功，客户端并不知道，
         * 但通常这也不是什么大问题，因为用户在浏览器上再刷新一下就好了
         */
        asyncTask.onTimeout(
                () -> {
                  /*
                   ModelAndView mav = new ModelAndView("longtimetask");
                    mav.addObject("result", "执行超时");
                  */
                    LOGGER.info("执行超时 thread id is:{}", Thread.currentThread());
                    return "执行超时";
                }
        );
        /**
         * 直接返回Callable<ModelAndView>都是可以的，但我们这里包装了一层，是为了上面的“超时处理”
         * Callable的call方法并不是我们直接调用的，而是在longTimeTask返回后，
         * 由Spring MVC用一个工作线程来调用
         * 即仅仅是简单地把请求处理线程的任务转交给另一工作线程而已。
         * 其实“超时处理线程”和“回调处理线程”可能都是线程池中的某个线程
         */
        return new WebAsyncTask<>(3000, callable);//时间 的单位是ms
    }


}