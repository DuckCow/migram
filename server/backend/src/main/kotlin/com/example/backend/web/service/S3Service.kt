package com.example.backend.web.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import org.springframework.security.access.AccessDeniedException

@Service
@ConditionalOnProperty(name = ["storage.type"], havingValue = "s3")
class S3Service(
    private val s3Client: S3Client,
    private val imageOptimizationService: ImageOptimizationService,
    @Value("\${cloud.aws.s3.bucket}")  val bucket: String
) : FileStorageService {
    override fun uploadFile(file: MultipartFile, fileName: String): String? {
        try {
            // 현재 인증된 사용자 확인
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

            // 메타데이터 설정
            val metadata = PutObjectRequest.builder()
                .bucket(bucket)
                .key("posts/$fileName")
                .contentType(contentType)
                .contentLength(fileSize)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build()

            // 파일 업로드
            s3Client.putObject(metadata, RequestBody.fromBytes(fileData))

            // S3 URL 반환
            return "https://$bucket.s3.amazonaws.com/posts/$fileName"
        } catch (e: Exception) {
            // S3 업로드 실패 시 경고 로그 출력하고 null 반환 (이미지 없이 게시글 저장)
            println("WARNING: Failed to upload file to S3: ${e.message}")
            println("Post will be created without image")
            return null
        }
    }

    private fun isImage(contentType: String?): Boolean {
        return contentType?.startsWith("image/") == true
    }

    override fun deleteFile(fileName: String) {
        try {
            val deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key("posts/$fileName")
                .build()

            s3Client.deleteObject(deleteRequest)
        } catch (e: Exception) {
            throw RuntimeException("Failed to delete file from S3", e)
        }
    }
}