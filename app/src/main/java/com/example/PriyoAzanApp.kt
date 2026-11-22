package com.example

import android.app.Application
import com.example.azan.AzanEngine
import com.example.database.PriyoAzanDatabase
import com.example.database.PriyoAzanRepository
import com.example.location.LocationService
import com.example.quran.QuranRepository
import com.example.sensors.CompassSensorManager
import com.example.updater.GitHubUpdateManager

class PriyoAzanApp : Application() {
    val database: PriyoAzanDatabase by lazy { PriyoAzanDatabase.getInstance(this) }
    val repository: PriyoAzanRepository by lazy { PriyoAzanRepository(database) }
    val azanEngine: AzanEngine by lazy { AzanEngine(this, repository) }
    val locationService: LocationService by lazy { LocationService(this) }
    val compassSensorManager: CompassSensorManager by lazy { CompassSensorManager(this) }
    val quranRepository: QuranRepository by lazy { QuranRepository(this) }
    val updateManager: GitHubUpdateManager by lazy { GitHubUpdateManager(this) }
}
