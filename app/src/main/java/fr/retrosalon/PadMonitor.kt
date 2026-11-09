package fr.retrosalon

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/** Une même manette n'est pas relue plus souvent (retour dans l'app, réveils rapprochés). */
private const val MIN_READ_GAP_MS = 15_000L

/**
 * Suit les manettes connectées et leur batterie, tant que l'app est visible (start/stop).
 *
 * Deux sources de batterie :
 * - InputDevice.getBatteryState() (Android 12+), sans autorisation : DualShock 4, DualSense et les
 *   manettes dont le pilote du noyau lit la batterie ;
 * - le service Batterie Bluetooth (GATT 0x180F) de la manette, pour les manettes Bluetooth LE comme
 *   la Xbox, dont Android ne transmet pas le niveau aux applis. Il faut l'autorisation
 *   BLUETOOTH_CONNECT (« Appareils à proximité ») sur Android 12+, BLUETOOTH avant.
 *
 * Tout l'état est lu et modifié sur le fil principal : [scope] doit y tourner (viewModelScope), et
 * les rappels Bluetooth y arrivent. Seule la lecture de la batterie du noyau se fait ailleurs.
 */
class PadMonitor(private val app: Application, private val scope: CoroutineScope) {
    private class Device(val id: Int, val name: String, val descriptor: String, val vendorId: Int, val productId: Int)

    private class KernelRead(val battery: PadBattery?, val slow: Boolean)

    /** Connexion au service Batterie d'une manette. [sure] : voir [Gamepads.Match]. */
    private class Connection(val client: BatteryServiceClient, val address: String, val sure: Boolean)

    private val inputManager = app.getSystemService(InputManager::class.java)

    private var devices: List<Device> = listDevices()
    private val _pads = MutableStateFlow(emptyList<Pad>())

    /** Manettes connectées, avec leur batterie quand elle est connue. */
    val pads: StateFlow<List<Pad>> = _pads.asStateFlow()

    /** Autorisation nécessaire pour lire la batterie Bluetooth. */
    val bluetoothPermission: String =
        if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH

    private val _bluetoothGranted = MutableStateFlow(bluetoothGranted())
    val bluetoothGranted: StateFlow<Boolean> = _bluetoothGranted.asStateFlow()

    private val _bluetoothDenied = MutableStateFlow(false)

    /** L'utilisateur a répondu « Refuser » à la dernière demande d'autorisation Bluetooth. */
    val bluetoothDenied: StateFlow<Boolean> = _bluetoothDenied.asStateFlow()

    // Par identifiant de manette (descriptor) : batteries connues, manettes déjà interrogées depuis
    // leur connexion, dernière lecture, pause après une lecture lente.
    private val kernel = HashMap<String, PadBattery>()
    private val bluetooth = HashMap<String, PadBattery>()
    private val checked = HashSet<String>()
    private val lastReadAt = HashMap<String, Long>()
    private val slowUntil = HashMap<String, Long>()
    private val connections = HashMap<String, Connection>()
    private val gattRetryAt = HashMap<String, Long>()

    // Heure d'arrivée, par numéro de périphérique (une manette reconnectée change de numéro).
    private val addedAt = HashMap<Int, Long>()

    // Lectures de batterie du noyau : un seul fil, jamais le fil principal (une lecture peut durer
    // plusieurs secondes sur certaines manettes).
    private val batteryThread = Dispatchers.IO.limitedParallelism(1)

    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var job: Job? = null

    private val listener = object : InputManager.InputDeviceListener {
        // Une manette qui arrive s'affiche tout de suite ; la boucle recalcule quand lire sa batterie.
        override fun onInputDeviceAdded(deviceId: Int) {
            if (sync()) wake.trySend(Unit)
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            sync()
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            if (sync()) wake.trySend(Unit)
        }
    }

    init {
        publish()
    }

