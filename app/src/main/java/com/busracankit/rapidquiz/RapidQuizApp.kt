package com.busracankit.rapidquiz

import android.app.Application

class RapidQuizApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.create()
    }
}
