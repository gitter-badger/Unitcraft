package unitcraft.game.rule

class Lifer {

    fun get(obj: Obj, prop: PropertyMetadata): Life {
        return obj.getOrPut(prop.name){Life(5)} as Life
    }

    fun set(obj: Obj, prop: PropertyMetadata, v: Life) {
        obj[prop.name] = v
    }
}


class Life(valueInit: Int) {
    var value: Int = valueInit
        private set

    fun alter(d: Int) {
        value += d
    }

    override fun toString() = "Life($value)"
}
