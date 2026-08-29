package com.yuyan.imemodule.utils

import android.content.Context
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.inputmethod.core.Kernel
import java.io.File

/**
 * 模糊音：按开关组生成 pinyin.custom.yaml（speller/algebra 追加 derive 规则），
 * 触发 Rime 重新部署。改动需重新部署后生效。
 */
object FuzzyPinyin {
    // id to (标题, derive 规则列表)
    val GROUPS: Map<String, Pair<String, List<String>>> = mapOf(
        "zh_z" to ("模糊音 zh/z" to listOf("derive/^([zcs])h/$1/", "derive/^([zcs])([aeiou])/$1h$2/")),
        "ch_c" to ("模糊音 ch/c" to listOf()),
        "sh_s" to ("模糊音 sh/s" to listOf()),
        "n_l" to ("模糊音 n/l" to listOf("derive/^n([aeiou])/l$1/", "derive/^l([aeiou])/n$1/")),
        "l_r" to ("模糊音 l/r" to listOf("derive/^l([aeiou])/r$1/", "derive/^r([aeiou])/l$1/")),
        "f_h" to ("模糊音 f/h" to listOf("derive/^f([aeiou])/h$1/", "derive/^h([aeiou])/f$1/")),
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
    fun apply(context: Context) {
        runCatching {
            val dir = File(CustomConstant.RIME_DICT_PATH)
            val patch = File(dir, "pinyin.custom.yaml")
            val rules = GROUPS.filter { isEnabled(context, it.key) }
                .flatMap { it.value.second.ifEmpty { GROUPS[it.key]!!.second } }
                .toMutableList()
            // zh/z、ch/c、sh/s 三组共用 zh 类规则，这里显式补全
            if (isEnabled(context, "ch_c")) rules += listOf("derive/^([zcs])h/$1/", "derive/^([zcs])([aeiou])/$1h$2/")
            if (isEnabled(context, "sh_s")) rules += listOf("derive/^([zcs])h/$1/", "derive/^([zcs])([aeiou])/$1h$2/")
            if (rules.isEmpty()) {
                patch.delete()
            } else {
                val body = rules.joinToString("
") { "    - $it" }
                patch.writeText("patch:
  speller/algebra/@next:
$body
")
            }
            Kernel.resetIme()
        }
    }
}
