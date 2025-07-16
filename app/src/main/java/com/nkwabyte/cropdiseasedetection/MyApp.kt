package com.nkwabyte.cropdiseasedetection

import android.app.Application
import com.nkwabyte.cropdiseasedetection.navigation.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            modules(appModule)
        }
    }
}
