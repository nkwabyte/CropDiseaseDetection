package com.nkwabyte.cropdiseasedetection.common.navigation.di

import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AuthViewModel
import com.nkwabyte.cropdiseasedetection.data.network.CloudinaryApi
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import com.nkwabyte.cropdiseasedetection.data.storage.OfflineQueueStore
import com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.HistoryViewModel
import org.koin.dsl.module

val appModule = module {
    single { ObjectDetector() }
    single { CloudinaryApi() }
    single { OfflineQueueStore() }
    single { SyncRepository(get()) }
    single { SettingsManager() }

    single { AppViewModel(get(), get()) }
    single {
        DetectionViewModel(
            detector = get<ObjectDetector>(),
            cloudinaryApi = get<CloudinaryApi>(),
            syncRepository = get<SyncRepository>()
        )
    }
    single { ProfileViewModel(get()) }
    single { AuthViewModel(get()) }
    single { HistoryViewModel(get(), get()) }
}
