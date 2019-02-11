package a75f.io.logic.jobs;



import a75f.io.logic.BaseJob;


public class PrintProcessJobTwo extends BaseJob {


    @Override
    public void doJob() {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Printing Two Time: working");
    }
}
