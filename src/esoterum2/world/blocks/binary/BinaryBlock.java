package esoterum2.world.blocks.binary;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.io.*;
import esoterum2.*;
import esoterum2.caller.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class BinaryBlock extends Block{

    public static final EsoCaller caller = Vars.content.getByName(ContentType.block, "esoterum-wire") != null ? new EsoterumCaller() : new EmptyCaller();

    public boolean[] outputs;
    public boolean[] inputs;
    public boolean largeConnections;
    public boolean rotateHighlight;
    public TextureRegion baseRegion, highlightRegion, connectionRegion;
    public boolean useOnOffHighlights;
    public TextureRegion onHighlight, offHighlight;
    public String decalType;
    public TextureRegion decalRegion;
    public TextureRegion[] decalRegions;

    public BinaryBlock(String name){
        super(name);
        rotate = true;
        update = true;
        solid = true;
        destructible = true;
        hideDetails = false;
        buildVisibility = BuildVisibility.shown;
        category = Category.logic;
        rotateHighlight = true;
        decalType = "";
        useOnOffHighlights = false;
    }

    @Override
    public void load(){
        super.load();
        baseRegion = Core.atlas.find("esoterum-duck-base");
        if(useOnOffHighlights){
            onHighlight = Core.atlas.find(name + "-on");
            //setting highlightRegion to offHightlight allows for max lazy
            highlightRegion = offHighlight = Core.atlas.find(name + "-off");
        }else{
            highlightRegion = Core.atlas.find(name + "-highlight");
        }
        connectionRegion = Core.atlas.find("esoterum-duck-connection" + (largeConnections ? "-large" : ""));
        if(!decalType.equals("")){
            decalRegion = Core.atlas.find("esoterum-duck-decal-" + decalType);
            decalRegions = new TextureRegion[4];
            for(int i = 0; i < 4; i++){
                decalRegions[i] = Core.atlas.find("esoterum-duck-decal-" + decalType + "-" + i);
            }
        }
    }

    @Override
    protected TextureRegion[] icons(){
        return decalRegion == null ? new TextureRegion[]{
        baseRegion, highlightRegion
        } : new TextureRegion[]{
        baseRegion, rotate ? decalRegions[0] : decalRegion, highlightRegion
        };
    }

    @Override
    public boolean canReplace(Block other){
        if(other.alwaysReplace) return true;
        return (other != this || rotate) && other instanceof BinaryBlock && size == other.size;
    }

    public static boolean signal(Building from, BinaryBuild to){
        if(from instanceof BinaryBuild b){
            return b.signal(to);
        }
        return caller.getSignal(from)[Utils.relativeDir(from, to)];
    }

    public class BinaryBuild extends Building{
        public boolean signal;
        public boolean shouldPropagate;
        public boolean permaUpdate;
        public boolean[] connections;

        @Override
        public void created(){
            super.created();
            if(!rotate) rotation(0);
            connections = new boolean[4];
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            updateConnections();
            updateSignal();
        }

        @Override
        public void updateTile(){
            if(permaUpdate || shouldPropagate){
                updateSignal();
                propagateSignal();
            }
        }

        public void updateConnections(){
            permaUpdate = false;
            for(int i = 0; i < 4; i++){
                connections[i] = multiB(i) instanceof BinaryBuild b &&
                ((b.inputValid(Utils.relativeDir(b, this)) && outputValid(i)) ||
                b.outputValid(Utils.relativeDir(b, this)) && inputValid(i));
                permaUpdate |= caller.isBuild(multiB(i));
            }
        }

        //generic signal propagation that works for pretty much everything
        public void propagateSignal(){
            shouldPropagate = false;
            for(int i = 0; i < 4; i++){
                if(outputValid(i) && multiB(i) instanceof BinaryBuild b && connections[i]){
                    try{
                        b.updateSignal();
                    }catch(StackOverflowError e){
                        //try it on next frame
                        shouldPropagate = true;
                        b.shouldPropagate = true;
                    }
                }
            }
        }

        //implementation generally left to the block
        //in general, it should call propagateSignal() if the state changed
        public void updateSignal(){
            boolean temp = signal;
            signal = false;
            for(int i = 0; i < 4; i++){
                if(inputValid(i)){
                    signal |= BinaryBlock.signal(multiB(i), this);
                }
            }
            if(temp != signal){
                propagateSignal();
            }
        }

        public boolean signal(int dir){
            return signal && outputValid(dir);
        }

        public boolean signal(Building b){
            return this.signal(Utils.relativeDir(this, b));
        }

        @Override
        public void draw(){
            drawBase();
            drawDecals();
            drawConnections();
            drawHighlight();
        }

        protected void drawBase(){
            Draw.rect(baseRegion, x, y, rotdeg());
        }

        protected void drawDecals(){
            if(decalRegion != null){
                Draw.rect(rotate ? decalRegions[rotation] : decalRegion, x, y, 0);
            }
        }

        protected void drawConnections(){
            for(int i = 0; i < 4; i++){
                if(connections[i] && multiB(i) instanceof BinaryBuild b){
                    Draw.color((
                    (inputValid(i) && b.signal(this)) ||
                    (signal(i) && b.inputValid(Utils.relativeDir(b, this)))
                    ) ? team.color : Color.white);
                    Draw.rect(connectionRegion, x, y, (rotation + i) % 4 * 90);
                }
            }
            Draw.color();
        }

        protected void drawHighlight(){
            if(useOnOffHighlights){
                Draw.rect(signal ? onHighlight : offHighlight, x, y, rotateHighlight ? rotdeg() : 0);
            }else{
                Draw.color(signal ? team.color : Color.white);
                Draw.rect(highlightRegion, x, y, rotateHighlight ? rotdeg() : 0);
                Draw.color();
            }
        }

        @Override
        public void drawTeam(){
            //no
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            signal = read.bool();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(signal);
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.enabled) return Mathf.num(signal);
            return super.sense(sensor);
        }

        //these exist for overriding
        public boolean outputValid(int dir){
            return outputs[dir];
        }

        public boolean inputValid(int dir){
            return inputs[dir];
        }

        //stolen and modified left() code
        public Building multiB(int dir){
            return nearby(Geometry.d4(rotation + dir).x * (block.size / 2 + 1),
            Geometry.d4(rotation + dir).y * (block.size / 2 + 1));
        }
    }
}
