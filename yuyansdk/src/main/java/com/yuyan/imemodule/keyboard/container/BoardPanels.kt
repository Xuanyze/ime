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

/**
 * 班牌横屏三栏布局的左栏：输入方案选择。
 * 固定三项：26键全拼 / 双拼 / 9键拼音，高亮当前方案，点击直接切换 Rime schema。
 * 复用 InputModeSwitcher.switchModeForSetting（与设置页"选择输入方案"同一条链路）。
 */
@SuppressLint("ViewConstructor")
class SchemeRail(context: Context, private val inputView: InputView) : LinearLayout(context) {

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
    )

    private val titleViews: List<TextView>

    init {
        orientation = VERTICAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
        titleViews = options.map { option ->
            boardPanelButton(option.title, EnvironmentSingleton.instance.keyTextSize) { switchTo(option) }
        }
        titleViews.forEach { addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)) }
        refresh()
    }

    private fun switchTo(option: SchemeOption) {
        InputModeSwitcher.switchModeForSetting(option.value())
        inputView.resetToIdleState()
        refresh()
    }

    /** 当前方案高亮（每次键盘切换后由 InputView.updateCandidateBar 调用） */
    fun refresh() {
        titleViews.forEachIndexed { index, view ->
            val active = runCatching { options[index].match() }.getOrDefault(false)
            val theme = ThemeManager.activeTheme
            (view.background as? GradientDrawable)?.setColor(if (active) theme.accentKeyBackgroundColor else theme.functionKeyBackgroundColor)
            view.setTextColor(if (active) theme.accentKeyTextColor else theme.keyTextColor)
        }
    }
}

/**
 * 班牌横屏三栏布局的右栏：数字小键盘 + 编辑功能。
 * 左半：数字小键盘（7-9/4-6/1-3/.0退格）与常用中文符号；右半：方向键/Home/End/Delete 与 Ctrl 组合键。
 * 所有行为复用现有按键链路（InputView 的 sendKeyEvent / processKeyUp / 编辑键处理）。
 */
@SuppressLint("ViewConstructor")
class FunctionColumn(context: Context, private val inputView: InputView) : LinearLayout(context) {

    init {
        orientation = HORIZONTAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
        val textSize = EnvironmentSingleton.instance.keyTextSmallSize

        fun row(vararg buttons: View): LinearLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            buttons.forEach { addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)) }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val numberPad = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(row(boardPanelButton("7", textSize) { inputView.panelCommitText("7") }, boardPanelButton("8", textSize) { inputView.panelCommitText("8") }, boardPanelButton("9", textSize) { inputView.panelCommitText("9") }))
            addView(row(boardPanelButton("4", textSize) { inputView.panelCommitText("4") }, boardPanelButton("5", textSize) { inputView.panelCommitText("5") }, boardPanelButton("6", textSize) { inputView.panelCommitText("6") }))
            addView(row(boardPanelButton("1", textSize) { inputView.panelCommitText("1") }, boardPanelButton("2", textSize) { inputView.panelCommitText("2") }, boardPanelButton("3", textSize) { inputView.panelCommitText("3") }))
            addView(row(boardPanelButton(".", textSize) { inputView.panelCommitText(".") }, boardPanelButton("0", textSize) { inputView.panelCommitText("0") }, boardPanelButton("⌫", textSize) { inputView.panelBackspace() }))
            addView(row(boardPanelButton("，", textSize) { inputView.panelCommitText("，") }, boardPanelButton("。", textSize) { inputView.panelCommitText("。") }, boardPanelButton("？", textSize) { inputView.panelCommitText("？") }))
            addView(row(boardPanelButton("！", textSize) { inputView.panelCommitText("！") }, boardPanelButton("：", textSize) { inputView.panelCommitText("：") }, boardPanelButton("、", textSize) { inputView.panelCommitText("、") }))
        }

        val editPad = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(row(boardPanelButton("↑", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_DPAD_UP) }, boardPanelButton("↓", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN) }))
            addView(row(boardPanelButton("←", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT) }, boardPanelButton("→", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT) }))
            addView(row(boardPanelButton("Home", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_MOVE_START) }, boardPanelButton("End", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_MOVE_END) }))
            addView(row(boardPanelButton("Del", textSize) { inputView.panelSendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL) }, boardPanelButton("⌫", textSize) { inputView.panelBackspace() }))
            addView(row(boardPanelButton("Ctrl+A", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_SELECT_ALL) }, boardPanelButton("Ctrl+C", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_COPY) }))
            addView(row(boardPanelButton("Ctrl+X", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_CUT) }, boardPanelButton("Ctrl+V", textSize) { inputView.panelUserDefKey(InputModeSwitcher.USER_KEYCODE_PASTE) }))
        }

        addView(numberPad, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.2f))
        addView(editPad, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
    }
}

/** 班牌侧栏按钮：中性圆角、主题配色、明显按压反馈；长标签自动缩小字号且不折行 */
private fun View.boardPanelButton(label: String, textSizePx: Int, onClick: () -> Unit): TextView {
    val view = TextView(context)
    view.gravity = Gravity.CENTER
    view.text = label
    view.maxLines = 1
    val fittedSize = when {
        label.length <= 2 -> textSizePx
        label.length <= 4 -> (textSizePx * 0.72f).toInt()
        else -> (textSizePx * 0.58f).toInt()
    }
    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, fittedSize.toFloat())
    view.setTextColor(ThemeManager.activeTheme.keyTextColor)
    view.background = GradientDrawable().apply {
        cornerRadius = dp(8).toFloat()
        setColor(ThemeManager.activeTheme.functionKeyBackgroundColor)
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