    fun start() {
        if (job != null) return
        wake.tryReceive()
        inputManager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        job = scope.launch {
            while (isActive) {
                refresh()
                withTimeoutOrNull(nextWakeMs()) { wake.receive() }
            }
        }
    }

    fun stop() {
        inputManager.unregisterInputDeviceListener(listener)
        job?.cancel()
        job = null
        // Libère le Bluetooth pendant le jeu. Le dernier niveau reste affiché jusqu'à la prochaine lecture.
        connections.values.forEach { it.client.close() }
        connections.clear()
    }

    /** Réponse à la demande d'autorisation Bluetooth. */
    fun permissionResult(granted: Boolean) {
        _bluetoothDenied.value = !granted
        permissionChanged()
    }

    /** À appeler au retour dans l'app : l'autorisation a pu changer dans les paramètres de la TV. */
    fun permissionChanged() {
        if (updateGranted()) wake.trySend(Unit)
    }

    /** Relit l'autorisation Bluetooth ; vrai si elle a changé. */
    private fun updateGranted(): Boolean {
        val granted = bluetoothGranted()
        if (granted) _bluetoothDenied.value = false
        if (granted == _bluetoothGranted.value) return false
        _bluetoothGranted.value = granted
        gattRetryAt.clear()
        return true
    }

    private fun bluetoothGranted() = app.checkSelfPermission(bluetoothPermission) == PackageManager.PERMISSION_GRANTED

    /**
     * Intervalle entre deux lectures. Android 12-13 lit le niveau en bloquant toute la saisie du
     * système, d'où un intervalle plus long.
     */
    private fun pollPeriodMs() = if (Build.VERSION.SDK_INT in 31..33) 120_000L else 60_000L

    /**
     * Attente avant le prochain passage : l'intervalle normal, ou moins si une manette pas encore
     * interrogée vient d'arriver (y compris si son délai a expiré pendant le passage précédent).
     */
    private fun nextWakeMs(): Long {
        val now = SystemClock.uptimeMillis()
        val settling = devices
            .filter { it.descriptor !in checked }
            .mapNotNull { d -> addedAt[d.id]?.let { (it + ADDED_DELAY_MS - now).coerceAtLeast(0L) } }
            .minOrNull()
        return minOf(pollPeriodMs(), settling?.plus(SETTLE_MARGIN_MS) ?: Long.MAX_VALUE)
    }

    private suspend fun refresh() {
        sync()
        val now = SystemClock.uptimeMillis()
        // Une manette qui vient d'arriver n'est pas encore interrogée : avant d'avoir envoyé sa
        // batterie, la lecture peut bloquer plusieurs secondes.
        val settled = devices.filter { d -> addedAt[d.id]?.let { now - it >= ADDED_DELAY_MS } ?: true }
        if (Build.VERSION.SDK_INT >= 31) {
            val due = settled.filter { d ->
                (lastReadAt[d.descriptor]?.let { now - it >= MIN_READ_GAP_MS } ?: true) &&
                    (slowUntil[d.descriptor] ?: 0L) <= now
            }
            if (due.isNotEmpty()) {
                // La lecture ne s'interrompt pas : même si l'app passe en arrière-plan entre-temps, son
                // résultat compte pour l'intervalle minimal et la pause après une lecture lente.
                val read = withContext(batteryThread + NonCancellable) { due.map { it to readKernel(it) } }
                val done = SystemClock.uptimeMillis()
                val live = devices.map { it.id }.toSet()
                for ((d, result) in read) {
                    if (d.id !in live) continue // partie ou reconnectée pendant la lecture
                    if (result.battery != null) kernel[d.descriptor] = result.battery else kernel.remove(d.descriptor)
                    // Une manette qui met plus d'une seconde à répondre n'est plus interrogée pendant 10 minutes.
                    if (result.slow) slowUntil[d.descriptor] = done + SLOW_BACKOFF_MS
                    lastReadAt[d.descriptor] = done
                }
                // Arrêté pendant la lecture : n'ouvre aucune connexion Bluetooth.
                currentCoroutineContext().ensureActive()
            }
        }
        val live = devices.map { it.id }.toSet()
        settled.filter { it.id in live }.forEach { checked.add(it.descriptor) }
        syncBluetooth()
        publish()
    }

