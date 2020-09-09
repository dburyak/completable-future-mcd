package async;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;

public class MdcWrapperHelper {
    public static <T> Callable<T> wrapWithMdcContext(Callable<T> task) {
        //save the current MDC context
        var ctx = MDC.getCopyOfContextMap();
        return () -> {
            setMDCContext(ctx);
            try {
                return task.call();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
            }
        };
    }

    public static Runnable wrapWithMdcContext(Runnable task) {
        //save the current MDC context
        var ctx = MDC.getCopyOfContextMap();
        return () -> {
            setMDCContext(ctx);
            try {
                task.run();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
            }
        };
    }

    public static <T> ForkJoinTask<T> wrapWithMdcContext(ForkJoinTask<T> task) {
        var ctx = MDC.getCopyOfContextMap();
        // TODO: not clear how to implement this, and even not sure whether it makes sense at all since ForkJoin task
        //  is also a Future. Just throwing exception here to empirically identify if this is used at all.
//        new ForkJoinTask.AdaptedCallable<T>(() -> {
//            setMDCContext(ctx)
//            throw new UnsupportedOperationException('not implemented')
//        })
        throw new UnsupportedOperationException("not implemented");
    }

    static void setMDCContext(Map<String, String> contextMap) {
        MDC.clear();
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }
}
