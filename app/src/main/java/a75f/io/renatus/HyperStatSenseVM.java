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
        if(mVm != null){
            return;
        }
        mVm = new MutableLiveData<HyperStatSenseModel>();
        model = new HyperStatSenseModel();
        mVm.setValue(model);
    }

    public LiveData<HyperStatSenseModel> get(){
        return mVm;
    }

    public String  getTempOffset(){
        return model.tempOffset;
    }

    public void setTempoffset(String temp){ model.tempOffset = temp;}

    public boolean getth1toggle() {
        return model.th1toggle;
    }

    public void setth1toggle(boolean val) {
        model.th1toggle = val;
    }

    public boolean getth2toggle() {
        return model.th2toggle;
    }

    public void setth2toggle(boolean val) {
        model.th2toggle = val;
    }

    public boolean getanlg1toggle() {
        return model.anlg1toggle;
    }

    public void setanlg1toggle(boolean val) {
        model.anlg1toggle = val;
    }

    public int getth1SpVal() {
        return model.th1Sp;
    }

    public void setth1SpVal(int val) {
        model.th1Sp = val;
    }

    public int getth2SpVal() {
        return model.th2Sp;
    }

    public void setth2SpVal(int val) {
        model.th1Sp = val;
    }

    public int getanlg1SpVal() {
        return model.anlg1Sp;
    }

    public void setanlg1SpVal(int val) {
        model.th1Sp = val;
    }

    public int getanlg2SpVal() {
        return model.anlg2Sp;
    }

    public void setanlg2SpVal(int val) {
        model.th1Sp = val;
    }




}
