package com.example.videoplaye

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent // Use the specific Cast version
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            // Corrected constant name for the Default Receiver
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .build()
    }

    override fun getAdditionalSessionProviders(
        context: Context
    ): MutableList<SessionProvider>? = null // Return null or mutableListOf()
}