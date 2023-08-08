package a75f.io.renatus;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HyperStatSenseVM extends ViewModel {
    //TODO
    MutableLiveData<HyperStatMonitoringModel> mVm;
    HyperStatMonitoringModel model;

    public void init(){
        if(mVm != null){
            return;
        }
        mVm = new MutableLiveData<HyperStatMonitoringModel>();
        model = new HyperStatMonitoringModel();
        mVm.setValue(model);
    }

    public LiveData<HyperStatMonitoringModel> get(){
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
