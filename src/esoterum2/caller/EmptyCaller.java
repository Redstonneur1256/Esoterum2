package esoterum2.caller;

import mindustry.gen.*;

public class EmptyCaller implements EsoCaller{

    private boolean[] empty = new boolean[6];

    @Override
    public boolean[] getSignal(Building building){
        return empty;
    }

    @Override
    public boolean isBuild(Building building){
        return false;
    }

}
