package com.example.videoplaye

import android.app.Application
import com.google.android.gms.cast.framework.CastContext

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize Cast ONCE for whole app
        CastContext.getSharedInstance(this)
    }
}
