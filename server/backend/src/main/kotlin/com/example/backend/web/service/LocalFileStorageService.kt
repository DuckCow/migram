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

    private val uploadPath: Path = Paths.get(uploadDir, "posts")

    init {
        // Create upload directory if it doesn't exist
        Files.createDirectories(uploadPath)
        println("Local file storage initialized at: ${uploadPath.toAbsolutePath()}")
    }

    override fun uploadFile(file: MultipartFile, fileName: String): String? {
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

            // Save file to local directory
            val targetPath = uploadPath.resolve(fileName)
            Files.write(targetPath, fileData)

            // Return URL to access the file
            val baseUrl = "http://localhost:$serverPort"
            return "$baseUrl/uploads/posts/$fileName"
        } catch (e: Exception) {
            println("WARNING: Failed to upload file locally: ${e.message}")
            println("Post will be created without image")
            e.printStackTrace()
            return null
        }
    }

    private fun isImage(contentType: String?): Boolean {
        return contentType?.startsWith("image/") == true
    }

    override fun deleteFile(fileName: String) {
        try {
            val filePath = uploadPath.resolve(fileName)
            Files.deleteIfExists(filePath)
            println("Deleted local file: $fileName")
        } catch (e: Exception) {
            println("WARNING: Failed to delete local file: ${e.message}")
            // Don't throw exception, just log the error
        }
    }
}
