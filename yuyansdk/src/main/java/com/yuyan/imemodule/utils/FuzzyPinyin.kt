package com.yuyan.imemodule.utils

import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.view.preference.ManagedPreference
import com.yuyan.inputmethod.core.Kernel

/**
 * 模糊音：pinyin / pinyin_fuzzy 双方案热切换。
 *
 * pinyin_fuzzy 方案（assets/rime/pinyin_fuzzy.schema.yaml）= 雾凇全拼 + 9 组模糊音 derive，
 * prism 已用 librime 1.11.2 预编译进 assets/rime/build/，切换只在已编译方案间选择，瞬时生效，
 * 完全不走设备端部署（私有 libyuyanime.so 的部署器会用内嵌的无 algebra 方案重建，不可用）。
 *
 * 粒度说明：9 组开关任意一个打开即整体启用模糊音（方案级粒度）。
 */
object FuzzyPinyin {

    private fun fuzzyPrefs(): List<ManagedPreference.PBool> =
        AppPrefs.getInstance().input.let {
            listOf(it.fuzzyZhZ, it.fuzzyChC, it.fuzzyShS, it.fuzzyNL,
                it.fuzzyLR, it.fuzzyFH, it.fuzzyAnAng, it.fuzzyEnEng, it.fuzzyInIng)
        }

    fun isFuzzyOn(): Boolean = fuzzyPrefs().any { it.getValue() }

    /** 全键拼音方案按模糊音开关解析为 pinyin / pinyin_fuzzy，其余方案原样返回 */
    fun resolveSchemaId(schemaId: String): String =
        if (schemaId == CustomConstant.SCHEMA_ZH_QWERTY && isFuzzyOn()) {
            CustomConstant.SCHEMA_ZH_QWERTY_FUZZY
        } else schemaId

    /** 模糊音开关变化：当前若处于全键拼音，立即热切换方案 */
    fun onTogglesChanged() {
        val target = resolveSchemaId(AppPrefs.getInstance().internal.pinyinModeRime.getValue())
        if (Kernel.getCurrentRimeSchema() != target) {
            Kernel.initImeSchema(target)
        }
    }
}
