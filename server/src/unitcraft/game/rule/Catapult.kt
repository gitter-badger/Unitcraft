package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class Catapult(r: Resource,val editor:Editor,val drawer:Drawer,val objs:()-> Objs) {
    val name = "catapult"
    val tile = r.tile(name)
    val tlsAkt = r.tlsAkt(name)

    init{
        editor.onEdit(tile,{pg, side ->
            objs().add(Obj(KindCatapult, Singl(pg)))
        },{objs().remove(it)})

        drawer.onDraw(PriorDraw.flat){side, ctx ->
            for(obj in objs()) if(obj.kind == KindCatapult) ctx.drawTile((obj.shape as Singl).pg,tile)
        }
    }
    //        info<MsgSpot>(20) {
    //            if (pgSrc in flats) g.voin(pgSpot,side)?.let {
    //                val tggl = g.info(MsgTgglRaise(pgSpot, it))
    //                if(!tggl.isCanceled) {
    //                    val r = Raise(pgSpot, tggl.isOn)
    //                    for (pg in g.pgs) if(!g.stop(EfkMove(pgSpot, pg, it))) r.add(pg, tlsAkt, EfkMove(pgSpot, pg, it))
    //                    add(r)
    //                }
    //            }
    //        }
}

object KindCatapult : Kind()