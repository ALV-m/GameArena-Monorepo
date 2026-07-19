package com.gamearena.booster.metrics

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.view.Choreographer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

internal data class CpuTimes(val total: Long, val idle: Long)

internal object CpuStatParser {
    fun parseTotalCpuLine(lines: List<String>): CpuTimes? {
        val parts = lines.firstOrNull()
            ?.trim()
            ?.split("\\s+".toRegex())
            ?: return null

        if (parts.size < 5 || parts[0] != "cpu") return null

        val values = parts.drop(1).map { it.toLongOrNull() ?: return null }
        val user = values.getOrNull(0) ?: return null
        val nice = values.getOrNull(1) ?: return null
        val system = values.getOrNull(2) ?: return null
        val idle = values.getOrNull(3) ?: return null
        val iowait = values.getOrNull(4) ?: 0L
        val irq = values.getOrNull(5) ?: 0L
        val softirq = values.getOrNull(6) ?: 0L
        val steal = values.getOrNull(7) ?: 0L

        return CpuTimes(
            total = user + nice + system + idle + iowait + irq + softirq + steal,
            idle = idle + iowait
        )
    }

    fun calculateUsage(previous: CpuTimes, current: CpuTimes): Int? {
        val diffTotal = current.total - previous.total
        val diffIdle = current.idle - previous.idle
        if (diffTotal <= 0L || diffIdle < 0L) return null

        return (((diffTotal - diffIdle).toFloat() / diffTotal) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }
}

@Singleton
class FpsMonitor @Inject constructor() {
    data class FpsState(val fps: Int, val jankyFrames: Int)

    private var frameCount = 0
    private var lastTimestamp = 0L
    private var lastFps = 0

    val fpsState: Flow<FpsState> = callbackFlow {
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastTimestamp == 0L) {
                    lastTimestamp = frameTimeNanos
                }

                frameCount++
                val elapsed = frameTimeNanos - lastTimestamp

                if (elapsed >= 1_000_000_000L) {
                    lastFps = frameCount
                    frameCount = 0
                    lastTimestamp = frameTimeNanos
                    trySend(FpsState(lastFps, 0))
                }

                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        withContext(Dispatchers.Main) {
            Choreographer.getInstance().postFrameCallback(callback)
        }

        awaitClose {
            Choreographer.getInstance().removeFrameCallback(callback)
        }
    }.flowOn(Dispatchers.Main)
}

@Singleton
class CpuMonitor @Inject constructor() {
    data class CpuClusterState(val effMhz: Int, val perfMhz: Int, val ultraMhz: Int)
    private data class CpuPolicy(val currentMhz: Int, val maxMhz: Int)

    val cpuClusterUsage: Flow<CpuClusterState> = flow {
        while (true) {
            emit(readClusterState())
            delay(1000)
        }
    }

    val cpuUsage: Flow<Int> = flow {
        while (true) {
            emit(readFreq(0))
            delay(1000)
        }
    }

    val cpuPercentageUsage: Flow<Int?> = flow {
        var previousCpuTimes: CpuTimes? = null
        while (true) {
            try {
                val lines = try {
                    java.io.File("/proc/stat").readLines()
                } catch (e: Exception) {
                    emptyList()
                }

                val currentCpuTimes = CpuStatParser.parseTotalCpuLine(lines)
                val previous = previousCpuTimes
                previousCpuTimes = currentCpuTimes

                if (currentCpuTimes != null && previous != null) {
                    emit(CpuStatParser.calculateUsage(previous, currentCpuTimes))
                } else {
                    emit(null)
                }
            } catch (e: Exception) {
                emit(null)
            }
            delay(1000)
        }
    }

    private fun readClusterState(): CpuClusterState {
        val policies = readCpuPolicies().sortedBy { it.maxMhz }
        return when (policies.size) {
            0 -> CpuClusterState(0, 0, 0)
            1 -> CpuClusterState(policies[0].currentMhz, 0, 0)
            2 -> CpuClusterState(policies[0].currentMhz, policies[1].currentMhz, 0)
            else -> CpuClusterState(
                effMhz = policies.first().currentMhz,
                perfMhz = policies[policies.lastIndex - 1].currentMhz,
                ultraMhz = policies.last().currentMhz
            )
        }
    }

