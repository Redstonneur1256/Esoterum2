package esoterum2.caller;

import esoterum.world.blocks.binary.BinaryBlock.*;
import mindustry.gen.*;

public class EsoterumCaller implements EsoCaller{

    private boolean[] empty = new boolean[6];

    @Override
    public boolean[] getSignal(Building building){
        if(building instanceof BinaryBuild build){
            return build.signal;
        }
        return empty;
    }

    @Override
    public boolean isBuild(Building building){
        return building instanceof BinaryBuild;
    }

}
