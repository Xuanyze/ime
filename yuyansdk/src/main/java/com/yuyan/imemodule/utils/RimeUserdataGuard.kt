package com.yuyan.imemodule.utils

import android.content.Context

/**
 * 公共设备场景：限制 Rime 用户词典(userdb)与引擎日志的无限增长。
 * 必须在 Rime 引擎启动、打开 userdb 之前调用（Launcher 初始化子线程），
 * 直接删除上一进程遗留的 userdb 与日志文件。
 * 代价是自学习数据不保留——本项目刻意为之：多人共用设备，输入体验统一、可预测。
 */
object RimeUserdataGuard {
    private const val PREF_NAME = "rime_userdata_guard"
    private const val KEY_LAST_CLEAN = "last_clean_time"
    private const val INTERVAL_MS = 24L * 60 * 60 * 1000

    fun limitUserdata(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CLEAN, 0L) < INTERVAL_MS) return
        prefs.edit().putLong(KEY_LAST_CLEAN, now).apply()
        runCatching {
            val dir = context.getExternalFilesDir("rime") ?: return
            dir.listFiles()?.forEach { file ->
                val name = file.name
                val isUserdata = name.contains(".userdb")
                val isLog = name.startsWith("rime.") || name.endsWith(".log")
                if (isUserdata || isLog) file.deleteRecursively()
            }
        }
    }
}
