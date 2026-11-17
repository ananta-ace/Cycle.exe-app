package com.example.data

import android.content.Context
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CommunityRepository
import com.example.data.repository.CrewRepository
import com.example.data.repository.FirebaseProvider
import com.example.data.repository.GalleryRepository
import com.example.data.repository.ImageUploadRepository
import com.example.data.repository.MissionsRepository
import com.example.data.repository.NotificationsRepository
import com.example.data.repository.ThemePreferencesRepository

class CycleContainer(context: Context) {
    private val appContext = context.applicationContext

    val firebaseProvider = FirebaseProvider(appContext)
    val themePreferencesRepository = ThemePreferencesRepository(appContext)
    val imageUploadRepository = ImageUploadRepository(appContext, firebaseProvider)
    val authRepository = AuthRepository(appContext, firebaseProvider)
    val communityRepository = CommunityRepository(firebaseProvider)
    val crewRepository = CrewRepository(firebaseProvider)
    val missionsRepository = MissionsRepository(firebaseProvider)
    val galleryRepository = GalleryRepository(firebaseProvider)
    val notificationsRepository = NotificationsRepository(firebaseProvider)
    val adminRepository = AdminRepository(firebaseProvider)
}
