package com.example.drivenext.presentation.auth

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.drivenext.R
import java.util.Calendar
import java.util.Locale

class AuthUi(
    val activity: AppCompatActivity,
    private val values: MutableMap<String, String>,
    private val onBack: () -> Unit,
    private val onPhoto: (String) -> Unit
) {
    val purple = Color.rgb(45, 17, 72)
    val hintColor = Color.rgb(108, 120, 144)
    private val border = Color.rgb(207, 213, 223)
    val body = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(28), 0, dp(28), dp(16))
    }
    private val bottom = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(28), dp(8), dp(28), dp(62))
    }

    fun attach() {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        activity.setContentView(root)
        val scroll = ScrollView(activity).apply { isFillViewport = true }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        scroll.addView(body)
        root.addView(bottom)
    }

    fun title(title: String, back: Boolean = false) {
        val row = FrameLayout(activity)
        body.addView(row, LinearLayout.LayoutParams(-1, dp(88)))
        row.addView(TextView(activity).apply {
            text = title
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 25, 25))
            gravity = Gravity.CENTER
        }, FrameLayout.LayoutParams(-1, -1))
        if (back) row.addView(TextView(activity).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.BLACK)
            contentDescription = "Назад"
            setOnClickListener { onBack() }
        }, FrameLayout.LayoutParams(dp(48), -1, Gravity.START))
    }

    fun spacer(height: Int) { body.addView(View(activity), LinearLayout.LayoutParams(1, dp(height))) }

    fun text(label: String, size: Float = 14f, color: Int = Color.DKGRAY, centered: Boolean = false): TextView =
        TextView(activity).apply {
            text = label
            textSize = size
            setTextColor(color)
            gravity = if (centered) Gravity.CENTER else Gravity.START
            body.addView(this, LinearLayout.LayoutParams(-1, -2))
        }

    fun field(
        label: String,
        placeholder: String,
        key: String,
        password: Boolean = false,
        keyboard: Int = InputType.TYPE_CLASS_TEXT
    ): EditText {
        text(label)
        val frame = FrameLayout(activity).apply { background = outline() }
        body.addView(frame, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(5) })
        val edit = EditText(activity).apply {
            setSingleLine(true)
            setTextSize(14f)
            setTextColor(Color.BLACK)
            setHintTextColor(hintColor)
            hint = placeholder
            background = null
            setPadding(dp(14), 0, if (password) dp(46) else dp(14), 0)
            inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else keyboard
            setText(values[key].orEmpty())
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    values[key] = s.toString()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        frame.addView(edit, FrameLayout.LayoutParams(-1, -1))
        if (password) {
            val eye = ImageView(activity).apply {
                setImageResource(R.drawable.ic_eye_hidden)
                contentDescription = "Показать пароль"
                setPadding(dp(10), dp(10), dp(10), dp(10))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val wasVisible = edit.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    edit.inputType = if (wasVisible) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    setImageResource(if (wasVisible) R.drawable.ic_eye_hidden else R.drawable.ic_eye_visible)
                    contentDescription = if (wasVisible) "Показать пароль" else "Скрыть пароль"
                    edit.setSelection(edit.text.length)
                }
            }
            frame.addView(eye, FrameLayout.LayoutParams(dp(44), -1, Gravity.END))
        }
        spacer(17)
        return edit
    }

    fun dateField(label: String, key: String) {
        text(label)
        val date = TextView(activity).apply {
            text = "▦  " + (values[key] ?: "DD/MM/YYYY")
            textSize = 14f
            setTextColor(hintColor)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = outline()
            setOnClickListener {
                val today = Calendar.getInstance()
                DatePickerDialog(activity, { _, year, month, day ->
                    values[key] = "%02d/%02d/%04d".format(Locale.US, day, month + 1, year)
                    text = "▦  ${values[key]}"
                }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
            }
        }
        body.addView(date, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(5) })
        spacer(17)
    }

    fun bottomButton(label: String, action: () -> Unit) {
        val view = TextView(activity).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(purple)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        bottom.addView(view, LinearLayout.LayoutParams(-1, dp(50)))
    }

    fun upload(label: String, key: String) {
        text(label)
        val row = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setOnClickListener { onPhoto(key) }
        }
        val icon = ImageView(activity).apply {
            if (values.containsKey(key)) setImageURI(Uri.parse(values[key]))
            else {
                setImageResource(android.R.drawable.ic_menu_upload)
                imageTintList = ColorStateList.valueOf(hintColor)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = outline()
            contentDescription = label
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(50), dp(50)))
        row.addView(TextView(activity).apply {
            text = if (values.containsKey(key)) "Фото загружено" else "Загрузить фото"
            textSize = 14f
            setTextColor(hintColor)
            setPadding(dp(15), 0, 0, 0)
        })
        body.addView(row, LinearLayout.LayoutParams(-1, dp(64)))
        spacer(8)
    }

    fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
    fun rounded(color: Int, radius: Int = 7) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    fun outline() = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(7).toFloat()
        setStroke(dp(1), border)
    }
    fun avatarTint() = ColorStateList.valueOf(border)
}
