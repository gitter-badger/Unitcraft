package unitcraft.content

import com.mortennobel.imagescaling.DimensionConstrain
import com.mortennobel.imagescaling.ResampleFilters
import com.mortennobel.imagescaling.ResampleOp
import unitcraft.game.CtxEffect
import unitcraft.server.Err
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage as Image

class CtxEffectImpl(var img: BufferedImage, val size: Int, val maskRaw: BufferedImage) : CtxEffect {
    init {
        if (size.mod(2) != 0) throw Err("size:$size is not even")
    }

    val sizeExtend = size * 2

    override fun fit() {
        img = resize(img, size)
    }

    override fun extend() {
        img = image(sizeExtend) {
            it.drawImage(img, (sizeExtend - img.width) / 2, (sizeExtend - img.height) / 2, null)
        }
    }

    override fun extendBottom() {
        img = image(sizeExtend) {
            val qua = sizeExtend / 4
            it.drawImage(img, qua + (qua * 2 - img.width) / 2, qua * 3 - img.height, null)
        }
    }

    override fun light(h:Int,s:Int,b:Int) {
        val color = colorFromHsb(h,s,b)
        val lightSize = 2
        img = image(sizeExtend) {
            val imgLight = image(sizeExtend) {
                it.drawImage(img, 0, 0, null)
                it.composite = AlphaComposite.SrcIn
                it.color = color
                it.fillRect(0, 0, sizeExtend, sizeExtend)
            }
            it.drawImage(imgLight, lightSize, lightSize, null)
            it.drawImage(imgLight, -lightSize, lightSize, null)
            it.drawImage(imgLight, lightSize, -lightSize, null)
            it.drawImage(imgLight, -lightSize, -lightSize, null)
            it.drawImage(imgLight, lightSize, 0, null)
            it.drawImage(imgLight, -lightSize, 0, null)
            it.drawImage(imgLight, 0, -lightSize, null)
            it.drawImage(imgLight, 0, lightSize, null)
            it.drawImage(img, 0, 0, null)
        }
    }

    private fun colorFromHsb(h:Int,s:Int,b:Int) = Color.getHSBColor(h/360F,s/100F,b/100F)


    override fun place() {
        img = resizeToSquare(img, sizeExtend)
        val mask = prepareMask()
        img = image(sizeExtend) {
            it.drawImage(img, 0, 0, null)
            it.composite = AlphaComposite.DstIn
            it.drawImage(mask, 0, 0, null)
        }
    }

    private fun prepareMask(): BufferedImage {
        val premask = resizeToSquare(maskRaw, sizeExtend / 4 * 3)

        val mask = image(sizeExtend) {}
        val tr = sizeExtend / 4
        for (x in tr..3 * tr - 1) {
            for (y in 0..2 * tr - 1) {
                if (2 * tr - y <= x && y < x && y >= x - 2 * tr && 4 * tr - y > x ) {
                    mask.setRGB(x, y, premask.getRGB(x, y))
                    mask.setRGB(x, y + 2 * tr, invertAlpha(premask.getRGB(x, y)))
                }
            }
        }

        for (x in 0..2 * tr - 1) {
            for (y in tr..3 * tr - 1) {
                if (2 * tr - y <= x && y >= x && y < x + 2 * tr && 4 * tr - y > x ) {
                    mask.setRGB(x, y, premask.getRGB(x, y))
                    mask.setRGB(x + 2 * tr, y, invertAlpha(premask.getRGB(x, y)))
                }
            }
        }
        return mask
    }

    override fun opacity(procent: Int) {
        if(procent <0 || procent >100) throw Err("invalid opacity=$procent")
        img = image(img.width, img.height) {
            it.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, procent.toFloat()/100);
            it.drawImage(img, 0, 0, null)
        }
    }

    override fun shadow(color: Color) {
        val sizeShadow = size / 10
        val imgShadow = createDropShadow(sizeShadow, color)
        img = image(img.width, img.height) {
            it.drawImage(imgShadow, sizeShadow / 2, sizeShadow / 2, null)
            it.drawImage(img, 0, 0, null)
        }
    }

    private fun createDropShadow(sizeShadow: Int, color: Color): BufferedImage {
        val xr = img.width + 4 * sizeShadow
        val yr = img.height + 4 * sizeShadow
        var imgShadow = image(xr, yr) {
            it.drawImage(img, 0, 0, null)
            it.composite = AlphaComposite.SrcIn;
            it.color = color;
            it.fillRect(0, 0, xr, yr);
        }
        imgShadow = getGaussianBlurFilter(sizeShadow, true).filter(imgShadow, null)
        imgShadow = getGaussianBlurFilter(sizeShadow, false).filter(imgShadow, null)
        return imgShadow
    }

    private fun getGaussianBlurFilter(radius: Int, horizontal: Boolean): ConvolveOp {
        val sigma = radius / 3f
        val twoSigmaSquare = 2f * sigma * sigma
        val sigmaRoot = Math.sqrt(twoSigmaSquare * Math.PI).toFloat()
        var total = 0f

        val size = radius * 2 + 1
        val data = FloatArray(size)
        for (i in -radius..radius) {
            val distance = i * i
            val index = i + radius
            data[index] = Math.exp(-distance.toDouble() / twoSigmaSquare).toFloat() / sigmaRoot
            total += data[index]
        }
        for (i in data.indices) {
            data[i] = data[i] / total
        }

        val kernel = if (horizontal) Kernel(size, 1, data) else Kernel(1, size, data)
        return ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
    }

    companion object {
        private fun image(size: Int, f: (Graphics2D) -> Unit) = image(size, size, f)

        private fun image(xr: Int, yr: Int, f: (Graphics2D) -> Unit): BufferedImage {
            val img = BufferedImage(xr, yr, BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            f(g)
            g.dispose()
            return img
        }

        // масштабирует img так, что его большая сторона равна sz
        // сохраняет пропорции
        fun resize(img: BufferedImage, sz: Int): BufferedImage {
            val op = ResampleOp(DimensionConstrain.createMaxDimension(sz, sz))
            op.filter = ResampleFilters.getLanczos3Filter()
            return op.filter(img, null)
        }

        // масштабирует img так, что его меньшая сторона равна sz
        // затем обрезает до квадрата со стороной sz
        private fun resizeToSquare(img: BufferedImage, sz: Int): BufferedImage {
            val fct = Math.max(sz.toFloat() / img.width, sz.toFloat() / img.height)
            val op = ResampleOp(DimensionConstrain.createRelativeDimension(fct))
            op.filter = ResampleFilters.getLanczos3Filter()
            val imgRsz = op.filter(img, null)
            return image(sz) {
                it.drawImage(imgRsz, (sz - imgRsz.width) / 2, (sz - imgRsz.height) / 2, null)
            }
        }

        private fun invertAlpha(rgba: Int): Int {
            val c = Color(rgba, true)
            val a = c.alpha
            val aa = when {
                a == 255 -> 0
                a == 0 -> 255
                a > 0 -> 255
                else -> a
            }
            return Color(0, 0, 0, aa).rgb
        }
    }
}