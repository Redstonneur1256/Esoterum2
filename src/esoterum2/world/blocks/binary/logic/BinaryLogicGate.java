package esoterum2.world.blocks.binary.logic;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import esoterum2.util.IntBoolf;
import esoterum2.world.blocks.binary.*;
import mindustry.gen.*;

public class BinaryLogicGate extends BinaryBlock{

    //accepts Integer instead of boolean[] to prevent this hypothetical scenario
    //i[0] && !i[1]
    //input order shouldn't matter
    public IntBoolf operation;
    public boolean single;
    //Logic gates are the only block that can create an infinite on-off loop
    //So I implement this only for them
    public int visitLimit;

    public BinaryLogicGate(String name){
        super(name);
        outputs = new boolean[]{true, false, false, false};
        //in reality, shouldn't be used
        inputs = new boolean[]{false, true, true, true};
        single = false;
        decalType = "silver";
        configurable = saveConfig = true;
        config(Integer.class, (BinaryLogicGateBuild b, Integer i) -> {
            b.config = ((i + 3) % 3);
            b.updateProximity();
        });
        operation = i -> false;
        visitLimit = 5;
    }

    public class BinaryLogicGateBuild extends BinaryBuild{
        public int config = 0;
        public int remVisits;

        @Override
        public void updateTile(){
            super.updateTile();
            remVisits = visitLimit;
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.rotate, () -> {
                configure(config - 1);
                updateProximity();
                updateSignal();
            }).size(40f).tooltip("Rotate Input" + (single ? "" : "s"));
        }

        @Override
        public Object config(){
            return config;
        }

        @Override
        public void updateSignal(){
            boolean temp = signal;
            int count = 0;
            for(int i = 1; i < 4; i++){
                if(inputValid(i) && BinaryBlock.signal(multiB(i), this)){
                    count++;
                }
            }
            signal = operation.get(count);
            if(temp != signal){
                propagateSignal();
            }
        }

        @Override
        public void propagateSignal(){
            if(remVisits != 0){
                remVisits--;
                super.propagateSignal();
            }else{
                shouldPropagate = true;
            }
        }

        @Override
        protected void drawConnections(){
            for(int i = 0; i < 4; i++){
                if(inputValid(i) || outputValid(i)){
                    Draw.color((((multiB(i) instanceof BinaryBuild b) &&
                    (inputValid(i) && b.signal(this))) ||
                    signal(i))
                    ? team.color : Color.white);
                    Draw.rect(connectionRegion, x, y, (rotation + i) % 4 * 90);
                }
            }
            Draw.color();
        }

        @Override
        public boolean inputValid(int dir){
            return dir == config + 1 || (!single && dir == ((config + 1) % 3) + 1);
        }

        @Override
        public byte version(){
            return (byte)(super.version() + 1);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision == version() - 1){
                config = read.b();
            }else{
                config = read.i();
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(config);
        }
    }
}
