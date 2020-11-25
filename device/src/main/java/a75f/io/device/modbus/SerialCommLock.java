package a75f.io.device.modbus;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SerialCommLock {
    
    private Lock      lock        = new ReentrantLock();
    private Condition condition = lock.newCondition();
    
    AtomicBoolean readPending = new AtomicBoolean(false);
    
    int registerNumber = 0;
    public void lock(long timeoutSeconds, int registerNum) {
        lock.lock();
        try{
            registerNumber = registerNum;
            readPending.set(true);
            while (readPending.get()) {
                condition.await(timeoutSeconds, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        } finally{
            lock.unlock();
        }
    
    }
    
    public void unlock() {
        readPending.set(false);
        //condition.signal();
    }
    
    public int getRegisterNumber() {
        return registerNumber;
    }
    
}
