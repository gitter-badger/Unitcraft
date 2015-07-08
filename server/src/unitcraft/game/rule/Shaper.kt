package unitcraft.game.rule

class Shaper{
    fun get(obj: Obj, prop: PropertyMetadata): Shape {
        return obj[prop.name] as Shape
    }

    fun set(obj: Obj, prop: PropertyMetadata, v: Shape) {
        obj[prop.name] = v
    }
}