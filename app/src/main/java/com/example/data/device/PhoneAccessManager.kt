package com.example.data.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.os.StatFs
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import com.example.data.workspace.FileNode
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class DeviceLocationSnapshot(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val provider: String,
  val timestampMs: Long,
) {
  fun formattedTime(): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestampMs))

  fun toJsonString(): String =
    """
    {
      "latitude": $latitude,
      "longitude": $longitude,
      "accuracyMeters": $accuracyMeters,
      "provider": "$provider",
      "timestampMs": $timestampMs
    }
    """.trimIndent() + "\n"
}

data class DeviceContextSnapshot(
  val networkSummary: String,
  val freeStorageMb: Long,
  val hardwareKeyboardConnected: Boolean,
  val pointerDeviceSummary: String,
  val locationPermissionGranted: Boolean,
)

class PhoneAccessManager(private val context: Context) {

  fun persistTreePermission(treeUri: Uri): String {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    runCatching { context.contentResolver.takePersistableUriPermission(treeUri, flags) }
    val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
    val friendly =
      docId?.substringAfter(':', docId)?.substringAfterLast('/')?.ifBlank { null }
        ?: treeUri.lastPathSegment?.substringAfterLast(':')
        ?: "Phone Folder"
    return friendly
  }

  suspend fun listPhoneTree(treeUriString: String): List<FileNode> =
    withContext(Dispatchers.IO) {
      val treeUri = Uri.parse(treeUriString)
      val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
      queryChildrenRecursive(treeUri, rootDocId, prefix = "", depth = 0)
    }

  private fun queryChildrenRecursive(
    treeUri: Uri,
    parentDocId: String,
    prefix: String,
    depth: Int,
  ): List<FileNode> {
    if (depth > 3) return emptyList()
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    val projection =
      arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
      )
    val nodes = mutableListOf<FileNode>()
    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
      val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
      val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
      val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
      val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
      val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

