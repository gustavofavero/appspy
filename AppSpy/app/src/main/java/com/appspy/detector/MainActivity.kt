package com.appspy.detector

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.appspy.detector.databinding.ActivityMainBinding

enum class SortMode { INSTALL_DATE_DESC, NOTIF_COUNT_DESC, NAME_ASC }

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppListAdapter
    private lateinit var logger: NotificationLogger
    private var fullList: List<AppInfo> = emptyList()
    private var currentSort = SortMode.NOTIF_COUNT_DESC
    private var showOnlyUserApps = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        logger = NotificationLogger(applicationContext)

        adapter = AppListAdapter(emptyList()) { app -> showAppDetails(app) }
        binding.recyclerApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerApps.adapter = adapter

        binding.btnSort.setOnClickListener { showSortMenu() }
        binding.btnFilter.setOnClickListener {
            showOnlyUserApps = !showOnlyUserApps
            binding.btnFilter.text = if (showOnlyUserApps) "Mostrando: Usuário" else "Mostrando: Todos"
            refreshList()
        }
        binding.btnEnableNotifAccess.setOnClickListener { openNotificationAccessSettings() }

        if (!isNotificationServiceEnabled()) {
            binding.cardNotifWarning.visibility = android.view.View.VISIBLE
        } else {
            binding.cardNotifWarning.visibility = android.view.View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        logger.trimOldEntries()
        refreshList()
        binding.cardNotifWarning.visibility =
            if (isNotificationServiceEnabled()) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun refreshList() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        Thread {
            val apps = loadInstalledApps()
            runOnUiThread {
                fullList = apps
                applyFilterAndSort()
                binding.progressBar.visibility = android.view.View.GONE
            }
        }.start()
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val notifCounts = logger.countLastHours(24)
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return installedApps.map { ai: ApplicationInfo ->
            val pkgInfo = try {
                pm.getPackageInfo(ai.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            val installer = try {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(ai.packageName)
            } catch (e: Exception) {
                null
            }
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            AppInfo(
                packageName = ai.packageName,
                appName = pm.getApplicationLabel(ai).toString(),
                isSystemApp = isSystem,
                installTime = pkgInfo?.firstInstallTime ?: 0L,
                updateTime = pkgInfo?.lastUpdateTime ?: 0L,
                notificationCount24h = notifCounts[ai.packageName] ?: 0,
                hasUnknownSource = installer == null || installer !in setOf(
                    "com.android.vending", // Google Play
                    "com.google.android.packageinstaller"
                )
            )
        }
    }

    private fun applyFilterAndSort() {
        var list = fullList
        if (showOnlyUserApps) {
            list = list.filter { !it.isSystemApp }
        }
        list = when (currentSort) {
            SortMode.INSTALL_DATE_DESC -> list.sortedByDescending { it.installTime }
            SortMode.NOTIF_COUNT_DESC -> list.sortedByDescending { it.notificationCount24h }
            SortMode.NAME_ASC -> list.sortedBy { it.appName.lowercase() }
        }
        adapter.updateItems(list)
        binding.textSummary.text = "${list.size} apps · ${list.count { it.riskScore() >= 2 }} suspeitos/alto risco"
    }

    private fun showSortMenu() {
        val options = arrayOf(
            "Data de instalação (mais recente primeiro)",
            "Notificações nas últimas 24h (mais primeiro)",
            "Nome (A-Z)"
        )
        AlertDialog.Builder(this)
            .setTitle("Ordenar por")
            .setItems(options) { _, which ->
                currentSort = when (which) {
                    0 -> SortMode.INSTALL_DATE_DESC
                    1 -> SortMode.NOTIF_COUNT_DESC
                    else -> SortMode.NAME_ASC
                }
                applyFilterAndSort()
            }
            .show()
    }

    private fun showAppDetails(app: AppInfo) {
        val df = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale("pt", "BR"))
        val msg = buildString {
            append("Pacote: ${app.packageName}\n")
            append("Instalado em: ${df.format(java.util.Date(app.installTime))}\n")
            append("Última atualização: ${df.format(java.util.Date(app.updateTime))}\n")
            append("Notificações (24h): ${app.notificationCount24h}\n")
            append("Fonte desconhecida (fora da Play Store): ${if (app.hasUnknownSource) "Sim" else "Não"}\n")
            append("Classificação de risco: ${app.riskLabel()}\n\n")
            append("Se este app dispara notificações/propaganda com muita frequência e você não reconhece a origem, considere desinstalá-lo.")
        }
        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setMessage(msg)
            .setPositiveButton("Desinstalar") { _, _ -> uninstallApp(app.packageName) }
            .setNegativeButton("Ver na tela de app") { _, _ -> openAppInfoSettings(app.packageName) }
            .setNeutralButton("Fechar", null)
            .show()
    }

    private fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    private fun openAppInfoSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    private fun openNotificationAccessSettings() {
        Toast.makeText(
            this,
            "Ative o acesso a notificações para o AppSpy poder contar as propagandas de cada app",
            Toast.LENGTH_LONG
        ).show()
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(packageName)
    }
}
