package a75f.io.logic.util;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RxTask {
    
    public static void executeAsync(Runnable function) {
        Observable.fromCallable(() -> {
            function.run();
            return true;
        })
                  .subscribeOn(Schedulers.io())
                  .subscribe();
    }
}
