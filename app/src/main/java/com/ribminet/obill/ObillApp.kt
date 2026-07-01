package com.ribminet.obill

import android.app.Application
import com.ribminet.obill.data.remote.ApiClient
import com.ribminet.obill.data.remote.CustomerRepository
import com.ribminet.obill.data.remote.TokenStore
import com.ribminet.obill.push.OneSignalConfig
import com.ribminet.obill.push.OneSignalManager

class ObillApp : Application() {

    lateinit var repository: CustomerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        // OneSignal wajib di-init sedini mungkin (sebelum Activity).
        OneSignalManager.init(this, OneSignalConfig.APP_ID)
        val tokenStore = TokenStore(this)
        val api = ApiClient.create(tokenStore)
        repository = CustomerRepository(api, tokenStore)
    }

    /** Nama versi aplikasi terpasang (mis. "3.0.0"). */
    fun appVersionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0.0"
    } catch (e: Exception) {
        "0.0.0"
    }

    companion object {
        lateinit var instance: ObillApp
            private set
    }
}
