package com.corridaparticular.app.taximetro

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.corridaparticular.app.App
import com.corridaparticular.app.MainActivity
import com.corridaparticular.app.R
import com.corridaparticular.app.data.Calculo
import com.corridaparticular.app.data.Endereco
import com.corridaparticular.app.data.Repo
import com.corridaparticular.app.data.Tarifas
import com.corridaparticular.app.util.duracaoTexto
import com.corridaparticular.app.util.kmTexto
import com.corridaparticular.app.util.reais
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Estado do taxímetro, compartilhado entre o serviço e as telas. */
data class TaxiDados(
    val ativo: Boolean = false,
    val pausado: Boolean = false,
    val finalizado: Boolean = false,
    val inicio: Long = 0L,
    val fim: Long = 0L,
    val msTotal: Long = 0L,
    val msParado: Long = 0L,
    val metros: Double = 0.0,
    val velocidadeKmh: Double = 0.0,
    val bandeira2: Boolean = false,
    val valor: Double = 0.0,
    val gpsOk: Boolean = false,
    val precisao: Float = 0f,
    val clienteId: String? = null,
    val clienteNome: String = "",
    val telefone: String = "",
    val origem: Endereco = Endereco(),
    val paradas: List<Endereco> = emptyList(),
    val destino: Endereco = Endereco(),
    val agendadaId: String? = null,
    // Espera do passageiro (embarque ou paradas)
    val esperando: Boolean = false,
    val msEsperaSessao: Long = 0L,          // espera em andamento
    val msEsperaTotal: Long = 0L,           // toda a espera da corrida, inclusive a parte grátis
    val msEsperaCobradaFechada: Long = 0L,  // parte cobrável das esperas já encerradas
) {
    val km: Double get() = metros / 1000.0

    fun msEsperaCobrada(t: Tarifas): Long = msEsperaCobradaFechada + Calculo.msCobravel(t, msEsperaSessao)

    /** Tempo grátis que ainda resta na espera atual. */
    fun msGratisRestante(t: Tarifas): Long = (t.esperaGratisMin * 60_000L - msEsperaSessao).coerceAtLeast(0L)

    fun encerrarEspera(t: Tarifas): TaxiDados = if (!esperando) this else copy(
        esperando = false,
        msEsperaCobradaFechada = msEsperaCobradaFechada + Calculo.msCobravel(t, msEsperaSessao),
        msEsperaSessao = 0L,
    )

    fun recalcular(t: Tarifas): TaxiDados =
        copy(valor = Calculo.taximetro(t, km, msParado, msEsperaCobrada(t), bandeira2))
}

object TaximetroState {
    val dados = MutableStateFlow(TaxiDados())

    fun iniciar(ctx: Context, base: TaxiDados) {
        dados.value = base.copy(
            ativo = true, pausado = false, finalizado = false,
            inicio = System.currentTimeMillis(), msTotal = 0, msParado = 0, metros = 0.0,
            msEsperaSessao = 0, msEsperaTotal = 0, msEsperaCobradaFechada = 0,
        ).recalcular(Repo.tarifas.value)
        ContextCompat.startForegroundService(
            ctx, Intent(ctx, TaximetroService::class.java).setAction(TaximetroService.ACAO_INICIAR)
        )
    }

    fun comando(ctx: Context, acao: String) {
        ctx.startService(Intent(ctx, TaximetroService::class.java).setAction(acao))
    }

    fun definirBandeira2(ctx: Context, b2: Boolean) {
        dados.update { it.copy(bandeira2 = b2) }
        comando(ctx, TaximetroService.ACAO_ATUALIZAR)
    }

    /** Liga/desliga a contagem de espera do passageiro. */
    fun definirEspera(ctx: Context, esperar: Boolean) {
        val t = Repo.tarifas.value
        dados.update {
            if (esperar) it.copy(esperando = true, msEsperaSessao = 0L)
            else it.encerrarEspera(t).recalcular(t)
        }
        comando(ctx, TaximetroService.ACAO_ATUALIZAR)
    }

    fun limpar() {
        dados.value = TaxiDados()
    }
}

class TaximetroService : Service(), LocationListener {

