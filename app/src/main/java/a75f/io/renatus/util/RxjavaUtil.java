package a75f.io.renatus.util;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RxjavaUtil {
    
    /**
     * Simple background execution using rxjava threadpool.
     * @param function
     */
    public static void executeBackground(Runnable function) {
        Observable.fromCallable(() -> {
                        function.run();
                        return true;
                    })
                  .subscribeOn(Schedulers.io())
                  .subscribe();
    }
    
    /**
     * RxJava based alternative to async task.
     * @param preExecuteFunction  - Executed on main/host thread
     * @param backGroundFunction  - Executed using rx thread pool
     * @param postExecute  - Executed on main thread.
     */
    public static void executeBackgroundTask(Runnable preExecuteFunction , Runnable backGroundFunction,
                                                     Runnable postExecute) {
        
        preExecuteFunction.run();
        
        Observable.fromCallable(() -> {
                        backGroundFunction.run();
                        return true;
                    })
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .doOnComplete(()-> {
                     postExecute.run();
                  })
                  .subscribe();
    }
    
}