    private fun readCpuPolicies(): List<CpuPolicy> {
        val policyDir = java.io.File("/sys/devices/system/cpu/cpufreq")
        val policies = policyDir.listFiles { file -> file.isDirectory && file.name.startsWith("policy") }
            ?.mapNotNull { policy ->
                val current = readMhz(policy.resolve("scaling_cur_freq"))
                val max = readMhz(policy.resolve("cpuinfo_max_freq"))
                if (current > 0 || max > 0) CpuPolicy(current, max.coerceAtLeast(current)) else null
            }
            .orEmpty()

        if (policies.isNotEmpty()) return policies

        return java.io.File("/sys/devices/system/cpu")
            .listFiles { file -> file.isDirectory && file.name.matches(Regex("cpu\\d+")) }
            ?.mapNotNull { cpu ->
                val freqDir = cpu.resolve("cpufreq")
                val current = readMhz(freqDir.resolve("scaling_cur_freq"))
                val max = readMhz(freqDir.resolve("cpuinfo_max_freq"))
                if (current > 0 || max > 0) CpuPolicy(current, max.coerceAtLeast(current)) else null
            }
            .orEmpty()
            .distinctBy { it.maxMhz }
    }

    private fun readFreq(coreIndex: Int): Int {
        return readMhz(java.io.File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq"))
    }

    private fun readMhz(file: java.io.File): Int {
        return try {
            val raw = file.readText().trim()
            (raw.toIntOrNull() ?: 0) / 1000
        } catch (e: Exception) {
            0
        }
    }
}

@Singleton
class RamMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class RamState(val usedGb: Float, val totalGb: Float)

    val ramUsage: Flow<RamState> = flow {
        while (true) {
            val state = try {
                parseMemInfo()
            } catch (e: Exception) {
                fallbackRam(context)
            }
            emit(state)
            delay(2000)
        }
    }

    private fun parseMemInfo(): RamState {
        val file = java.io.File("/proc/meminfo")
        if (file.exists()) {
            val lines = file.readLines()
            var totalKb = 0f
            var availKb = -1f
            var freeKb = 0f
            var buffersKb = 0f
            var cachedKb = 0f
            for (line in lines) {
                val parts = line.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val valStr = parts[1].trim().split("\\s+".toRegex())[0].trim()
                    val value = valStr.toFloatOrNull() ?: 0f
                    when (key) {
                        "MemTotal" -> totalKb = value
                        "MemAvailable" -> availKb = value
                        "MemFree" -> freeKb = value
                        "Buffers" -> buffersKb = value
                        "Cached" -> cachedKb = value
                    }
                }
            }
            val usedKb = if (availKb >= 0f) {
                totalKb - availKb
            } else {
                totalKb - freeKb - buffersKb - cachedKb
            }
            return RamState(usedKb / (1024f * 1024f), totalKb / (1024f * 1024f))
        } else {
            return fallbackRam(context)
        }
    }

    private fun fallbackRam(context: Context): RamState {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return RamState(
            usedGb = (info.totalMem - info.availMem).toFloat() / (1024 * 1024 * 1024),
            totalGb = info.totalMem.toFloat() / (1024 * 1024 * 1024)
        )
    }
}

@Singleton
class NetworkMonitor @Inject constructor() {
    data class NetworkState(val rxSpeedKbps: Float, val txSpeedKbps: Float)

