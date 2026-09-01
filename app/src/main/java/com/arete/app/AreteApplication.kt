package com.arete.app

import android.app.Application
import com.arete.app.data.AreteRepository
import com.arete.app.data.CatalogSeeder
import com.arete.app.data.PreferencesRepository
import com.arete.app.data.local.AreteDatabase

class AreteApplication : Application() {
    val database: AreteDatabase by lazy { AreteDatabase.create(this) }
    val repository: AreteRepository by lazy {
        AreteRepository(database, CatalogSeeder(this, database))
    }
    val preferences: PreferencesRepository by lazy { PreferencesRepository(this) }
}
