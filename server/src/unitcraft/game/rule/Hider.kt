package unitcraft.game.rule

class Hider {
    fun get(obj: Obj, prop: PropertyMetadata): Boolean {
        return (obj[prop.name] as? Boolean)?:false
    }

    fun set(obj: Obj, prop: PropertyMetadata, v: Boolean) {
        obj[prop.name] = v
    }

    fun hide(obj: Obj) {

    }

    //    fun getBusy(pg: Pg, tpMove: TpMove, side: Side): Busy? {
    //        if(tpMove!=TpMove.unit)  return null
    //        return grid()[pg]?.let{ if(it.isHided) Busy{ it.isHided = false } else Busy() }
    //    }
}