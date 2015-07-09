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
import java.awt.image.BufferedImage as Image

class CtxEffectImpl(var img: BufferedImage, val size: Int, val maskRaw: BufferedImage) : CtxEffect {
    init {
        if (size.mod(2) != 0) throw Err("size:${size} is not even")
    }

    val sizeExtend = size * 2

    override fun fit() {
        img = resize(img, size)
    }

    override fun extend() {
        img = image(sizeExtend) {
            it.drawImage(img, (sizeExtend - img.getWidth()) / 2, (sizeExtend - img.getHeight()) / 2, null)
        }
    }

    override fun extendBottom() {
        img = image(sizeExtend) {
            val qua = sizeExtend / 4
            it.drawImage(img, qua + (qua * 2 - img.getWidth()) / 2, qua * 3 - img.getHeight(), null)
        }
    }

    override fun light(color:Color) {
        val lightSize = if (size <= 100) 2 else 3
        img = image(sizeExtend) {
            val imgLight = image(sizeExtend) {
                it.drawImage(img, 0, 0, null)
                it.setComposite(AlphaComposite.SrcIn);
                it.setColor(color);
                it.fillRect(0, 0, sizeExtend, sizeExtend);
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

    override fun place() {
        img = resizeToSquare(img, sizeExtend)
        val mask = prepareMask()
        img = image(sizeExtend) {
            it.drawImage(img, 0, 0, null)
            it.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN))
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

    override fun glow(color: Color) {
        val lightSize = if (size <= 100) 2 else 3
        val sizeShadow = size / 5
        var imgLight = image(sizeExtend) {
            val imgLight = image(sizeExtend) {
                it.drawImage(img, 0, 0, null)
                it.setComposite(AlphaComposite.SrcIn);
                it.setColor(color);
                it.fillRect(0, 0, sizeExtend, sizeExtend);
            }
            it.drawImage(imgLight, lightSize, lightSize, null)
            it.drawImage(imgLight, -lightSize, lightSize, null)
            it.drawImage(imgLight, lightSize, -lightSize, null)
            it.drawImage(imgLight, -lightSize, -lightSize, null)
            it.drawImage(imgLight, lightSize, 0, null)
            it.drawImage(imgLight, -lightSize, 0, null)
            it.drawImage(imgLight, 0, -lightSize, null)
            it.drawImage(imgLight, 0, lightSize, null)
        }
        imgLight = getGaussianBlurFilter(sizeShadow, true).filter(imgLight, null)
        imgLight = getGaussianBlurFilter(sizeShadow, false).filter(imgLight, null)
        img = image(img.getWidth(), img.getHeight()) {
            it.drawImage(imgLight, 0, 0, null)
            it.drawImage(img, 0, 0, null)
        }
    }

    private fun createGlow(sizeShadow: Int, color: Color): BufferedImage {
        val xr = img.getWidth() + 4 * sizeShadow
        val yr = img.getHeight() + 4 * sizeShadow
        var imgShadow = image(xr, yr) {
            it.drawImage(img, 0, 0, null)
            it.setComposite(AlphaComposite.SrcIn);
            it.setColor(color);
            it.fillRect(0, 0, xr, yr);
        }
        imgShadow = getGaussianBlurFilter(sizeShadow, true).filter(imgShadow, null)
        imgShadow = getGaussianBlurFilter(sizeShadow, false).filter(imgShadow, null)
        return imgShadow
    }

    override fun shadow(color: Color) {
        val sizeShadow = size / 10
        val imgShadow = createDropShadow(sizeShadow, color)
        img = image(img.getWidth(), img.getHeight()) {
            it.drawImage(imgShadow, -sizeShadow * 2 + sizeShadow / 2, -sizeShadow * 2 + sizeShadow / 2, null)
            it.drawImage(img, 0, 0, null)
        }
    }

    private fun createDropShadow(sizeShadow: Int, color: Color): BufferedImage {
        val xr = img.getWidth() + 4 * sizeShadow
        val yr = img.getHeight() + 4 * sizeShadow
        var imgShadow = image(xr, yr) {
            it.drawImage(img, sizeShadow * 2, sizeShadow * 2, null)
            it.setComposite(AlphaComposite.SrcIn);
            it.setColor(color);
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
            op.setFilter(ResampleFilters.getLanczos3Filter())
            return op.filter(img, null)
        }

        // масштабирует img так, что его меньшая сторона равна sz
        // затем обрезает до квадрата со стороной sz
        private fun resizeToSquare(img: BufferedImage, sz: Int): BufferedImage {
            val fct = Math.max(sz.toFloat() / img.getWidth(), sz.toFloat() / img.getHeight())
            val op = ResampleOp(DimensionConstrain.createRelativeDimension(fct))
            op.setFilter(ResampleFilters.getLanczos3Filter())
            val imgRsz = op.filter(img, null)
            return image(sz) {
                it.drawImage(imgRsz, (sz - imgRsz.getWidth()) / 2, (sz - imgRsz.getHeight()) / 2, null)
            }
        }

        private fun invertAlpha(rgba: Int): Int {
            val c = Color(rgba, true)
            val a = c.getAlpha()
            val aa = when {
                a == 255 -> 0
                a == 0 -> 255
                a > 0 -> 255
                else -> a
            }
            return Color(0, 0, 0, aa).getRGB()
        }
    }
}