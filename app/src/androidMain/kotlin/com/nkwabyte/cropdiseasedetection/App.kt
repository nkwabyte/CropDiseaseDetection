package com.nkwabyte.cropdiseasedetection

import android.app.Application
import com.nkwabyte.cropdiseasedetection.common.navigation.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}
