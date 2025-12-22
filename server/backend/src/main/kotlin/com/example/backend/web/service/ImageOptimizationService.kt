package com.example.backend.web.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter

@Service
class ImageOptimizationService {

    companion object {
        const val MAX_WIDTH = 1920
        const val MAX_HEIGHT = 1920
        const val JPEG_QUALITY = 0.85f
    }

    /**
     * Optimize image by resizing and compressing
     * Returns optimized image as byte array with updated content type
     */
    fun optimizeImage(file: MultipartFile): OptimizedImage {
        val originalImage = ImageIO.read(ByteArrayInputStream(file.bytes))
            ?: throw IllegalArgumentException("Invalid image file")

        // Calculate new dimensions maintaining aspect ratio
        val (newWidth, newHeight) = calculateNewDimensions(
            originalImage.width,
            originalImage.height
        )

        // Resize if needed
        val resizedImage = if (newWidth != originalImage.width || newHeight != originalImage.height) {
            resizeImage(originalImage, newWidth, newHeight)
        } else {
            originalImage
        }

        // Compress image
        val compressedBytes = compressImage(resizedImage, file.contentType ?: "image/jpeg")

        return OptimizedImage(
            data = compressedBytes,
            contentType = "image/jpeg",  // Always convert to JPEG for consistency
            originalSize = file.size,
            optimizedSize = compressedBytes.size.toLong()
        )
    }

    private fun calculateNewDimensions(width: Int, height: Int): Pair<Int, Int> {
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return Pair(width, height)
        }

        val widthRatio = MAX_WIDTH.toDouble() / width
        val heightRatio = MAX_HEIGHT.toDouble() / height
        val ratio = minOf(widthRatio, heightRatio)

        return Pair(
            (width * ratio).toInt(),
            (height * ratio).toInt()
        )
    }

    private fun resizeImage(originalImage: BufferedImage, newWidth: Int, newHeight: Int): BufferedImage {
        val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = resizedImage.createGraphics()

        // High-quality rendering hints
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
        graphics.dispose()

        return resizedImage
    }

    private fun compressImage(image: BufferedImage, contentType: String): ByteArray {
        val outputStream = ByteArrayOutputStream()

        // Get JPEG writer
        val writers = ImageIO.getImageWritersByFormatName("jpg")
        if (!writers.hasNext()) {
            throw IllegalStateException("No JPEG writer found")
        }

        val writer: ImageWriter = writers.next()
        val writeParam = writer.defaultWriteParam

        // Set compression quality
        writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
        writeParam.compressionQuality = JPEG_QUALITY

        // Write compressed image
        ImageIO.createImageOutputStream(outputStream).use { ios ->
            writer.output = ios
            writer.write(null, IIOImage(image, null, null), writeParam)
        }

        writer.dispose()

        return outputStream.toByteArray()
    }
}

data class OptimizedImage(
    val data: ByteArray,
    val contentType: String,
    val originalSize: Long,
    val optimizedSize: Long
) {
    val compressionRatio: Double
        get() = (1.0 - (optimizedSize.toDouble() / originalSize)) * 100

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OptimizedImage

        if (!data.contentEquals(other.data)) return false
        if (contentType != other.contentType) return false
        if (originalSize != other.originalSize) return false
        if (optimizedSize != other.optimizedSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + originalSize.hashCode()
        result = 31 * result + optimizedSize.hashCode()
        return result
    }
}
