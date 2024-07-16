package a75f.io.api.haystack.sync;


import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;

import java.util.ArrayList;

public class HGridIterator {
    private final HGrid mGrid;
    private int   index = 0;
    
    public HGridIterator(HGrid grid) {
        mGrid = grid;
    }
    
    public boolean hasNext()
    {
        return index < mGrid.numRows();
    }
    
    public HGrid next(int limit)
    {
        ArrayList<HDict> dictList = new ArrayList<>();
        int dictIndex = 0;
        
        while (hasNext() && dictIndex < limit) {
            dictList.add(next());
            dictIndex++;
        }
        if (dictIndex > 0) {
            return HGridBuilder.dictsToGrid(dictList.toArray(new HDict[0]));
        } else {
            return null;
        }
    }
    
    public HDict next() {
        if (hasNext())
            return mGrid.row(index++);
        else
            return null;
    }
}
