package a75f.io.renatus;

import android.widget.Spinner;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import a75f.io.logic.L;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class HyperStatSenseVM extends ViewModel {
    //TODO
    MutableLiveData<HyperStatSenseModel> mVm;
    HyperStatSenseModel model;

    public void init(){
        mVm = new MutableLiveData<HyperStatSenseModel>();
        model = new HyperStatSenseModel();
    }

    public LiveData<HyperStatSenseModel> get(){
        return mVm;
    }

    public void saveConfig(){

    }




}
