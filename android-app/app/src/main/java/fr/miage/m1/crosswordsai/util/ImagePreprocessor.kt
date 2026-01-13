package fr.miage.m1.crosswordsai.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.min

/**
 * Prétraitement d'image pour les grilles de mots croisés.
 * Réplique la logique OpenCV du backend en utilisant les APIs Android natives.
 */
object ImagePreprocessor {

    private const val MAX_DIMENSION = 1500

    /**
     * Prétraitement complet : Redimensionnement + Gris + Netteté + Binarisation
     * @param bitmap L'image source
     * @return L'image prétraitée
     */
    fun preprocess(bitmap: Bitmap): Bitmap {
        // 1. Redimensionnement (max 1500px)
        val resized = resizeIfNeeded(bitmap)
        
        // 2. Conversion en niveaux de gris
        val grayscale = toGrayscale(resized)
        
        // 3. Netteté (sharpen)
        val sharpened = applySharpen(grayscale)
        
        // 4. Binarisation (seuil à 200)
        val binarized = applyThreshold(sharpened, 200)
        
        // Nettoyer les bitmaps intermédiaires si différents
        if (resized != bitmap) resized.recycle()
        if (grayscale != resized) grayscale.recycle()
        if (sharpened != grayscale) sharpened.recycle()
        
        return binarized
    }

    /**
     * Redimensionne l'image si une dimension dépasse MAX_DIMENSION
     */
    private fun resizeIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return bitmap
        }
        
        val scale = min(
            MAX_DIMENSION.toFloat() / width,
            MAX_DIMENSION.toFloat() / height
        )
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Convertit en niveaux de gris
     */
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }

    /**
     * Applique un filtre de netteté (sharpen kernel 3x3)
     * Kernel: [0, -1, 0, -1, 5, -1, 0, -1, 0]
     */
    private fun applySharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val result = IntArray(width * height)
        
        // Kernel de netteté
        val kernel = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sum = 0f
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val gray = Color.red(pixel) // En grayscale, R=G=B
                        val kernelValue = kernel[(ky + 1) * 3 + (kx + 1)]
                        sum += gray * kernelValue
                    }
                }
                
                val value = sum.toInt().coerceIn(0, 255)
                result[y * width + x] = Color.rgb(value, value, value)
            }
        }
        
        // Copier les bords
        for (x in 0 until width) {
            result[x] = pixels[x]
            result[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            result[y * width] = pixels[y * width]
            result[y * width + width - 1] = pixels[y * width + width - 1]
        }
        
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(result, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    /**
     * Applique une binarisation avec un seuil donné
     */
    private fun applyThreshold(bitmap: Bitmap, threshold: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            val value = if (gray > threshold) 255 else 0
            pixels[i] = Color.rgb(value, value, value)
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
