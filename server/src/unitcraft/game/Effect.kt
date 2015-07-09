package unitcraft.game

import java.awt.image.BufferedImage as Image
import com.mortennobel.imagescaling.ResampleFilters
import com.mortennobel.imagescaling.DimensionConstrain
import com.mortennobel.imagescaling.ResampleOp
import unitcraft.game.CtxEffect
import java.awt.image.BufferedImage
import java.awt.Graphics2D
import java.awt.AlphaComposite
import java.awt.Color
import unitcraft.server.Err
import java.awt.image.ConvolveOp
import java.awt.image.Kernel


class Effect(val name: String, val op: CtxEffect.() -> Unit){
    override fun toString(): String {
        return "Effect($name)"
    }
}

interface CtxEffect{
    fun fit()
    fun extend()
    fun extendBottom()
    fun light(color: Color)
    fun place()
    fun shadow(color: Color)
    fun glow(color: Color)
}
