package com.example.demo.data

import com.example.demo.data.model.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository: JpaRepository<UserEntity, Long> {

    fun findByUserName(userName: String): UserEntity

    fun existsByUserName(userName: String): Boolean
}