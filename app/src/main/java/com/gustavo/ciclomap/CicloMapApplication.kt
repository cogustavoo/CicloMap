package com.gustavo.ciclomap

import android.app.Application
import com.google.firebase.FirebaseApp

class CicloMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}