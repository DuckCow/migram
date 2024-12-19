package com.example.backend.web.model

import java.time.LocalDateTime

fun Post.toResponseDTO(): PostResponseDTO {
    return PostResponseDTO(
        id = this.id,
        title = this.title,
        content = this.content,
        imageUrl = this.mediaUrl,
        country= this.country,
        userDTO = this.user.toDTO(),
        /*
        createdAt = this.createdAt,
        */
        createdAt =this.modifiedAt ?: LocalDateTime.now(),
        createdBy = this.createdBy ?: "",

        modifiedAt = this.modifiedAt ?: LocalDateTime.now()
    )
}

fun com.example.backend.web.model.Comment.toResponseDTO(): com.example.backend.web.model.CommentResponseDTO {
    return com.example.backend.web.model.CommentResponseDTO(
        id = this.id,
        content = this.content,
        postId = this.post.id,
        userDTO = this.user.toDTO(),

        //createdAt = this.createdAt,
        createdAt = this.modifiedAt ?: LocalDateTime.now(),
        createdBy = this.createdBy ?: "",
        modifiedAt = this.modifiedAt ?: LocalDateTime.now()
    )
}

fun User.toDTO(): UserDTO {
    return UserDTO(
        id = this.id,
        name = this.name,
        email = this.email,
        profilePicture = this.profilePicture,
        originCountry = this.originCountry,
        currentCountry = this.currentCountry
    )
}