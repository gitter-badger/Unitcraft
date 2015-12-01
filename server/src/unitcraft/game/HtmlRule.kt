package unitcraft.game

import java.util.*

class HtmlRule(val slots :List<Slot<*>>) {
    //private val slots = ArrayList<Slot<*>>()
    private val sb = StringBuilder()

    fun html():String {
        sb.appendln("<!DOCTYPE html>")
        sb.appendln("""<meta charset="UTF-8">""")
        tagLn("title","Приоритет правил")
        for (slot in slots) {
            title(slot.title)
            slot.each{prior,desc -> rule(prior,desc) }

        }
        return sb.toString()
    }

    fun tag(name:String, content:String){
        sb.append("<$name>$content</$name>")
    }

    fun tag(name:String, content:()->Unit){
        sb.appendln("<$name>")
        content()
        sb.appendln("</$name>")
    }

    fun tagLn(name:String,content:String){
        tag(name,content)
        sb.appendln()
    }

    fun title(s:String){
        tagLn("h2",s)
    }

    fun rule(prior:Int,desc:String){
        tag("p") {
            tag("strong", prior.toString())
            sb.appendln(" " + desc)
        }
    }


}
