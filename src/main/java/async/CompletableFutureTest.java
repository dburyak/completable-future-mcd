package async;

import org.apache.logging.log4j.core.util.ExecutorServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class CompletableFutureTest {
    private static final Logger log = LoggerFactory.getLogger(CompletableFutureTest.class);

    static {
        try {
            setFinalStatic(CompletableFuture.class.getDeclaredField("ASYNC_POOL"), new MdcAwareForkJoinPool());
        } catch (Exception e) {
            throw new RuntimeException("failed to configure MDC aware thread pool for CompletableFuture", e);
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        log.info("starting");
        var numConcurrentClients = 10;

        var exec = Executors.newFixedThreadPool(10);

        var tasks = new ArrayList<CompletableFuture<?>>();
        IntStream.range(0, numConcurrentClients).forEach(step -> {
            var txid = "tx-" + step;
            var fut = CompletableFuture
                    .supplyAsync(() -> {
                        MDC.put("txid", txid);
                        log.info("stage 1 : {}", txid);
                        return "stage 1 : " + txid;
                    })
                    .thenApply(s -> {
                        log.info("stage 2 : {}", txid);
                        return "stage 2 : " + txid;
                    })
                    .thenCompose(s -> {
                        // a pretty reasonable use of CF
                        // (e.g. could happen in an external library or framework)
                        // breaks the transaction logging for future steps:
                        var rv = new CompletableFuture<String>();
                        exec.submit(() -> {
                            log.info("stage 3 : {}", txid);
                            rv.complete("stage 3 : " + txid);
                        });
                        return rv;
                    })
                    .thenCompose(s -> CompletableFuture.supplyAsync(() -> {
                        log.info("stage 4 : {}", txid);
                        return "stage 4 : " + txid;
                    }))
                    .handle((s, err) -> {
                        log.info("stage 5 : {}", txid);
                        return "stage 5 : " + txid;
                    })
                    .thenAccept(s -> {
                        log.info("stage 6 : {}", txid);
                    });
            tasks.add(fut);
        });

        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).get();

        log.info("done");
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        var modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}
