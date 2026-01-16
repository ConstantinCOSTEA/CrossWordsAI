package fr.miage.m1.crosswordsai.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.min
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

/**
 * Prétraitement d'image pour les grilles de mots croisés.
 * Réplique la logique OpenCV du backend en utilisant les APIs Android natives.
 */
object ImagePreprocessor {

    private const val MAX_DIMENSION = 1500

    /**
     * Prétraitement complet : Redimensionnement + Gris + Contraste + Netteté légère
     * Optimisé pour l'OCR - pas de binarisation brutale
     * @param bitmap L'image source
     * @return L'image prétraitée
     */
    fun preprocess(bitmap: Bitmap): Bitmap {
        // 1. Redimensionnement (max 1500px)
        val resized = resizeIfNeeded(bitmap)
        
        // 2. Conversion en niveaux de gris
        val grayscale = toGrayscale(resized)
        
        // 3. Amélioration du contraste (remplace la binarisation brutale)
        val enhanced = enhanceContrast(grayscale)
        
        // 4. Netteté légère (sharpen)
        val sharpened = applySharpen(enhanced)
        
        // Nettoyer les bitmaps intermédiaires si différents
        if (resized != bitmap) resized.recycle()
        if (grayscale != resized) grayscale.recycle()
        if (enhanced != grayscale) enhanced.recycle()
        
        return sharpened
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
        
        return bitmap.scale(newWidth, newHeight)
    }

    /**
     * Convertit en niveaux de gris
     */
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = createBitmap(bitmap.width, bitmap.height)
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
        
        val resultBitmap = createBitmap(width, height)
        resultBitmap.setPixels(result, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    /**
     * Améliore le contraste de l'image avec un étirement d'histogramme adaptatif.
     * Bien meilleur pour l'OCR qu'une binarisation brutale.
     */
    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Trouver les valeurs min et max (ignorer les extrêmes 1%)
        val values = pixels.map { Color.red(it) }.sorted()
        val minIdx = (values.size * 0.01).toInt()
        val maxIdx = (values.size * 0.99).toInt()
        val minVal = values[minIdx]
        val maxVal = values[maxIdx]
        
        // Éviter la division par zéro
        val range = (maxVal - minVal).coerceAtLeast(1)
        
        // Étirement d'histogramme avec légère augmentation du contraste
        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            // Normaliser entre 0 et 255
            val normalized = ((gray - minVal) * 255 / range).coerceIn(0, 255)
            // Appliquer une courbe de contraste (gamma léger)
            val enhanced = (255 * Math.pow(normalized / 255.0, 0.9)).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(enhanced, enhanced, enhanced)
        }
        
        val result = createBitmap(width, height)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
