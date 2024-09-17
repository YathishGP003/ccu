package a75f.io.logic.util;

public interface onLoadingCompleteListener {
        onLoadingCompleteListener INSTANCE = new onLoadingCompleteListener() {
            @Override
            public void onLoadingComplete() {
            }
        };
        void onLoadingComplete();
}
