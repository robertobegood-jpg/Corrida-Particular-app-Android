package com.corridaparticular.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.corridaparticular.app.data.Repo

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_TAXIMETRO, "Taxímetro em andamento", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Mostra o valor da corrida enquanto o taxímetro está ligado"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_AGENDA, "Lembretes de corridas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisa antes das corridas agendadas"
            }
        )
    }

    companion object {
        const val CANAL_TAXIMETRO = "taximetro"
        const val CANAL_AGENDA = "agenda"
    }
}