      while (cursor.moveToNext()) {
        val docId = if (idCol >= 0) cursor.getString(idCol) else continue
        val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "unknown" else "unknown"
        val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
        val size = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
        val mod = if (modCol >= 0 && !cursor.isNull(modCol)) cursor.getLong(modCol) else 0L
        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
        val rel = if (prefix.isEmpty()) name else "$prefix/$name"
        val children =
          if (isDir) queryChildrenRecursive(treeUri, docId, rel, depth + 1) else emptyList()
        nodes +=
          FileNode(
            relativePath = rel,
            name = name,
            isDirectory = isDir,
            sizeBytes = size,
            lastModified = mod,
            children = children,
            documentId = docId,
          )
      }
    }
    return nodes.sortedWith(compareBy<FileNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
  }

  suspend fun readPhoneDocument(treeUriString: String, documentId: String): String =
    withContext(Dispatchers.IO) {
      val docUri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(treeUriString), documentId)
      context.contentResolver.openInputStream(docUri)?.use { input ->
        input.bufferedReader(Charsets.UTF_8).readText()
      } ?: throw IOException("Unable to read selected phone file")
    }

  suspend fun writePhoneDocument(treeUriString: String, documentId: String, content: String): Unit =
    withContext(Dispatchers.IO) {
      val docUri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(treeUriString), documentId)
      context.contentResolver.openOutputStream(docUri, "wt")?.use { out ->
        out.write(content.toByteArray(Charsets.UTF_8))
      } ?: throw IOException("Unable to write to phone file (read-only provider or revoked access)")
    }

  suspend fun createPhoneDocument(
    treeUriString: String,
    parentDocId: String?,
    name: String,
    isDirectory: Boolean,
  ): String =
    withContext(Dispatchers.IO) {
      val treeUri = Uri.parse(treeUriString)
      val targetParentId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
      val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, targetParentId)
      val mime =
        if (isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else guessMimeType(name)
      val createdUri =
        DocumentsContract.createDocument(context.contentResolver, parentUri, mime, name.trim())
          ?: throw IOException("Provider refused creating $name")
      DocumentsContract.getDocumentId(createdUri)
    }

  suspend fun renamePhoneDocument(
    treeUriString: String,
    documentId: String,
    newName: String,
  ): Unit =
    withContext(Dispatchers.IO) {
      val docUri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(treeUriString), documentId)
      DocumentsContract.renameDocument(context.contentResolver, docUri, newName.trim())
        ?: throw IOException("Failed to rename document")
    }

  suspend fun deletePhoneDocument(treeUriString: String, documentId: String): Unit =
    withContext(Dispatchers.IO) {
      val docUri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(treeUriString), documentId)
      if (!DocumentsContract.deleteDocument(context.contentResolver, docUri)) {
        throw IOException("Failed to delete document")
      }
    }

  fun hasLocationPermission(): Boolean {
    val coarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val fine =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return coarse || fine
  }

  @SuppressLint("MissingPermission")
  suspend fun fetchCurrentLocation(): DeviceLocationSnapshot? {
    if (!hasLocationPermission()) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers =
      listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

    val activeProvider =
      providers.firstOrNull { it == LocationManager.NETWORK_PROVIDER || it == LocationManager.GPS_PROVIDER }
        ?: providers.firstOrNull()

    val fresh: Location? =
      if (activeProvider != null) {
        withTimeoutOrNull(4000L) {
          suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              val signal = CancellationSignal()
              cont.invokeOnCancellation { signal.cancel() }
              runCatching {
                lm.getCurrentLocation(
                  activeProvider,
                  signal,
                  ContextCompat.getMainExecutor(context),
                ) { loc ->
                  if (cont.isActive) cont.resume(loc)
                }
              }.onFailure {
                if (cont.isActive) cont.resume(null)
              }
            } else {
              @Suppress("DEPRECATION")
              val listener =
                object : LocationListener {
                  override fun onLocationChanged(location: Location) {
                    if (cont.isActive) cont.resume(location)
                  }
                  override fun onProviderEnabled(provider: String) {}
                  override fun onProviderDisabled(provider: String) {}
                  @Deprecated("Deprecated in Java")
                  override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
              @Suppress("DEPRECATION")
              runCatching {
                lm.requestSingleUpdate(activeProvider, listener, Looper.getMainLooper())
              }.onFailure {
                if (cont.isActive) cont.resume(null)
              }
            }
          }
        }
      } else null

    val best =
      fresh
        ?: providers
          .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
          .maxByOrNull { it.time }

    return best?.let {
      DeviceLocationSnapshot(
        latitude = it.latitude,
        longitude = it.longitude,
        accuracyMeters = it.accuracy,
        provider = it.provider ?: "location",
        timestampMs = it.time,
      )
    }
  }

  fun inspectDeviceContext(): DeviceContextSnapshot {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val network = cm?.activeNetwork
    val caps = if (network != null) cm.getNetworkCapabilities(network) else null
    val netText =
      when {
        caps == null -> "Offline"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Connected"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet Connected"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Connected"
        else -> "Online"
      }

    val stat = StatFs(context.filesDir.absolutePath)
    val freeMb = stat.availableBytes / (1024L * 1024L)

    val config = context.resources.configuration
    val hasHwKeyboard =
      config.keyboard == Configuration.KEYBOARD_QWERTY &&
        config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
    val pointerText =
      when (config.navigation) {
        Configuration.NAVIGATION_DPAD -> "D-Pad / External Navigation"
        Configuration.NAVIGATION_TRACKBALL -> "Trackball / Mouse"
        else -> if (config.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH) "Touch + Mouse Ready" else "Mouse / Non-Touch"
      }

    return DeviceContextSnapshot(
      networkSummary = netText,
      freeStorageMb = freeMb,
      hardwareKeyboardConnected = hasHwKeyboard,
      pointerDeviceSummary = pointerText,
      locationPermissionGranted = hasLocationPermission(),
    )
  }

  private fun guessMimeType(name: String): String =
    when (name.substringAfterLast('.', "").lowercase()) {
      "py", "js", "ts", "md", "txt", "sh", "json", "css", "html" -> "text/plain"
      else -> "application/octet-stream"
    }
}