    /**
     * Met à jour la liste des manettes ; vrai si elle a changé. Une manette nouvelle ou reconnectée
     * (nouveau numéro) repart de zéro, y compris quand elle est arrivée pendant le jeu.
     */
    private fun sync(): Boolean {
        val before = devices.associate { it.descriptor to it.id }
        devices = listDevices()
        val current = devices.associate { it.descriptor to it.id }
        val now = SystemClock.uptimeMillis()
        (before.keys - current.keys).forEach(::forget)
        for ((descriptor, id) in current) {
            if (before[descriptor] == id) continue
            forget(descriptor)
            addedAt.putIfAbsent(id, now)
        }
        addedAt.keys.retainAll(current.values.toSet())
        updateGranted()
        syncBluetooth()
        publish()
        return before != current
    }

    /** Oublie tout ce qui concerne une manette partie ou reconnectée. */
    private fun forget(descriptor: String) {
        kernel.remove(descriptor)
        bluetooth.remove(descriptor)
        checked.remove(descriptor)
        lastReadAt.remove(descriptor)
        slowUntil.remove(descriptor)
        gattRetryAt.remove(descriptor)
        connections.remove(descriptor)?.client?.close()
    }

    private fun listDevices(): List<Device> =
        inputManager.inputDeviceIds.toList()
            .mapNotNull { runCatching { inputManager.getInputDevice(it) }.getOrNull() }
            .filter { !it.isVirtual && (it.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD }
            .distinctBy { it.descriptor }
            .map { Device(it.id, it.name, it.descriptor, it.vendorId, it.productId) }

    @RequiresApi(31)
    private fun readKernel(d: Device): KernelRead {
        if (!Gamepads.kernelBatteryTrusted(d.vendorId, d.productId)) return KernelRead(null, slow = false)
        val start = SystemClock.uptimeMillis()
        val battery = runCatching {
            val state = inputManager.getInputDevice(d.id)?.batteryState ?: return@runCatching null
            Gamepads.fromBatteryState(state.isPresent, state.capacity, state.status)
        }.getOrNull()
        return KernelRead(battery, slow = SystemClock.uptimeMillis() - start > SLOW_READ_MS)
    }

    /**
     * Ouvre ou ferme les connexions au service Batterie Bluetooth : une par manette déjà interrogée
     * côté Android sans qu'Android donne de pourcentage.
     */
    @SuppressLint("MissingPermission")
    private fun syncBluetooth() {
        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
        val usable = bluetoothGranted() && adapter != null && runCatching { adapter.isEnabled }.getOrDefault(false)
        val wanted = if (usable) {
            devices.filter { it.descriptor in checked && kernel[it.descriptor]?.percent == null }
        } else {
            emptyList()
        }
        val wantedKeys = wanted.map { it.descriptor }.toSet()
        val now = SystemClock.uptimeMillis()

        // Connexions à fermer : manette partie, batterie lue autrement, ou pas de réponse à temps.
        connections.keys.toList().forEach { key ->
            val client = connections.getValue(key).client
            val timedOut = !client.hasLevel && now - client.openedAt > GATT_TIMEOUT_MS
            if (key !in wantedKeys || timedOut) drop(key, failed = timedOut)
        }
        if (wanted.isEmpty() || adapter == null) return

        val bonded by lazy {
            runCatching {
                adapter.bondedDevices.orEmpty().map { Gamepads.Bonded(it.address, it.name, transport(it.type)) to it }
            }.getOrDefault(emptyList())
        }
        for (d in wanted) {
            val existing = connections[d.descriptor]
            if (existing != null) {
                existing.client.refresh()
                continue
            }
            if ((gattRetryAt[d.descriptor] ?: 0L) > now) continue
            // Le nom ne départage rien si une autre manette connectée porte le même.
            val nameIsUnique = devices.none { it !== d && it.name == d.name }
            val match = Gamepads.matchBonded(
                d.name, d.descriptor, d.vendorId, d.productId, bonded.map { it.first }, Build.VERSION.SDK_INT, nameIsUnique,
            )
            val device = match?.let { m -> bonded.firstOrNull { it.first == m.bonded }?.second }
            if (match == null || device == null || connections.values.any { it.address == match.bonded.address }) {
                // Aucun appareil Bluetooth LE ne correspond (manette USB, Bluetooth classique…), ou il est
                // déjà celui d'une autre manette : on ne recherche plus avant un moment.
                gattRetryAt[d.descriptor] = now + GATT_RETRY_MS
                continue
            }
            lateinit var client: BatteryServiceClient
            client = BatteryServiceClient(
                app, device,
                onLevel = { battery ->
                    if (connections[d.descriptor]?.client === client) {
                        bluetooth[d.descriptor] = battery
                        publish()
                    }
                },
                onFailed = { permanent ->
                    if (connections[d.descriptor]?.client === client) {
                        drop(d.descriptor, failed = true, permanent = permanent)
                        publish()
                    }
                },
            )
            connections[d.descriptor] = Connection(client, match.bonded.address, match.sure)
            client.open()
        }
    }

    /**
     * Ferme la connexion Bluetooth d'une manette. Après un échec passager, RetroSalon réessaie plus
     * tard si la manette est sûre ; sinon (pas de service Batterie, appareil incertain) seulement
     * quand elle se reconnecte.
     */
    private fun drop(descriptor: String, failed: Boolean, permanent: Boolean = false) {
        val connection = connections.remove(descriptor) ?: return
        connection.client.close()
        bluetooth.remove(descriptor)
        if (failed) {
            gattRetryAt[descriptor] =
                if (connection.sure && !permanent) SystemClock.uptimeMillis() + GATT_RETRY_MS else Long.MAX_VALUE
        }
    }

    private fun transport(type: Int) = when (type) {
        BluetoothDevice.DEVICE_TYPE_LE -> Gamepads.Transport.LE
        BluetoothDevice.DEVICE_TYPE_DUAL -> Gamepads.Transport.DUAL
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> Gamepads.Transport.CLASSIC
        else -> Gamepads.Transport.UNKNOWN
    }

    private fun publish() {
        _pads.value = devices.map {
            Pad(
                it.name, it.descriptor,
                Gamepads.merge(kernel[it.descriptor], bluetooth[it.descriptor]),
                batteryPending = it.descriptor !in checked,
            )
        }
    }

    private companion object {
        const val ADDED_DELAY_MS = 5_000L
        const val SETTLE_MARGIN_MS = 100L
        const val SLOW_READ_MS = 1_000L
        const val SLOW_BACKOFF_MS = 10 * 60_000L
        const val GATT_TIMEOUT_MS = 30_000L
        const val GATT_RETRY_MS = 5 * 60_000L
    }
}

/**
 * Lit le niveau du service Batterie Bluetooth d'une manette déjà connectée, puis suit ses
 * notifications. La connexion de la manette elle-même (HID) n'est pas touchée : fermer ce client
 * libère seulement l'accès de RetroSalon.
 *
 * Fil principal uniquement : les rappels Bluetooth y sont envoyés (Handler passé à connectGatt).
 */
@SuppressLint("MissingPermission")
internal class BatteryServiceClient(
    private val context: Context,
    private val device: BluetoothDevice,
    private val onLevel: (PadBattery) -> Unit,
    /** [permanent] : la manette n'a pas de niveau lisible (pas de service Batterie, valeur invalide). */
    private val onFailed: (permanent: Boolean) -> Unit,
) {
    val openedAt = SystemClock.uptimeMillis()

    var hasLevel = false
        private set

    private var closed = false
    private var gatt: BluetoothGatt? = null
    private var level: BluetoothGattCharacteristic? = null

    // Une seule opération GATT à la fois.
    private var busy = false
    private var subscribed = false
    private var lastReadAt = 0L

    fun open() {
        gatt = runCatching {
            device.connectGatt(
                context, false, callback, BluetoothDevice.TRANSPORT_LE,
                BluetoothDevice.PHY_LE_1M_MASK, Handler(Looper.getMainLooper()),
            )
        }.getOrNull()
        if (gatt == null) fail()
    }

    /** Relit le niveau, pour les manettes qui n'envoient pas de notification. */
    fun refresh() {
        val g = gatt ?: return
        val c = level ?: return
        val now = SystemClock.uptimeMillis()
        if (closed || busy || now - lastReadAt < MIN_READ_GAP_MS) return
        read(g, c)
    }

    fun close() {
        closed = true
        runCatching { gatt?.close() }
        gatt = null
    }

    private fun read(g: BluetoothGatt, c: BluetoothGattCharacteristic): Boolean {
        lastReadAt = SystemClock.uptimeMillis()
        busy = runCatching { g.readCharacteristic(c) }.getOrDefault(false)
        return busy
    }

    private fun fail(permanent: Boolean = false) {
        if (closed) return
        close()
        onFailed(permanent)
    }

    // Sur Android 12-14, un client fermé avant la fin de son enregistrement est quand même connecté
    // ensuite : ses rappels arrivent encore, il faut le refermer.
    private fun closeLate(g: BluetoothGatt) {
        runCatching { g.close() }
    }

    // Selon la version d'Android, le système appelle soit les variantes avec « value » (API 33+), soit
    // les anciennes : chacune ne sert qu'une fois, sans appel à super.
    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (closed) return closeLate(g)
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                if (!runCatching { g.discoverServices() }.getOrDefault(false)) fail()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                fail()
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (closed) return closeLate(g)
            if (status != BluetoothGatt.GATT_SUCCESS) return fail()
            val c = g.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_LEVEL) ?: return fail(permanent = true)
            level = c
            if (!read(g, c)) fail()
        }

        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            onRead(g, c, value, status)
        }

        @Deprecated("Appelée avant Android 13 (API 33)")
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            @Suppress("DEPRECATION")
            onRead(g, c, c.value, status)
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            onChanged(c, value)
        }

        @Deprecated("Appelée avant Android 13 (API 33)")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            onChanged(c, c.value)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            busy = false
        }
    }

    private fun onRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray?, status: Int) {
        busy = false
        if (closed || c.uuid != BATTERY_LEVEL) return
        val ok = status == BluetoothGatt.GATT_SUCCESS
        val battery = if (ok) Gamepads.fromBatteryLevel(value) else null
        if (battery != null) {
            hasLevel = true
            onLevel(battery)
        } else if (!hasLevel) {
            // Lu sans erreur mais hors de 0-100 (0xFF = inconnu) : la manette ne donne pas son niveau.
            return fail(permanent = ok)
        }
        if (!subscribed) {
            subscribed = true
            subscribe(g, c)
        }
    }

    private fun onChanged(c: BluetoothGattCharacteristic, value: ByteArray?) {
        if (closed || c.uuid != BATTERY_LEVEL) return
        Gamepads.fromBatteryLevel(value)?.let {
            hasLevel = true
            onLevel(it)
        }
    }

    /** Demande les notifications de niveau, si la manette les propose. */
    private fun subscribe(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
        if (c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) return
        val d = c.getDescriptor(CLIENT_CONFIG) ?: return
        runCatching {
            if (!g.setCharacteristicNotification(c, true)) return
            busy = if (Build.VERSION.SDK_INT >= 33) {
                g.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(d)
            }
        }
    }

    private companion object {
        val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
