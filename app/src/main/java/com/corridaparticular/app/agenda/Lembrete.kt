package com.corridaparticular.app.agenda

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.corridaparticular.app.App
import com.corridaparticular.app.MainActivity
import com.corridaparticular.app.R
import com.corridaparticular.app.data.Corrida
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.util.horaTexto
import com.corridaparticular.app.util.reais

/** Lembretes de corridas agendadas (notificação X minutos antes). */
object Lembrete {

    private fun pi(ctx: Context, id: String): PendingIntent = PendingIntent.getBroadcast(
        ctx, id.hashCode(),
        Intent(ctx, LembreteReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Agenda ou cancela o lembrete conforme o status da corrida. */
    fun sincronizar(ctx: Context, c: Corrida) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val quando = c.dataHora - Repo.config.value.lembreteMin * 60_000L
        if (c.status != Corrida.STATUS_AGENDADA || quando <= System.currentTimeMillis()) {
            am.cancel(pi(ctx, c.id))
            return
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi(ctx, c.id))
    }

    fun cancelar(ctx: Context, id: String) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pi(ctx, id))
    }

    fun reagendarTodas(ctx: Context) {
        Repo.corridas.value.forEach { sincronizar(ctx, it) }
    }

    /** Salva a corrida e ajusta o lembrete. */
    fun salvar(ctx: Context, c: Corrida) {
        Repo.salvarCorrida(c)
        sincronizar(ctx, c)
    }

    fun excluir(ctx: Context, id: String) {
        cancelar(ctx, id)
        Repo.excluirCorrida(id)
    }
}

class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Repo.init(ctx)
        val c = Repo.corrida(intent.getStringExtra("id")) ?: return
        if (c.status != Corrida.STATUS_AGENDADA) return
        val abrir = PendingIntent.getActivity(
            ctx, 2,
            Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_ABA, "AGENDA")
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, App.CANAL_AGENDA)
            .setSmallIcon(R.drawable.ic_stat_taxi)
            .setContentTitle("Corrida às ${c.dataHora.horaTexto()} – ${c.clienteNome}")
            .setContentText("${c.origem.texto} → ${c.destino.texto}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Origem: ${c.origem.texto}\nDestino: ${c.destino.texto}\nValor: ${c.valor.reais()}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(c.id.hashCode(), n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Repo.init(ctx)
            Lembrete.reagendarTodas(ctx)
        }
    }
}
