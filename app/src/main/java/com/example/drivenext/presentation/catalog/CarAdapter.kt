package com.example.drivenext.presentation.catalog

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drivenext.R

internal val purple = Color.rgb(45, 20, 73)
internal val ink = Color.rgb(21, 30, 47)
internal val muted = Color.rgb(99, 106, 120)
internal fun Context.dp(n: Int) = (n * resources.displayMetrics.density + 0.5f).toInt()

internal fun Context.column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

internal fun Context.label(value: String, size: Float, color: Int = ink, bold: Boolean = false) =
    TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

internal fun Context.shape(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
    setColor(fill)
    cornerRadius = dp(radius).toFloat()
    stroke?.let { setStroke(dp(1), it) }
}

internal fun Context.picture(resource: Int) = ImageView(this).apply {
    scaleType = ImageView.ScaleType.FIT_CENTER
    setImageResource(resource)
}

internal fun Context.action(text: String, filled: Boolean, click: () -> Unit) =
    label(text, 15f, if (filled) Color.WHITE else purple).apply {
        gravity = Gravity.CENTER
        background = shape(if (filled) purple else Color.WHITE, 8, purple)
        setOnClickListener { click() }
        isFocusable = true
    }

internal class CarAdapter(
    private val book: (Car) -> Unit,
    private val details: (Car) -> Unit
) : RecyclerView.Adapter<CarAdapter.Holder>() {
    private var cars = emptyList<Car>()

    fun show(items: List<Car>) {
        cars = items
        notifyDataSetChanged()
    }

    override fun getItemCount() = cars.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(parent.context, book, details)

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(cars[position])

    class Holder(
        context: Context, book: (Car) -> Unit, details: (Car) -> Unit
    ) : RecyclerView.ViewHolder(context.column()) {
        private val title = context.label("", 17f, Color.BLACK, true)
        private val brand = context.label("", 13f, muted)
        private val price = context.label("", 14f, Color.BLACK, true)
        private val transmission = context.label("", 12f, muted)
        private val fuel = context.label("", 12f, muted)
        private val carImage = context.picture(R.drawable.car)
        private var car: Car? = null

        init {
            with(context) {
                val card = itemView as LinearLayout
                card.layoutParams = RecyclerView.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
                card.background = shape(Color.WHITE, 8, Color.BLACK)
                card.setPadding(dp(22), dp(26), dp(22), dp(22))

                val area = FrameLayout(this)
                val info = column()
                info.addView(title)
                info.addView(brand)
                info.addView(price, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(7) })
                area.addView(carImage, FrameLayout.LayoutParams(dp(151), dp(70),
                    Gravity.END or Gravity.CENTER_VERTICAL))
                area.addView(info)
                card.addView(area, LinearLayout.LayoutParams(-1, dp(105)))

                val specs = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(picture(R.drawable.shift), LinearLayout.LayoutParams(dp(17), dp(17)))
                    addView(transmission, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(4) })
                    addView(picture(R.drawable.fuel), LinearLayout.LayoutParams(dp(17), dp(17)).apply {
                        leftMargin = dp(16)
                    })
                    addView(fuel, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(4) })
                }
                card.addView(specs)
                val row = LinearLayout(this)
                row.addView(action("Забронировать", true) { car?.let(book) },
                    LinearLayout.LayoutParams(0, dp(40), 1f).apply { rightMargin = dp(8) })
                row.addView(action("Детали", false) { car?.let(details) },
                    LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(8) })
                card.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(36) })
            }
        }

        fun bind(item: Car) {
            car = item
            carImage.setImageResource(if (item.brand == "Tesla") R.drawable.lab4_tesla else R.drawable.car)
            title.text = item.model
            brand.text = item.brand
            price.text = "${item.price}₽ в день"
            transmission.text = item.transmission
            fuel.text = item.fuel
        }
    }
}
