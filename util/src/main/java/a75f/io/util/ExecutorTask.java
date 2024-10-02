package a75f.io.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutorTask {
    private static final int MAXIMUM_POOL_SIZE = 8;
    private static final ExecutorService service = Executors.newFixedThreadPool(MAXIMUM_POOL_SIZE);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static Future executeBackground(Runnable runnable) {
        return service.submit(runnable);
    }

    /**
     * Execute a task in background and post the result on main thread.
     * @param preExecute - runs on the calling thread
     * @param background - runs on background thread
     * @param postExecute - runs on main thread
     */
    public static Future executeAsync(Runnable preExecute, Runnable background, Runnable postExecute) {
        preExecute.run();
        return service.submit(() -> {
            background.run();
            mainHandler.post(postExecute);
        });
    }

    /**
     * Execute a task in background and post the result on main thread.
     * @param background
     * @param postExecute
     * @return
     */
    public static Future executeAsync(Runnable background, Runnable postExecute) {
        return service.submit(() -> {
            background.run();
            mainHandler.post(postExecute);
        });
    }
}
