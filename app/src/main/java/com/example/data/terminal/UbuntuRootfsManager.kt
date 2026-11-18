package com.example.data.terminal

import android.content.Context
import android.os.Build
import android.os.Environment
import android.system.Os
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

sealed interface RootfsInstallState {
  data object Idle : RootfsInstallState
  data class Downloading(val downloadedBytes: Long, val totalBytes: Long, val fileName: String) :
    RootfsInstallState
  data object VerifyingChecksum : RootfsInstallState
  data class Extracting(val entriesCount: Int) : RootfsInstallState
  data class Ready(val rootfsDir: File, val verifiedSha256: String?) : RootfsInstallState
  data class Error(val message: String) : RootfsInstallState
}

data class ProotDiagnostics(
  val supportedAbis: List<String>,
  val isArm64Device: Boolean,
  val nativeLibraryDir: String,
  val prootBinaryPath: String?,
  val isProotAvailable: Boolean,
)

class UbuntuRootfsManager(
  private val context: Context,
  private val okHttpClient: OkHttpClient = OkHttpClient(),
) {
  val rootfsDir: File by lazy {
    File(context.filesDir, "ubuntu-arm64-rootfs").apply { if (!exists()) mkdirs() }
  }

  private val markerFile: File
    get() = File(rootfsDir, ".pocketcode_rootfs_sha256")

  private val _state =
    MutableStateFlow<RootfsInstallState>(
      if (isRootfsInstalled()) {
        RootfsInstallState.Ready(rootfsDir, readInstalledChecksum())
      } else {
        RootfsInstallState.Idle
      }
    )
  val state: StateFlow<RootfsInstallState> = _state.asStateFlow()

  fun inspectDiagnostics(): ProotDiagnostics {
    val abis = Build.SUPPORTED_ABIS.toList()
    // nativeLibraryDir can be null in some environments (e.g. unit tests); never crash the UI on it.
    val nativeDirPath: String? = context.applicationInfo.nativeLibraryDir
    val nativeDir = File(nativeDirPath ?: context.filesDir.absolutePath)
    val prootFile = File(nativeDir, "libproot.so")
    val available = prootFile.exists() && prootFile.canExecute()
    return ProotDiagnostics(
      supportedAbis = abis,
      isArm64Device = abis.any { it.contains("arm64") },
      nativeLibraryDir = nativeDir.absolutePath,
      prootBinaryPath = if (prootFile.exists()) prootFile.absolutePath else null,
      isProotAvailable = available,
    )
  }

  fun isRootfsInstalled(): Boolean =
    markerFile.exists() && File(rootfsDir, "bin/bash").exists()

  private fun readInstalledChecksum(): String? =
    if (markerFile.exists()) runCatching { markerFile.readText().trim() }.getOrNull() else null

  suspend fun downloadAndInstallUbuntuArm64(): Result<File> =
    withContext(Dispatchers.IO) {
      runCatching {
        val checksumsReq = Request.Builder().url(UBUNTU_SHA256SUMS_URL).build()
        val checksumsText =
          okHttpClient.newCall(checksumsReq).execute().use { resp ->
            if (!resp.isSuccessful) {
              throw IOException("Failed to fetch SHA256SUMS (HTTP ${resp.code})")
            }
            resp.body?.string() ?: throw IOException("Empty SHA256SUMS response")
          }

        val entryLine =
          checksumsText
            .lineSequence()
            .firstOrNull { line ->
              line.contains("ubuntu-base-") && line.contains("-base-arm64.tar.gz")
            }
            ?: throw IOException("Could not find arm64 rootfs entry in Ubuntu SHA256SUMS")

        val parts = entryLine.trim().split(Regex("""\s+\*?"""))
        if (parts.size < 2) throw IOException("Malformed SHA256SUMS line: $entryLine")
        val expectedSha256 = parts[0].lowercase()
        val archiveName = parts[1].removePrefix("*")
        val downloadUrl = "$UBUNTU_RELEASE_BASE_URL$archiveName"

        // cacheDir can be missing after the system clears app cache -> ENOENT on open.
        context.cacheDir.mkdirs()
        val archiveFile = File(context.cacheDir, archiveName)
        if (archiveFile.exists()) archiveFile.delete()

        val downloadReq = Request.Builder().url(downloadUrl).build()
        okHttpClient.newCall(downloadReq).execute().use { resp ->
          if (!resp.isSuccessful) {
            throw IOException("Failed to download $archiveName (HTTP ${resp.code})")
          }
          val body = resp.body ?: throw IOException("Empty rootfs download body")
          val totalBytes = body.contentLength()
          var downloaded = 0L
          val buffer = ByteArray(32 * 1024)
          body.byteStream().use { input ->
            FileOutputStream(archiveFile).use { output ->
              while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                downloaded += read
                _state.value = RootfsInstallState.Downloading(downloaded, totalBytes, archiveName)
              }
            }
          }
        }

        _state.value = RootfsInstallState.VerifyingChecksum
        val actualSha256 = computeSha256(archiveFile)
        if (actualSha256 != expectedSha256) {
          archiveFile.delete()
          throw SecurityException(
            "SHA-256 checksum mismatch! Expected $expectedSha256 but got $actualSha256"
          )
        }

        if (rootfsDir.exists()) rootfsDir.deleteRecursively()
        rootfsDir.mkdirs()

        extractTarGzSafely(archiveFile, rootfsDir)
        archiveFile.delete()

        val etcDir = File(rootfsDir, "etc").apply { mkdirs() }
        File(etcDir, "resolv.conf").writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")
        markerFile.writeText(expectedSha256)

        _state.value = RootfsInstallState.Ready(rootfsDir, expectedSha256)
        rootfsDir
      }.onFailure { err ->
        _state.value =
          RootfsInstallState.Error(err.localizedMessage ?: "Ubuntu rootfs installation failed")
      }
    }

  fun deleteRootfs() {
    rootfsDir.deleteRecursively()
    rootfsDir.mkdirs()
    _state.value = RootfsInstallState.Idle
  }

  fun buildProotShellArgs(workspaceDir: File, initialCommand: String? = null): Pair<String, Array<String>> {
    val diag = inspectDiagnostics()
    val prootPath =
      diag.prootBinaryPath
        ?: throw IllegalStateException(
          "libproot.so is not present in ${diag.nativeLibraryDir}. Android 10+ W^X blocks executing downloaded ELF binaries directly from writable app storage."
        )
    // Android has no /etc/resolv.conf; without it apt/curl/npm cannot resolve hostnames.
    runCatching {
      val resolv = File(rootfsDir, "etc/resolv.conf")
      if (!resolv.exists() || resolv.length() == 0L) {
        resolv.parentFile?.mkdirs()
        resolv.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
      }
    }
    File(context.cacheDir, "proot-tmp").mkdirs()
    val args = mutableListOf(
      "--link2symlink",
      "--kill-on-exit",
      "-0",
      "-r",
      rootfsDir.absolutePath,
      "-b",
      "/dev",
      "-b",
      "/proc",
      "-b",
      "/sys",
      "-b",
      "${workspaceDir.absolutePath}:/workspace",
    )
    // With storage access granted, expose the phone's shared storage inside Ubuntu.
    val storageGranted =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
      } else {
        context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
          android.content.pm.PackageManager.PERMISSION_GRANTED
      }
    if (storageGranted) {
      val shared = Environment.getExternalStorageDirectory()
      if (shared.exists()) {
        args += "-b"
        args += "${shared.absolutePath}:/sdcard"
      }
    }
    args += listOf(
      "-w",
      "/workspace",
      "/usr/bin/env",
      "-i",
      "HOME=/root",
      "TERM=xterm-256color",
      "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      "/bin/bash",
    )
    if (!initialCommand.isNullOrBlank()) {
      args += "-lc"
      args += initialCommand
    } else {
      args += "--login"
    }
    return prootPath to args.toTypedArray()
  }

  /** Environment proot needs on Android: unbundled loader shipped as a native lib + a writable tmp dir. */
  fun prootEnv(): List<String> {
    val nativeDirPath = context.applicationInfo.nativeLibraryDir ?: return emptyList()
    return listOf(
      "PROOT_LOADER=$nativeDirPath/libproot-loader.so",
      "PROOT_TMP_DIR=${File(context.cacheDir, "proot-tmp").absolutePath}",
      "PROOT_NO_SECCOMP=1",
    )
  }

  fun buildSetupAiClisCommand(): String =
    "apt-get update && apt-get install -y curl ca-certificates gnupg && " +
      "curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && " +
      "apt-get install -y nodejs && " +
      "npm install -g @anthropic-ai/claude-code @google/gemini-cli"

  private fun computeSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(32 * 1024)
    file.inputStream().use { input ->
      while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        digest.update(buffer, 0, read)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun extractTarGzSafely(archive: File, destDir: File) {
    val rootCanonical = destDir.canonicalFile
    val rootPrefix = rootCanonical.path + File.separator
    var count = 0

    BufferedInputStream(archive.inputStream()).use { raw ->
      GzipCompressorInputStream(raw).use { gz ->
        TarArchiveInputStream(gz).use { tar ->
          var entry: TarArchiveEntry? = tar.nextEntry
          while (entry != null) {
            val cleanName = entry.name.removePrefix("./")
            if (cleanName.isNotBlank()) {
              val target = File(rootCanonical, cleanName).canonicalFile
              if (!target.path.startsWith(rootPrefix) && target != rootCanonical) {
                throw SecurityException("Blocked unsafe TAR entry: ${entry.name}")
              }
              when {
                entry.isDirectory -> {
                  target.mkdirs()
                }
                entry.isSymbolicLink -> {
                  target.parentFile?.mkdirs()
                  if (target.exists()) target.delete()
                  runCatching { Os.symlink(entry.linkName, target.absolutePath) }
                }
                entry.isLink -> {
                  // Hard link: the entry has no data; copy the already-extracted target.
                  val src = File(rootCanonical, entry.linkName.removePrefix("./")).canonicalFile
                  if (!src.path.startsWith(rootPrefix)) {
                    throw SecurityException("Blocked unsafe TAR link: ${entry.name}")
                  }
                  target.parentFile?.mkdirs()
                  if (target.exists()) target.delete()
                  if (src.isFile) {
                    src.copyTo(target, overwrite = true)
                    runCatching { Os.chmod(target.absolutePath, entry.mode and 0x1FF) }
                  }
                }
                entry.isFile -> {
                  target.parentFile?.mkdirs()
                  FileOutputStream(target).use { out -> tar.copyTo(out) }
                  if (entry.mode != 0) {
                    runCatching { Os.chmod(target.absolutePath, entry.mode and 0x1FF) }
                  }
                }
                else -> Unit // skip devices/fifos
              }
              count++
              if (count % 100 == 0) {
                _state.value = RootfsInstallState.Extracting(count)
              }
            }
            entry = tar.nextEntry
          }
        }
      }
    }
  }

  companion object {
    private const val UBUNTU_RELEASE_BASE_URL =
      "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/"
    private const val UBUNTU_SHA256SUMS_URL = "${UBUNTU_RELEASE_BASE_URL}SHA256SUMS"
  }
}
