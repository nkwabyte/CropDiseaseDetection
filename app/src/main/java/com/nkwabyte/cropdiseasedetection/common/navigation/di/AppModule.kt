package com.nkwabyte.cropdiseasedetection.common.navigation.di

import com.nkwabyte.cropdiseasedetection.common.helpers.PyTorchObjectDetector
import com.nkwabyte.cropdiseasedetection.common.helpers.TFObjectDetector
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import org.koin.dsl.module

val appModule = module {
    single { PyTorchObjectDetector(get()) }
    single { TFObjectDetector(get()) }

    // Shared instances
    single { AppViewModel() }
    single {
        DetectionViewModel(
            pytorchDetector = get<PyTorchObjectDetector>(),
            tfDetector = get<TFObjectDetector>()
        )
    }
    single { ProfileViewModel() }
}
