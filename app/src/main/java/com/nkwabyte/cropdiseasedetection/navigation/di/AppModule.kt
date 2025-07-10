package com.nkwabyte.cropdiseasedetection.navigation.di

import com.nkwabyte.cropdiseasedetection.navigation.viewmodel.DetectionResultViewModel
import com.nkwabyte.cropdiseasedetection.navigation.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
//    single { (context: Context) -> ObjectDetector(context) }
    viewModelOf(::DetectionResultViewModel)
    viewModelOf(::ProfileViewModel)
}
