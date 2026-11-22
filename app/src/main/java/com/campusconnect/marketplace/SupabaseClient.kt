package com.campusconnect.marketplace

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin

val supabase: SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Auth) {
        autoLoadFromStorage = true
        alwaysAutoRefresh = true
    }
    install(Postgrest)
    install(Storage)
    install(Realtime)
    install(ComposeAuth) {
        // MUST BE THE WEB CLIENT ID (ends with .apps.googleusercontent.com)
        // DO NOT USE THE ANDROID CLIENT ID HERE
        googleNativeLogin(serverClientId = "623780508348-cfhh7e2p831lrc11hp9kj5qgrsdu1jsc.apps.googleusercontent.com")
    }
}