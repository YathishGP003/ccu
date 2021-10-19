package a75f.io.logic.util;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RxjavaUtil {

    /**
     * Simple background execution using rxjava threadpool.
     *
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

    public static Disposable executeBackgroundWithDisposable(Runnable function) {
        return Completable.fromCallable(() -> {
            function.run();
            return true;
        })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * RxJava based alternative to async task. (Risky !- Does not handle Disposable)
     *
     * @param preExecuteFunction - Executed on main/host thread
     * @param backGroundFunction - Executed using rx thread pool
     * @param postExecute        - Executed on main thread.
     */
    public static void executeBackgroundTask(Runnable preExecuteFunction, Runnable backGroundFunction,
                                             Runnable postExecute) {

        preExecuteFunction.run();

        Completable.fromCallable(() -> {
            backGroundFunction.run();
            return true;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> postExecute.run())
                .subscribe(
                        () -> {
                        },
                        e -> CcuLog.e(L.TAG_CCU, "Background task failed : ")
                );
    }

    /**
     * RxJava based alternative to async task.
     *
     * @param preExecuteFunction - Executed on main/host thread
     * @param backGroundFunction - Executed using rx thread pool
     * @param postExecute        - Executed on main thread.
     */
    public static Disposable executeBackgroundTaskWithDisposable(Runnable preExecuteFunction,
                                                                 Runnable backGroundFunction,
                                                                 Runnable postExecute) {

        preExecuteFunction.run();

        return Completable.fromCallable(() -> {
            backGroundFunction.run();
            return true;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> postExecute.run())
                .subscribe(
                        () -> {
                        },
                        e -> CcuLog.e(L.TAG_CCU, "Background task failed : ")
                );
    }
}