    val networkSpeed: Flow<NetworkState> = flow {
        if (TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED.toLong()) {
            while (true) { emit(NetworkState(0f, 0f)); delay(2000) }
            return@flow
        }
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        var previousTime = System.currentTimeMillis()

        while (true) {
            delay(1000)
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            val currentTime = System.currentTimeMillis()

            val timeDiffSec = (currentTime - previousTime) / 1000.0f
            if (timeDiffSec > 0 && currentRx >= 0 && currentTx >= 0) {
                val rxSpeedKbps = ((currentRx - previousRx) / 1024.0f) / timeDiffSec
                val txSpeedKbps = ((currentTx - previousTx) / 1024.0f) / timeDiffSec
                emit(NetworkState(
                    rxSpeedKbps.coerceAtLeast(0f),
                    txSpeedKbps.coerceAtLeast(0f)
                ))
            } else {
                emit(NetworkState(0f, 0f))
            }

            previousRx = currentRx
            previousTx = currentTx
            previousTime = currentTime
        }
    }
}

@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val batteryTemp: Flow<Float> = flow {
        while (true) {
            val intent = context.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val raw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            emit(raw / 10.0f)
            delay(5000)
        }
    }
}

@Singleton
class ThermalMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class ThermalState(
        val cpuC: Float = 0f,
        val gpuC: Float = 0f,
        val npuC: Float = 0f,
        val skinC: Float = 0f,
        val batteryC: Float = 0f,
        val status: Int = 0
    ) {
        val statusLabel: String get() = when (status) {
            0 -> "NONE"; 1 -> "LIGHT"; 2 -> "MODERATE"; 3 -> "SEVERE"
            4 -> "CRITICAL"; 5 -> "EMERGENCY"; 6 -> "SHUTDOWN"; else -> "?"
        }
    }

    val thermalState: Flow<ThermalState> = flow {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        while (true) {
            val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                powerManager.currentThermalStatus
            } else 0

            val intent = context.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val batteryTemp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f

            emit(ThermalState(
                batteryC = batteryTemp,
                cpuC = batteryTemp,
                status = status
            ))
            delay(5000)
        }
    }
}

@Singleton
class PingMonitor @Inject constructor() {
    val ping: Flow<Int> = flow {
        while (true) {
            val pingMs = try {
                val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "8.8.8.8"))
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (output.contains("time=")) {
                    output.split("time=").getOrNull(1)
                        ?.split(" ")?.getOrNull(0)
                        ?.toFloatOrNull()
                        ?.roundToInt() ?: 0
                } else 0
            } catch (e: Exception) {
                0
            }
            emit(pingMs)
            delay(20000)
        }
    }
}

@Singleton
class TopProcessMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class TopProcess(val name: String, val cpuPercent: Float)

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val topProcess: Flow<TopProcess?> = flow {
        while (true) {
            val result = try {
                val processes = activityManager.runningAppProcesses
                if (processes.isNullOrEmpty()) {
                    null
                } else {
                    processes
                        .filter { it.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE }
                        .maxByOrNull { it.importance }
                        ?.let { proc ->
                            TopProcess(
                                name = proc.processName.substringAfterLast('.'),
                                cpuPercent = 0f
                            )
                        }
                }
            } catch (e: Exception) {
                null
            }
            emit(result)
            delay(5000)
        }
    }
}

@Singleton
class ForegroundAppMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class ForegroundApp(val packageName: String, val label: String)

    val foregroundApp: Flow<ForegroundApp?> = flow {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val packageManager = context.packageManager

        while (true) {
            val result = try {
                if (usageStatsManager != null) {
                    val now = System.currentTimeMillis()
                    val stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        now - 5000,
                        now
                    )
                    stats?.filter { it.lastTimeUsed > 0 }
                        ?.maxByOrNull { it.lastTimeUsed }
                        ?.let { statsEntry ->
                            val label = try {
                                val ai = packageManager.getApplicationInfo(statsEntry.packageName, 0)
                                packageManager.getApplicationLabel(ai).toString()
                            } catch (_: Exception) {
                                statsEntry.packageName
                            }
                            ForegroundApp(statsEntry.packageName, label)
                        }
                } else null
            } catch (e: Exception) {
                null
            }
            emit(result)
            delay(2000)
        }
    }
}
