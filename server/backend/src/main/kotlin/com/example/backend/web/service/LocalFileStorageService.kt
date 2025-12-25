package com.example.backend.web.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Service
@ConditionalOnProperty(name = ["storage.type"], havingValue = "local", matchIfMissing = true)
class LocalFileStorageService(
    private val imageOptimizationService: ImageOptimizationService,
    @Value("\${storage.local.upload-dir:./uploads}") private val uploadDir: String,
    @Value("\${server.port:8080}") private val serverPort: String
) : FileStorageService {

    init {
        // Create upload directories if they don't exist
        Files.createDirectories(Paths.get(uploadDir, "posts"))
        Files.createDirectories(Paths.get(uploadDir, "chat"))
        println("Local file storage initialized at: ${Paths.get(uploadDir).toAbsolutePath()}")
    }

    override fun uploadFile(file: MultipartFile, fileName: String): String? {
        return uploadFile(file, fileName, "posts")
    }

    override fun uploadFile(file: MultipartFile, fileName: String, pathPrefix: String): String? {
        try {
            // Verify authentication
            val authentication = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("UnAuthorized")

            // Check if file is an image and optimize
            val (fileData, contentType, fileSize) = if (isImage(file.contentType)) {
                val optimized = imageOptimizationService.optimizeImage(file)
                println("Image optimized: ${file.originalFilename}")
                println("Original size: ${optimized.originalSize} bytes")
                println("Optimized size: ${optimized.optimizedSize} bytes")
                println("Compression ratio: ${"%.2f".format(optimized.compressionRatio)}%")

                Triple(optimized.data, optimized.contentType, optimized.optimizedSize)
            } else {
                Triple(file.bytes, file.contentType ?: "application/octet-stream", file.size)
            }

            // Create directory for pathPrefix if it doesn't exist
            val prefixPath = Paths.get(uploadDir, pathPrefix)
            Files.createDirectories(prefixPath)

            // Save file to local directory
            val targetPath = prefixPath.resolve(fileName)
            Files.write(targetPath, fileData)

            // Return URL to access the file
            val baseUrl = "http://localhost:$serverPort"
            return "$baseUrl/uploads/$pathPrefix/$fileName"
        } catch (e: Exception) {
            println("WARNING: Failed to upload file locally: ${e.message}")
            println("Upload will fail gracefully")
            e.printStackTrace()
            return null
        }
    }

    private fun isImage(contentType: String?): Boolean {
        return contentType?.startsWith("image/") == true
    }

    override fun deleteFile(fileName: String) {
        try {
            // Try to delete from posts directory (default)
            val postsPath = Paths.get(uploadDir, "posts", fileName)
            if (Files.deleteIfExists(postsPath)) {
                println("Deleted local file from posts: $fileName")
                return
            }

            // Try to delete from chat directory
            val chatPath = Paths.get(uploadDir, "chat", fileName)
            if (Files.deleteIfExists(chatPath)) {
                println("Deleted local file from chat: $fileName")
                return
            }

            println("WARNING: File not found in any directory: $fileName")
        } catch (e: Exception) {
            println("WARNING: Failed to delete local file: ${e.message}")
            // Don't throw exception, just log the error
        }
    }
}
