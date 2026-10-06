package com.thanu.steady

import android.app.Application
import com.thanu.steady.di.AppContainer

class SteadyApplication : Application() {
    val container by lazy { AppContainer(applicationContext) }
}
