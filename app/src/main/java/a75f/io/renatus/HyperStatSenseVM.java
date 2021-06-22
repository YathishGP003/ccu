package a75f.io.renatus;

import android.widget.Spinner;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import a75f.io.logic.L;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class HyperStatSenseVM extends ViewModel {
    //TODO
    MutableLiveData<Boolean> mVm;

    public void init(){
        mVm = new MutableLiveData<Boolean>();
    }

    public LiveData<Boolean> get(){
        return mVm;
    }
}
