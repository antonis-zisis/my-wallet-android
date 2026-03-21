package com.mywallet.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

@HiltAndroidApp
class MyWalletApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
