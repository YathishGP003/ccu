package a75f.io.device.modbus;

import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.modbus.Register;
import a75f.io.logic.L;

public class SerialCommLock {
    
    private static final int WAIT_TIME_MS = 10;
    
    private Lock      lock        = new ReentrantLock();
    private Condition condition = lock.newCondition();
    
    AtomicBoolean readPending = new AtomicBoolean(false);
    
    Register register;
    public void lock(Register register, long timeoutMS) {
        Log.d(L.TAG_CCU_MODBUS, "SerialComm lock :"+register.getRegisterAddress());
        lock.lock();
        try{
            this.register = register;
            readPending.set(true);
            long waitMillis = timeoutMS;
            while (readPending.get() && waitMillis > 0) {
                condition.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
                waitMillis -= WAIT_TIME_MS;
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        } finally{
            if (readPending.get()) {
                Log.d(L.TAG_CCU_MODBUS, " SerialComm Timeout :"+register.getRegisterAddress());
                ModbusWatchdog.getInstance().reportTimeout();
            }
            lock.unlock();
        }
        
    }
    
    public void unlock() {
        Log.d(L.TAG_CCU_MODBUS, " SerialCommLock unlock :"+register.getRegisterAddress());
        readPending.set(false);
        ModbusWatchdog.getInstance().pet();
    }
    
    public Register getRegister() {
        return register;
    }
    
}

