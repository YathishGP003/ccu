package a75f.io.logic.bo.building.bpos;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

public class BPOSConfiguration  extends BaseProfileConfiguration {

    private double temperatureOffset;
    private int zonePriority;
    private boolean autoforceOccupied;
    private boolean autoAway;


    public double gettempOffset(){
        return temperatureOffset;
    }
    public void settempOffset(double tempoff){
        temperatureOffset = tempoff;
    }



    public int getzonePriority(){
        return zonePriority;
    }
    public void setzonePriority(int priority){
        zonePriority = priority;
    }



    public boolean getautoforceOccupied(){
        return autoforceOccupied;
    }
    public void setautoforceOccupied(boolean autoforce){
        autoforceOccupied = autoforce;
    }


    public boolean getautoAway(){
        return autoAway;
    }
    public void setautoAway(boolean autoaway){
        autoAway = autoaway;
    }

}

