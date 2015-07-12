package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class Catapult(r: Resource, val drawer: Drawer, val spoter: Spoter, val shaper: Shaper, val objs: () -> Objs) : Skil {
    val name = "catapult"
    val tile = r.tile(name)
    val tlsAkt = r.tlsAkt(name)

    init {
        shaper.addToEditor(KindCatapult,ZetOrder.flat,tile)
        drawer.onDraw(PriorDraw.flat) { side, ctx ->
            for (obj in objs()) if (obj.kind == KindCatapult) ctx.drawTile((obj.shape as Singl).head, tile)
        }

        spoter.listSkils.add {
            if (it.shape.zetOrder==ZetOrder.voin && it.shape.pgs.intersect(objs().byKind(KindCatapult).flatMap { it.shape.pgs }).isNotEmpty()) this else null
        }
    }

    override fun preAkts(sideVid: Side, obj: Obj) =
            obj.shape.head.all.map { pg ->
                val move = Move(obj, obj.shape.headTo(pg), sideVid)
                val can = shaper.canMove(move)
                if (can != null) PreAkt(pg, tlsAkt) {
                    if (can()) {
                        shaper.move(move)
                        spoter.tire(obj)
                    }
                } else null
            }.filterNotNull()


    private object KindCatapult : Kind()
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

