package com.yuyan.inputmethod.core

import android.content.Context
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.application.Launcher
import kotlin.system.measureTimeMillis

class Rime(fullCheck: Boolean) {

    init {
        startup(Launcher.instance.context, fullCheck)
    }

    companion object {
        private var instance: Rime? = null
        private var mContext: RimeContext? = null
        private var mStatus: RimeStatus? = null
        private val rimeLock = Any()

        /** 重部署进行中（native 会话销毁重建，此期间所有引擎调用安全降级） */
        @Volatile
        var deploying: Boolean = false

        @JvmStatic
        fun getInstance(fullCheck: Boolean = false): Rime {
            synchronized(rimeLock) {
                if (instance == null) instance = Rime(fullCheck)
                return instance!!
            }
        }

        init {
            System.loadLibrary("yuyanime")
        }

        fun startup(context: Context, fullCheck: Boolean) {
            synchronized(rimeLock) {
                startupRime(context, CustomConstant.RIME_DICT_PATH, CustomConstant.RIME_DICT_PATH, fullCheck)
                updateStatus()
            }
        }

        @JvmStatic
        fun destroy() {
            synchronized(rimeLock) {
                exitRime()
                instance = null
            }
        }

        /**
         * 销毁并以指定 fullCheck 重建，两步在同一锁内完成。
         * 若分开调用 destroy+getInstance，中间其他线程可能抢先以 fullCheck=false 建实例，
         * 导致全量部署被跳过。
         */
        fun recreate(fullCheck: Boolean) {
            synchronized(rimeLock) {
                exitRime()
                instance = Rime(fullCheck)
            }
        }

        fun updateStatus() {
            if (deploying) return
            measureTimeMillis {
                mStatus = getRimeStatus() ?: RimeStatus()
            }
        }

        fun updateContext() {
            if (deploying) return
            measureTimeMillis {
                mContext = getRimeContext() ?: RimeContext()
            }
            updateStatus()
        }

        @JvmStatic
        val isComposing get() = mStatus?.isComposing == true

        @JvmStatic
        fun hasMenu(): Boolean {
            return isComposing && mContext?.menu?.numCandidates != 0
        }

        @JvmStatic
        fun hasRight(): Boolean {
            return hasMenu() && mContext?.menu?.isLastPage == false
        }

        @JvmStatic
        val composition: RimeComposition?
            get() = mContext?.composition

        @JvmStatic
        val compositionText: String
            get() = composition?.preedit ?: ""

        @JvmStatic
        fun processKey(keycode: Int, mask: Int): Boolean {
            if (keycode <= 0 || keycode == 0xffffff || deploying) return false
            setRimePageSize(100)
            return processRimeKey(keycode, mask).also {
                updateContext()
            }
        }

        @JvmStatic
        fun replaceKey(caretPos: Int, length: Int, key: String): Boolean {
            if (deploying) return false
            return replaceRimeKey(caretPos, length, key).also {
                updateContext()
            }
        }

        @JvmStatic
        fun clearComposition() {
            if (deploying) return
            clearRimeComposition()
            updateContext()
        }

        @JvmStatic
        fun selectCandidate(index: Int): Boolean {
            if (deploying) return false
            return selectRimeCandidate(index).also {
                updateContext()
            }
        }

        @JvmStatic
        fun setOption(option: String, value: Boolean) {
            if (deploying) return
            setRimeOption(option, value)
        }

        @JvmStatic
        fun selectSchema(schemaId: String): Boolean {
            if (deploying) return false
            return selectRimeSchema(schemaId).also {
                updateContext()
            }
        }

        fun getAssociateList(key: String?): Array<String?> {
            if (deploying) return emptyArray()
            return getRimeAssociateList(key)
        }

        fun chooseAssociate(index: Int): Boolean {
            if (deploying) return false
            return selectRimeAssociate(index)
        }

        @JvmStatic
        external fun startupRime(context: Context, sharedDir: String, userDir: String, fullCheck: Boolean, )

        @JvmStatic
        external fun exitRime()

        @JvmStatic
        external fun setRimePageSize(pageSize:Int)

        @JvmStatic
        external fun processRimeKey(keycode: Int, mask: Int): Boolean

        @JvmStatic
        external fun replaceRimeKey(caretPos: Int, length: Int, key: String?): Boolean

        @JvmStatic
        external fun clearRimeComposition()

        @JvmStatic
        external fun getRimeCommit(): RimeCommit?

        @JvmStatic
        external fun getRimeContext(): RimeContext?

        @JvmStatic
        external fun getRimeStatus(): RimeStatus?

        @JvmStatic
        external fun setRimeOption(option: String, value: Boolean, )

        @JvmStatic
        external fun getCurrentRimeSchema(): String

        @JvmStatic
        external fun selectRimeSchema(schemaId: String): Boolean

        @JvmStatic
        external fun selectRimeCandidate(index: Int): Boolean

        @JvmStatic
        external fun getRimeKeycodeByName(name: String): Int

        @JvmStatic
        external fun getRimeAssociateList(key: String?): Array<String?>

        @JvmStatic
        external fun selectRimeAssociate(index: Int): Boolean
    }
}
