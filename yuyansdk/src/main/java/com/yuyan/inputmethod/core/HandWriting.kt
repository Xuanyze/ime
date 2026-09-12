package com.yuyan.inputmethod.core

import android.content.Context
import androidx.annotation.Keep

object HandWriting {

    /** 手写 native 库仅在 arm64-v8a 提供；32 位设备（如班牌）加载失败时整体降级，不影响拼音输入 */
    val isLoaded: Boolean

    init {
        isLoaded = try {
            System.loadLibrary("handwriting")
            true
        } catch (t: Throwable) {
            false
        }
    }

    private var initialized = false

    fun init(context: Context): Boolean {
        if (!isLoaded) return false
        if (this.initialized) return true
        val result = initWithDirectory(context, context.getExternalFilesDir("hw").toString())
        this.initialized = result
        return  result
    }

    fun setProperties() {
        if (!isLoaded) return
        reloadConfig()
    }

    fun selectInputMode(i: Int): Boolean {
        if (!isLoaded) return false
        return activeMode(i)
    }

    @Throws(NumberFormatException::class)
    fun getCandidatesPyComposition(): Array<Array<String?>?> {
        if (!isLoaded) return emptyArray()
        return getCandidates()
    }

    @Keep
    external fun getPackageName(): String?

    external fun activeMode(mode: Int): Boolean

    external fun getCandidates(): Array<Array<String?>?>

    external fun initWithDirectory(context: Context, str: String?): Boolean

    external fun inputHWPoints(iArr: IntArray?): Boolean

    external fun release()

    external fun reloadConfig(): Boolean

    external fun reset(): Boolean
}