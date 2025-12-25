package com.example.backend.web.service

import org.springframework.web.multipart.MultipartFile

interface FileStorageService {
    /**
     * Upload a file and return the URL to access it
     * @param file The file to upload
     * @param fileName The name to save the file as
     * @return The URL to access the uploaded file, or null if upload fails
     */
    fun uploadFile(file: MultipartFile, fileName: String): String?

    /**
     * Upload a file with custom path prefix
     * @param file The file to upload
     * @param fileName The name to save the file as
     * @param pathPrefix The path prefix (e.g., "posts", "chat")
     * @return The URL to access the uploaded file, or null if upload fails
     */
    fun uploadFile(file: MultipartFile, fileName: String, pathPrefix: String): String?

    /**
     * Delete a file
     * @param fileName The name of the file to delete
     */
    fun deleteFile(fileName: String)
}
