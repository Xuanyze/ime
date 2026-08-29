package com.yuyan.imemodule.utils

import android.content.Context
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.inputmethod.RimeEngine
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.inputmethod.core.Kernel
import com.yuyan.inputmethod.core.Rime
import java.io.File

/**
 * 模糊音：按开关组生成 pinyin.custom.yaml（speller/algebra 追加 derive 规则），
 * 触发 Rime 重新部署。改动需重新部署后生效。
 */
object FuzzyPinyin {
    // id to (标题, derive 规则列表)
    val GROUPS: Map<String, Pair<String, List<String>>> = mapOf(
        "zh_z" to ("模糊音 zh/z" to listOf("derive/^([zcs])h/\$1/", "derive/^([zcs])([aeiou])/\$1h\$2/")),
        "ch_c" to ("模糊音 ch/c" to listOf()),
        "sh_s" to ("模糊音 sh/s" to listOf()),
        "n_l" to ("模糊音 n/l" to listOf("derive/^n([aeiou])/l\$1/", "derive/^l([aeiou])/n\$1/")),
        "l_r" to ("模糊音 l/r" to listOf("derive/^l([aeiou])/r\$1/", "derive/^r([aeiou])/l\$1/")),
        "f_h" to ("模糊音 f/h" to listOf("derive/^f([aeiou])/h\$1/", "derive/^h([aeiou])/f\$1/")),
        "an_ang" to ("模糊音 an/ang" to listOf("derive/ang$/an/", "derive/an$/ang/")),
        "en_eng" to ("模糊音 en/eng" to listOf("derive/eng$/en/", "derive/en$/eng/")),
        "in_ing" to ("模糊音 in/ing" to listOf("derive/ing$/in/", "derive/in$/ing/")),
    )

    fun isEnabled(context: Context, id: String): Boolean =
        context.getSharedPreferences("fuzzy_prefs", Context.MODE_PRIVATE).getBoolean(id, false)

    fun setEnabled(context: Context, id: String, enabled: Boolean) {
        context.getSharedPreferences("fuzzy_prefs", Context.MODE_PRIVATE).edit().putBoolean(id, enabled).apply()
        apply(context)
    }

    /** 依据开关状态写/删 pinyin.custom.yaml，并触发 Rime 重部署（重部署在本进程内完成） */
    private val DOUBLE_SCHEMAS = listOf(
        "double_pinyin_natural", "double_pinyin_flypy", "double_pinyin_mspy",
        "double_pinyin_sogou", "double_pinyin_abc", "double_pinyin_ziguang")

    /** 声母组（双拼=首键互换，全拼=首字母互换） */
    private val initialRules = mapOf(
        "n_l" to listOf("derive/^n(.)/l$1D".replace("D",""), "derive/^l(.)/n$1D".replace("D","")),
        "l_r" to listOf("derive/^l(.)/r$1D".replace("D",""), "derive/^r(.)/l$1D".replace("D","")),
        "f_h" to listOf("derive/^f(.)/h$1D".replace("D",""), "derive/^h(.)/f$1D".replace("D","")))

    /** 韵尾组（仅全拼方案生效；双拼韵母键位各方案不同，v1 不做） */
    private val finalRules = mapOf(
        "an_ang" to listOf("derive/ang$/an/", "derive/an$/ang/"),
        "en_eng" to listOf("derive/eng$/en/", "derive/en$/eng/"),
        "in_ing" to listOf("derive/ing$/in/", "derive/in$/ing/"))

    private val BSN: String = "\n"

    private fun yamlPatch(rules: List<String>): String? =
        if (rules.isEmpty()) null
        else "patch:" + BSN + "  speller/algebra/@next:" + BSN +
            rules.joinToString(BSN) { "    - " + it.replace("$", BACKSLASH + "$") } + BSN

    private val BACKSLASH: String = "\\"

    fun apply(context: Context) {
        runCatching {
            val dir = File(CustomConstant.RIME_DICT_PATH)
            val on = GROUPS.keys.filter { isEnabled(context, it) }
            // 全拼：声母组 + 韵尾组
            val pyRules = initialRules.filterKeys { it in on }.values.flatten() +
                finalRules.filterKeys { it in on }.values.flatten()
            writePatch(File(dir, "pinyin.custom.yaml"), yamlPatch(pyRules))
            // 双拼：仅声母组
            val dpRules = initialRules.filterKeys { it in on }.values.flatten()
            DOUBLE_SCHEMAS.forEach { id ->
                writePatch(File(dir, "$id.custom.yaml"), yamlPatch(dpRules))
            }
            // resetIme() 默认 fullCheck=false 不会重部署，custom patch 不会被编译；
            // 这里走 fullCheck=true 触发完整部署后再切回当前方案
            RimeEngine.destroy()
            Rime.getInstance(fullCheck = true)
            Kernel.initImeSchema(AppPrefs.getInstance().internal.pinyinModeRime.getValue())
        }
    }

    private fun writePatch(file: File, content: String?) {
        if (content == null) file.delete() else file.writeText(content)
    }
}
