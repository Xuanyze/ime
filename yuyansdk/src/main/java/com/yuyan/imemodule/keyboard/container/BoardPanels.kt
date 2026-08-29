package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.inputmethod.core.Kernel
import splitties.dimensions.dp
import splitties.views.dsl.core.margin

/**
 * 班牌横屏三栏布局的左栏：输入方案选择 + 数字小键盘。
 * 左列四键：26键全拼 / 双拼 / 9键拼音 / 手写（约一行键盘高的方形键），
 * 右列数字小键盘（7-9/4-6/1-3/·0退格），列宽 1:3，键位近似方形。
 * 方案切换复用 InputModeSwitcher.switchModeForSetting（与设置页"选择输入方案"同一条链路）。
 */
@SuppressLint("ViewConstructor")
class BoardLeftPanel(context: Context, private val inputView: InputView) : LinearLayout(context) {

    private class SchemeOption(
        val title: String,
        val value: () -> Pair<Int, String>,
        val match: () -> Boolean
    )

    private val options = listOf(
        SchemeOption(context.getString(R.string.board_scheme_qwerty),
            { Pair(InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN, CustomConstant.SCHEMA_ZH_QWERTY) },
            { InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN && Kernel.getCurrentRimeSchema() == CustomConstant.SCHEMA_ZH_QWERTY }),
        SchemeOption(context.getString(R.string.board_scheme_double),
            { Pair(InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN, CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + AppPrefs.getInstance().input.doublePYSchemaMode.getValue()) },
            { InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN && Kernel.getCurrentRimeSchema()?.startsWith(CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY) == true }),
        SchemeOption(context.getString(R.string.board_scheme_t9),
            { Pair(InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN, CustomConstant.SCHEMA_ZH_T9) },
            { InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN }),
        SchemeOption(context.getString(R.string.board_scheme_hand),
            { Pair(InputModeSwitcher.MASK_SKB_LAYOUT_HANDWRITING, CustomConstant.SCHEMA_ZH_HANDWRITING) },
            { InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_HANDWRITING }),
    )

    private val titleViews: List<TextView>

    init {
        orientation = HORIZONTAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
        val env = EnvironmentSingleton.instance
        val schemeSize = env.keyTextSize

        val schemeButtons = options.map { option ->
            boardPanelButton(option.title, schemeSize) { switchTo(option) }
        }
        titleViews = schemeButtons

        val schemesCol = LinearLayout(context).apply { orientation = VERTICAL }
        schemeButtons.forEach { view ->
            schemesCol.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { margin = dp(5) })
        }

        fun numberRow(vararg buttons: View): LinearLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            buttons.forEach { addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { margin = dp(5) }) }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val numberPad = LinearLayout(context).apply { orientation = VERTICAL }
        val numSize = env.keyTextSize
        numberPad.addView(numberRow(boardPanelButton("7", numSize) { inputView.panelCommitText("7") }, boardPanelButton("8", numSize) { inputView.panelCommitText("8") }, boardPanelButton("9", numSize) { inputView.panelCommitText("9") }))
        numberPad.addView(numberRow(boardPanelButton("4", numSize) { inputView.panelCommitText("4") }, boardPanelButton("5", numSize) { inputView.panelCommitText("5") }, boardPanelButton("6", numSize) { inputView.panelCommitText("6") }))
        numberPad.addView(numberRow(boardPanelButton("1", numSize) { inputView.panelCommitText("1") }, boardPanelButton("2", numSize) { inputView.panelCommitText("2") }, boardPanelButton("3", numSize) { inputView.panelCommitText("3") }))
        numberPad.addView(numberRow(boardPanelButton("·", numSize) { inputView.panelCommitText(".") }, boardPanelButton("0", numSize) { inputView.panelCommitText("0") }, boardPanelButton("⌫", numSize) { inputView.panelBackspace() }))

        addView(schemesCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        addView(numberPad, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3f))
        refresh()
    }

    private fun switchTo(option: SchemeOption) {
        InputModeSwitcher.switchModeForSetting(option.value())
        inputView.resetToIdleState()
        refresh()
    }

    /** 当前方案高亮（仅在键盘布局变化时由 InputView 调用，避免每次按键都做原生调用） */
    fun refresh() {
        val theme = ThemeManager.activeTheme
        titleViews.forEachIndexed { index, view ->
            if (index >= options.size) return@forEachIndexed
            val active = runCatching { options[index].match() }.getOrDefault(false)
            (view.background as? GradientDrawable)?.setColor(if (active) theme.accentKeyBackgroundColor else theme.keyBackgroundColor)
            view.setTextColor(if (active) theme.accentKeyTextColor else theme.keyTextColor)
        }
    }
}

/**
 * 班牌横屏三栏布局的右栏：编辑功能，2 列 × 6 行方形键。
 * 方向/Home/End/Delete + 全选/复制/剪切/粘贴；退格用主键盘的（不重复放）。
 * 全部复用现有编辑键链路，无假功能。
 */
@SuppressLint("ViewConstructor")
class BoardRightPanel(context: Context, private val inputView: InputView) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
        val textSize = (EnvironmentSingleton.instance.keyTextSmallSize * 1.15f).toInt()

        fun row(vararg buttons: View): LinearLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            buttons.forEach { addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { margin = dp(5) }) }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        addView(row(boardPanelButton("↑", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_DPAD_UP) }, boardPanelButton("↓", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN) }))
        addView(row(boardPanelButton("←", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT) }, boardPanelButton("→", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT) }))
        addView(row(boardPanelButton("Home", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_MOVE_START) }, boardPanelButton("End", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_MOVE_END) }))
        addView(row(boardPanelButton("全选", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_SELECT_ALL) }, boardPanelButton("复制", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_COPY) }))
        addView(row(boardPanelButton("剪切", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_CUT) }, boardPanelButton("粘贴", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_PASTE) }))
        addView(row(boardPanelButton("Del", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL) }))
    }
}

/**
 * 班牌侧栏按键：素色圆角（无边框），颜色取自当前主题适配深色模式。
 * 长标签自动缩小字号且不折行。
 */
private fun View.boardPanelButton(label: String, textSizePx: Int, onClick: () -> Unit): TextView {
    val view = TextView(context)
    view.gravity = Gravity.CENTER
    view.text = label
    view.maxLines = 1
    val fittedSize = when {
        label.length <= 2 -> textSizePx
        label.length <= 4 -> (textSizePx * 0.78f).toInt()
        else -> (textSizePx * 0.7f).toInt()
    }
    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, fittedSize.toFloat())
    view.setTextColor(ThemeManager.activeTheme.keyTextColor)
    view.background = GradientDrawable().apply {
        cornerRadius = dp(12).toFloat()
        setColor(ThemeManager.activeTheme.keyBackgroundColor)
    }
    view.setPadding(dp(2), dp(2), dp(2), dp(2))
    view.isClickable = true
    view.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> v.alpha = 0.55f
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.alpha = 1f
        }
        false
    }
    view.setOnClickListener {
        DevicesUtils.tryPlayKeyDown()
        DevicesUtils.tryVibrate(view)
        onClick()
    }
    return view
}