    companion object {
        const val ACAO_INICIAR = "iniciar"
        const val ACAO_PAUSAR = "pausar"
        const val ACAO_RETOMAR = "retomar"
        const val ACAO_FINALIZAR = "finalizar"
        const val ACAO_ATUALIZAR = "atualizar"
        private const val NOTIF_ID = 1001

        /** Abaixo desta velocidade conta "tempo parado" (cobrado por hora parada). */
        private const val LIMIAR_PARADO_KMH = 10.0
        private const val PRECISAO_MAX_M = 35f
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null
    private var lm: LocationManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ultimoPonto: Location? = null
    private var ultimoFix = 0L
    private var ultimoTick = 0L
    private var contTick = 0
    private var rodando = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACAO_INICIAR -> iniciar()
            ACAO_PAUSAR -> {
                TaximetroState.dados.update { it.copy(pausado = true) }
                notificar()
            }
            ACAO_RETOMAR -> {
                ultimoPonto = null
                ultimoTick = SystemClock.elapsedRealtime()
                TaximetroState.dados.update { it.copy(pausado = false) }
                notificar()
            }
            ACAO_ATUALIZAR -> recalcular()
            ACAO_FINALIZAR -> finalizar()
            else -> if (!rodando) stopSelf()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun iniciar() {
        val d = TaximetroState.dados.value
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notificacao(d), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIF_ID, notificacao(d))
            }
        } catch (e: Exception) {
            TaximetroState.limpar()
            stopSelf()
            return
        }
        if (rodando) return
        rodando = true
        ultimoPonto = null
        ultimoFix = 0L
        ultimoTick = SystemClock.elapsedRealtime()

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CorridaParticular:taximetro")
            .apply { acquire(12 * 60 * 60 * 1000L) }

        try {
            lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
        } catch (e: SecurityException) {
            TaximetroState.dados.update { it.copy(gpsOk = false) }
        } catch (e: Exception) {
            TaximetroState.dados.update { it.copy(gpsOk = false) }
        }

        ticker = scope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
    }

    private fun tick() {
        val agora = SystemClock.elapsedRealtime()
        val dt = (agora - ultimoTick).coerceIn(0, 60_000)
        ultimoTick = agora
        TaximetroState.dados.update { d ->
            if (!d.ativo || d.pausado) d else {
                val semSinal = ultimoFix == 0L || agora - ultimoFix > 6000
                val vel = if (semSinal) 0.0 else d.velocidadeKmh
                // Durante a espera do passageiro o tempo vai para a espera, não para "parado".
                val parado = !d.esperando && vel < LIMIAR_PARADO_KMH
                d.copy(
                    msTotal = d.msTotal + dt,
                    msParado = d.msParado + if (parado) dt else 0L,
                    msEsperaSessao = d.msEsperaSessao + if (d.esperando) dt else 0L,
                    msEsperaTotal = d.msEsperaTotal + if (d.esperando) dt else 0L,
                    velocidadeKmh = vel,
                    gpsOk = !semSinal,
                ).recalcular(Repo.tarifas.value)
            }
        }
        if (++contTick % 3 == 0) notificar()
    }

    private fun recalcular() {
        TaximetroState.dados.update { d -> d.recalcular(Repo.tarifas.value) }
        notificar()
    }

    override fun onLocationChanged(loc: Location) {
        ultimoFix = SystemClock.elapsedRealtime()
        val d = TaximetroState.dados.value
        val precisaoBoa = !loc.hasAccuracy() || loc.accuracy <= PRECISAO_MAX_M
        val velGps = if (loc.hasSpeed()) loc.speed * 3.6 else null

        if (!d.ativo || d.pausado || !precisaoBoa) {
            TaximetroState.dados.update {
                it.copy(precisao = loc.accuracy, velocidadeKmh = velGps ?: it.velocidadeKmh, gpsOk = precisaoBoa)
            }
            return
        }

        var somar = 0.0
        var velCalc: Double? = null
        val ant = ultimoPonto
        if (ant == null) {
            ultimoPonto = loc
        } else {
            val dist = ant.distanceTo(loc).toDouble()
            val dtSeg = (loc.elapsedRealtimeNanos - ant.elapsedRealtimeNanos) / 1e9
            val vMs = if (dtSeg > 0) dist / dtSeg else 0.0
            velCalc = vMs * 3.6
            // Ignora "pulos" do GPS e a oscilação quando o carro está parado.
            val paradoGps = velGps != null && velGps < 3.0 && dist < 25
            if (dist >= 8 && vMs < 70 && !paradoGps) {
                somar = dist
                ultimoPonto = loc
            }
        }
        val vel = velGps ?: velCalc ?: 0.0
        TaximetroState.dados.update {
            it.copy(
                metros = it.metros + somar,
                velocidadeKmh = vel,
                gpsOk = true,
                precisao = loc.accuracy,
            ).recalcular(Repo.tarifas.value)
        }
    }

    override fun onProviderDisabled(provider: String) {
        TaximetroState.dados.update { it.copy(gpsOk = false) }
    }

    override fun onProviderEnabled(provider: String) {}

    @Deprecated("Necessário para Android 9 e anteriores")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun finalizar() {
        pararTudo()
        val t = Repo.tarifas.value
        TaximetroState.dados.update {
            it.encerrarEspera(t)
                .copy(ativo = false, pausado = false, finalizado = true, fim = System.currentTimeMillis())
                .recalcular(t)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pararTudo() {
        rodando = false
        ticker?.cancel()
        ticker = null
        runCatching { lm?.removeUpdates(this) }
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        pararTudo()
        scope.cancel()
        super.onDestroy()
    }

    // ---------- Notificação ----------

    private fun notificar() {
        if (!rodando) return
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, notificacao(TaximetroState.dados.value))
    }

    private fun acao(acao: String, codigo: Int): PendingIntent = PendingIntent.getService(
        this, codigo, Intent(this, TaximetroService::class.java).setAction(acao),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun notificacao(d: TaxiDados): Notification {
        val abrir = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_ABA, "TAXIMETRO")
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prefixo = when {
            d.pausado -> "⏸ Pausado • "
            d.esperando -> "⏳ Espera ${duracaoTexto(d.msEsperaSessao)} • "
            else -> ""
        }
        val titulo = prefixo + d.valor.reais()
        val texto = buildString {
            append(d.km.kmTexto()).append(" • ").append(duracaoTexto(d.msTotal))
            if (d.bandeira2) append(" • Bandeira 2")
            if (d.clienteNome.isNotBlank()) append(" • ").append(d.clienteNome)
        }
        val b = NotificationCompat.Builder(this, App.CANAL_TAXIMETRO)
            .setSmallIcon(R.drawable.ic_stat_taxi)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(abrir)
        if (d.pausado) b.addAction(0, "Retomar", acao(ACAO_RETOMAR, 11))
        else b.addAction(0, "Pausar", acao(ACAO_PAUSAR, 12))
        return b.build()
    }
}
