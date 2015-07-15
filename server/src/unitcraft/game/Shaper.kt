package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.init
import java.util.*
import kotlin.properties.Delegates

class Shaper(r:Resource,val hider: Hider,val editor: Editor,val objs:()-> Objs) {
    val slotStopMove = ArrayList<(Move)->Boolean>()
    val slotMoveAfter = ArrayList<(Shape,Move)->Unit>()

    private val kindsEditor = HashMap<ZetOrder, Pair<MutableList<Kind>, MutableList<Tile>>>().init{
        for(zetOrder in ZetOrder.all)
            this[zetOrder] = ArrayList<Kind>() to ArrayList<Tile>()
    }
    val refinesEditor = ArrayList<(Obj,Pg,Side)->Unit>()

    init{
        for(zetOrder in ZetOrder.all){
            val pairList = kindsEditor[zetOrder]!!
            editor.onEdit(zetOrder.toPriorDraw(),pairList.second, { pg, side, num ->
                val shape = Singl(zetOrder, pg)
                objClashed(shape).forEach { remove(it) }
                val obj = Obj(pairList.first[num], shape)
                refinesEditor.forEach{it(obj,pg,side)}
                objs().add(obj)
            },{pg ->
                objs().lay(zetOrder,pg)?.let {
                    remove(it)
                } ?: false
            })
        }
    }

    //val creates = ArrayList<(Obj)->Unit>()
//    val stopAim = exts.filterIsInstance<OnStopAim>()
//    val arm = exts.filterIsInstance<OnArm>()
//    val getBusys = exts.filterIsInstance<OnGetBusy>()
    /**
     * Возвращает доступность движения.
     * null - движение недоступно
     * ()->Boolean - движение выглядит доступным: фунция возвращает true, если это правда
     */
    fun canMove(move: Move): (()->Boolean)? {
        if(slotStopMove.any{it(move)}) return null
        val objs = objClashed(move.shapeTo)
        if(objs.isEmpty()) return {true}
        val objHided = objs.filter{hider.isHided(it,move.sideVid)}
        if(objHided.isEmpty()) return null
        return { hider.reveal(objHided);false}
    }

    fun move(move: Move) {
        if(objClashed(move.shapeTo).isNotEmpty()) throw Err("cant move obj=${move.obj} to shape=${move.shapeTo}")
        val shapeFrom = move.obj.shape
        move.obj.shape = move.shapeTo
        slotMoveAfter.forEach{it(shapeFrom,move)}
    }

    private fun objClashed(shape: Shape):List<Obj>{
        val sameZetOrd = objs().byZetOrder(shape.zetOrder)
        return sameZetOrd.filter{obj -> shape.pgs.any{it in obj.shape.pgs}}
    }

    fun canCreate(shape:Shape):Boolean{
        return objClashed(shape).isEmpty()
    }

    fun create(kind:Kind,shape:Shape):Obj{
        val obj = Obj(kind,shape)
        //creates.forEach{ it(obj) }
        if(objClashed(shape).isNotEmpty()) throw Err("cant create obj with shape=$shape kind=$kind")
        objs().add(obj)
        return obj
    }

    fun remove(obj:Obj):Boolean{
        return objs().remove(obj)
    }

    fun addToEditor(kind:Kind,zetOrder:ZetOrder,tile:Tile){
        val (kinds,tiles) = kindsEditor[zetOrder]
        kinds.add(kind)
        tiles.add(tile)
    }
}

class Move(
        val obj: Obj,
        val shapeTo: Shape,
        val sideVid: Side
)

enum class ZetOrder {
    flat, voin, fly;

    fun toPriorDraw() = PriorDraw.valueOf(this.name())

    companion object{
        val all = ZetOrder.values()
        val reverse = ZetOrder.values().reverse()
    }
}