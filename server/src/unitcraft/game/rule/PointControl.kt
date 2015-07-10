package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Flag(r: Resource,val drawer:DrawerObjOwn,val editor:EditorObjOwn,val pointOfControl:PointControl) {
    val tls = r.tlsObjOwn("flag")
    init{
        drawer.regKind(KindFlag,tls)
        editor.addKind(KindFlag,tls.neut)
        pointOfControl.addKind(KindFlag)
    }
//    endTurn(100){
//        for((pg,flat) in flats)
//            g.info(MsgVoin(pg)).voin?.let{
//                flat.side = it.side!!
//            }
//    }
}

object KindFlag : Kind()

class Mine(r: Resource,val drawer:DrawerObjOwn,val editor:EditorObjOwn,val pointOfControl:PointControl) {
    val tls = r.tlsObjOwn("mine")
    init{
        drawer.regKind(KindMine,tls)
        editor.addKind(KindMine,tls.neut)
        pointOfControl.addKind(KindMine)
    }
    //        endTurn(100){
    //            for((pg,flat) in flats)
    //                g.info(MsgVoin(pg)).voin?.let{
    //                    flat.side = it.side!!
    //                }
    //            for((pg,flat) in flats) flat.side?.let{
    //                if(it == g.sideTurn) g.make(EfkGold(1,it))
    //            }
    //        }
}

object KindMine : Kind()

class Hospital(r: Resource,val drawer:DrawerObjOwn,val editor:EditorObjOwn,val pointOfControl:PointControl) {
    val tls = r.tlsObjOwn("hospital")
    init{
        drawer.regKind(KindHospital,tls)
        editor.addKind(KindHospital,tls.neut)
        pointOfControl.addKind(KindHospital)
    }
}

object KindHospital : Kind()

class DrawerObjOwn(val drawer:Drawer,val objs:()-> Objs) {
    private val tlsObjOwns = HashMap<Kind,TlsObjOwn>()
    private val kinds = ArrayList<Kind>()

    init{
        drawer.onDraw(PriorDraw.flat){ side,ctx ->
            for (obj in objs().filterIsInstance<ObjOwn>().byKind(kinds)) {
                val shape = obj.shape
                when(shape){
                    is Singl -> {
                        ctx.drawTile(shape.pg,tlsObjOwns[obj.kind](side,obj))
                    }
                    else -> throw Err("unknown shape=$shape")
                }
            }
        }
    }

    fun regKind(kind:Kind,tlsObjOwn:TlsObjOwn){
        kinds.add(kind)
        tlsObjOwns[kind] = tlsObjOwn
    }
}

class EditorObjOwn(val editor:Editor,val objs:()-> Objs){

    private val kinds = ArrayList<Kind>()
    private val tiles = ArrayList<Int>()

    fun build(){
        editor.onEdit(tiles,{pg,side,num ->
            objs().add(ObjOwn(kinds[num],Singl(ZetOrder.flat,pg)))
        },{pg ->
            objs().byPg(pg).filterIsInstance<ObjOwn>().byKind(kinds).firstOrNull()?.let {
                objs().remove(it)
            } ?: false})
    }

    fun addKind(kind:Kind,tile:Int){
        kinds.add(kind)
        tiles.add(tile)
    }
}

class PointControl(val stager:Stager,val objs:()-> Objs){
    val kinds = ArrayList<Kind>()

    init{
        stager.onEndTurn {
            for (obj in objs().byKind(kinds).filterIsInstance<ObjOwn>()) {
                for(voin in objs().filterIsInstance<Voin>()){
                    if(intersect(obj.shape,voin.shape)){
                        obj.side = voin.side
                    }
                }
            }
        }
    }

    private fun intersect(shape: Shape, shapeOther: Shape) =
        if(shape is Singl && shapeOther is Singl) shape.pg == shapeOther.pg else false


    fun addKind(kind:Kind){
        kinds.add(kind)
    }
}
