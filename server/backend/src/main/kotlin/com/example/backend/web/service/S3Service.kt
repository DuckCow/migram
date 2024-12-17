package com.example.backend.web.service

import org.springframework.beans.factory.annotation.Value
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
class S3Service(
    private val s3Client: S3Client,
    @Value("\${cloud.aws.s3.bucket}") private val bucket: String
) {
    fun uploadFile(file: MultipartFile, fileName: String): String {
        try {
            // 현재 인증된 사용자 확인
            val authentication = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("UnAuthorized")

            // 메타데이터 설정
            val metadata = PutObjectRequest.builder()
                .bucket(bucket)
                .key("posts/$fileName")
                .contentType(file.contentType)
                .contentLength(file.size)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build()

            // 파일 업로드
            s3Client.putObject(metadata, RequestBody.fromInputStream(file.inputStream, file.size))

            // S3 URL 반환
            return "https://$bucket.s3.amazonaws.com/posts/$fileName"
        } catch (e: Exception) {
            throw RuntimeException("Failed to upload file to S3", e)
        }
    }

    fun deleteFile(fileName: String) {
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