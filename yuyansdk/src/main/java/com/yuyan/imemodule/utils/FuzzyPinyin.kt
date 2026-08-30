package com.yuyan.imemodule.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.view.preference.ManagedPreference
import com.yuyan.inputmethod.core.Kernel
import com.yuyan.inputmethod.core.Rime
import java.io.File
import java.util.concurrent.Executors

/**
 * 模糊音：按开关组生成 <schema>.custom.yaml（speller/algebra 头部插入 derive 规则），
 * 触发 Rime 完整重部署（fullCheck=true）。改动需重新部署后生效。
 *
 * 注意：
 * - 开关状态唯一来源是设置页的 AppPrefs（历史版本曾读独立的 fuzzy_prefs，导致规则恒为空）；
 * - patch 键必须用 "speller/algebra/0+"（librime 逐项头插）。"@next" 赋列表会嵌成单个列表元素，
 *   派生规则全部失效；
 * - 规则必须插在 algebra 头部（0+）：双拼方案随后会把 zh/ch/sh、韵母 xform 成单符号拼写，
 *   尾部追加的派生拼写得不到同样变换，无法命中键位。
 * - 全量重部署耗时分钟级，放子线程防抖执行；部署期间 Rime.deploying=true 让输入链路安全降级。
 */
object FuzzyPinyin {
    private const val TAG = "FuzzyPinyin"
    private const val DEBOUNCE_MS = 600L

    // 开关 id → derive 规则（前 6 组为声母组，后 3 组为韵尾组，仅全拼生效）
    private val RULES: Map<String, List<String>> = mapOf(
        "zh_z" to listOf("derive/^zh/z/", "derive/^z([aeiou])/zh\$1/"),
        "ch_c" to listOf("derive/^ch/c/", "derive/^c([aeiou])/ch\$1/"),
        "sh_s" to listOf("derive/^sh/s/", "derive/^s([aeiou])/sh\$1/"),
        "n_l" to listOf("derive/^n(.)/l\$1/", "derive/^l(.)/n\$1/"),
        "l_r" to listOf("derive/^l([aeiou])/r\$1/", "derive/^r([aeiou])/l\$1/"),
        "f_h" to listOf("derive/^f([aeiou])/h\$1/", "derive/^h([aeiou])/f\$1/"),
        "an_ang" to listOf("derive/ang\$/an/", "derive/an\$/ang/"),
        "en_eng" to listOf("derive/eng\$/en/", "derive/en\$/eng/"),
        "in_ing" to listOf("derive/ing\$/in/", "derive/in\$/ing/"))

    private val INITIAL_IDS = setOf("zh_z", "ch_c", "sh_s", "n_l", "l_r", "f_h")

    private val DOUBLE_SCHEMAS = listOf(
        "double_pinyin_natural", "double_pinyin_flypy", "double_pinyin_mspy",
        "double_pinyin_sogou", "double_pinyin_abc", "double_pinyin_ziguang")

    private val switchPrefs: Map<String, ManagedPreference.PBool> by lazy {
        val input = AppPrefs.getInstance().input
        mapOf(
            "zh_z" to input.fuzzyZhZ, "ch_c" to input.fuzzyChC, "sh_s" to input.fuzzyShS,
            "n_l" to input.fuzzyNL, "l_r" to input.fuzzyLR, "f_h" to input.fuzzyFH,
            "an_ang" to input.fuzzyAnAng, "en_eng" to input.fuzzyEnEng, "in_ing" to input.fuzzyInIng)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "fuzzy-deploy") }

    /** 防抖：连续拨动多个开关只触发一次部署 */
    fun apply(context: Context) {
        mainHandler.removeCallbacks(deployTask)
        mainHandler.postDelayed(deployTask, DEBOUNCE_MS)
    }

    private val deployTask = Runnable { executor.execute { deployNow() } }

    private fun yamlPatch(rules: List<String>): String? =
        if (rules.isEmpty()) null
        else "patch:\n  speller/algebra/0+:\n" +
            rules.joinToString("\n") { "    - $it" } + "\n"

    private fun deployNow() {
        val context = Launcher.instance.context
        try {
            val dir = File(CustomConstant.RIME_DICT_PATH)
            val on = RULES.keys.filter { switchPrefs[it]?.getValue() == true }
            val pyRules = RULES.filterKeys { it in on }.values.flatten()
            val dpRules = RULES.filterKeys { it in on && it in INITIAL_IDS }.values.flatten()
            writePatch(File(dir, "pinyin.custom.yaml"), yamlPatch(pyRules))
            DOUBLE_SCHEMAS.forEach { id ->
                writePatch(File(dir, "$id.custom.yaml"), yamlPatch(dpRules))
            }
            Log.i(TAG, "fuzzy deploy start: on=$on, pinyinRules=${pyRules.size}")
            Rime.deploying = true
            val start = SystemClock.elapsedRealtime()
            Rime.recreate(fullCheck = true)
            Rime.deploying = false
            Kernel.initImeSchema(AppPrefs.getInstance().internal.pinyinModeRime.getValue())
            Log.i(TAG, "fuzzy deploy done in ${SystemClock.elapsedRealtime() - start} ms")
            toast(context, "模糊音部署完成")
        } catch (e: Throwable) {
            Rime.deploying = false
            Log.w(TAG, "fuzzy deploy failed", e)
            toast(context, "模糊音部署失败：${e.message}")
        }
    }

    private fun writePatch(file: File, content: String?) {
        if (content == null) file.delete() else file.writeText(content)
    }

    private fun toast(context: Context, msg: String) {
        mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }
}
