package com.example.drivenext.presentation.catalog

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drivenext.data.auth.Supabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import com.example.drivenext.presentation.auth.AuthActivity

class CatalogActivity : AppCompatActivity() {
    private val account by lazy { Supabase(this) }
    private val api by lazy { CatalogApi(account) }
    private var user = JSONObject()
    private var avatar: Bitmap? = null
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("catalog_ui", MODE_PRIVATE) }
    private var query = ""
    private var page = "home"
    private var carId = 0L
    private var bookingId = 0L
    private var period = RentalPeriod()
    private var rentalBack = "home"
    private var bookingsBack = "settings"
    private var carBack = "home"
    private lateinit var content: LinearLayout
    private lateinit var status: LinearLayout
    private lateinit var list: RecyclerView
    private lateinit var adapter: CarAdapter
    private val delayedSearch = Runnable {
        if (query.isBlank()) loadCars() else openResults()
    }

    private val avatarPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) account.run({
            account.saveAvatar(uri)
            val bytes = account.avatar()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }) { result ->
            if (!isFinishing && !isDestroyed) result.onSuccess {
                avatar = it
                if (page == "profile" || page == "settings") show(page)
            }.onFailure { toast(it.message ?: "Не удалось сохранить фото.") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!account.hasSession() || account.needsPassword()) {
            openLogin()
            return
        }
        account.run({
            val profile = account.user()
            val data = profile.optJSONObject("user_metadata")
            val picture = if (data?.optBoolean("has_avatar") == true) runCatching {
                val bytes = account.avatar()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull() else null
            profile to picture
        }) { result ->
            if (!isFinishing && !isDestroyed) result.onSuccess { (profile, picture) ->
                user = profile
                avatar = picture
                if (page == "settings" || page == "profile" || page == "add") show(page)
            }.onFailure { if (!account.hasSession()) openLogin() }
        }
        query = savedInstanceState?.getString("query") ?: prefs.getString("last_query", "").orEmpty()
        period = RentalPeriod(savedInstanceState)
        carId = savedInstanceState?.getLong("car_id") ?: 0L
        bookingId = savedInstanceState?.getLong("booking_id") ?: 0L
        carBack = savedInstanceState?.getString("car_back") ?: "home"
        rentalBack = savedInstanceState?.getString("rental_back") ?: "home"
        bookingsBack = savedInstanceState?.getString("bookings_back") ?: "settings"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (page) {
                    "home" -> finish()
                    "results" -> show("home")
                    "profile", "add" -> show("settings")
                    "bookings" -> show(bookingsBack)
                    "booking" -> show("bookings")
                    "details" -> show(carBack)
                    "reserve" -> show(rentalBack)
                    else -> openHome()
                }
            }
        })
        show(savedInstanceState?.getString("catalog_page") ?: if (query.isBlank()) "home" else "results")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("catalog_page", page)
        outState.putString("query", query)
        outState.putLong("car_id", carId)
        outState.putLong("booking_id", bookingId)
        outState.putString("car_back", carBack)
        outState.putString("rental_back", rentalBack)
        outState.putString("bookings_back", bookingsBack)
        period.save(outState)
    }

    private fun openHome() {
        query = ""
        saveQuery()
        show("home")
    }

    private fun show(destination: String) {
        handler.removeCallbacks(delayedSearch)
        api.cancel()
        page = destination

        val root = column().apply { setBackgroundColor(Color.WHITE) }
        content = column()
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        if (page in listOf("home", "results", "details", "favorites", "settings", "profile", "add")) {
            val activeTab = if (page == "details") carBack else page
            val nav = catalogNavigation(activeTab) { target ->
                if (target == "home") openHome() else show(target)
            }
            root.addView(nav, LinearLayout.LayoutParams(-1, dp(56)))
        }
        setContentView(root)
        window.statusBarColor = if (page == "home") Color.rgb(246, 242, 252) else Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val rental = RentalPages(this, content, account)
        when (page) {
            "home", "results", "favorites" -> catalog()
            "details" -> rental.details(carId, { show(carBack) }, ::startRental)
            "reserve" -> rental.checkout(carId, period, { show(rentalBack) }) {
                carId = 0
                show("trip")
            }
            "trip" -> rental.success({
                bookingsBack = "home"
                show("bookings")
            }, ::openHome)
            "bookings" -> rental.bookings({ show(bookingsBack) }) {
                bookingId = it
                show("booking")
            }
            "booking" -> rental.booking(bookingId, user.optJSONObject("user_metadata")
                ?.optString("full_name").orEmpty()) { show("bookings") }
            else -> {
                val pages = CatalogPages(this, content, account, user, avatar, { target ->
                    if (target == "bookings") bookingsBack = "settings"
                    show(target)
                }, { avatarPicker.launch(arrayOf("image/*")) }, ::signOut)
                when (page) {
                    "settings" -> pages.settings()
                    "profile" -> pages.profile()
                    "add" -> pages.addCar()
                }
            }
        }
    }

    private fun catalog() {
        if (page == "home") {
            val header = column().apply {
                setBackgroundColor(Color.rgb(246, 242, 252))
                setPadding(dp(19), dp(28), dp(19), dp(18))
                isFocusableInTouchMode = true
            }
            val search = SearchView(this).apply {
                setIconifiedByDefault(false)
                isSubmitButtonEnabled = true
                queryHint = "Введите марку автомобиля"
                background = shape(Color.WHITE, 8)
                elevation = dp(3).toFloat()
                setQuery(query, false)
            }
            header.addView(search, LinearLayout.LayoutParams(-1, dp(46)))
            header.addView(label("Давайте найдем автомобиль", 17f, ink, true).apply {
                setPadding(0, dp(28), 0, 0)
            })
            content.addView(header)
            header.requestFocus()
            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(text: String): Boolean {
                    query = text
                    saveQuery()
                    api.cancel()
                    handler.removeCallbacks(delayedSearch)
                    handler.postDelayed(delayedSearch, 1200L)
                    return true
                }

                override fun onQueryTextSubmit(text: String): Boolean {
                    query = text.trim()
                    saveQuery()
                    openResults()
                    return true
                }
            })
        } else if (page == "favorites") {
            catalogHeading(content, "Избранное")
        } else {
            catalogHeading(content, "Результаты поиска") { show("home") }
        }
        status = column().apply { gravity = Gravity.CENTER }
        content.addView(status, LinearLayout.LayoutParams(-1, -2))
        adapter = CarAdapter({ openCar(it, "reserve") }, { openCar(it, "details") })
        list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@CatalogActivity)
            adapter = this@CatalogActivity.adapter
            setPadding(dp(19), dp(if (page == "home") 28 else 6), dp(19), 0)
            clipToPadding = false
        }
        content.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        loadCars()
    }

    private fun openResults() {
        currentFocus?.let { view ->
            getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(view.windowToken, 0)
        }
        show(if (query.isBlank()) "home" else "results")
    }

    private fun saveQuery() { prefs.edit().putString("last_query", query).apply() }

    private fun loadCars() {
        val favorites = page == "favorites"
        val term = if (favorites) "" else query.trim()
        val ids: (() -> List<Long>)? = if (favorites) {
            { FavoritesDb.get(this).favorites().ids(account.ownerKey()) }
        } else null
        status.removeAllViews()
        status.addView(ProgressBar(this), LinearLayout.LayoutParams(dp(40), dp(48)))
        adapter.show(emptyList())
        api.load(term, ids) { result ->
            status.removeAllViews()
            result.fold(
                onSuccess = { cars ->
                    adapter.show(cars)
                    if (cars.isEmpty()) status.addView(
                        label(if (favorites) "В избранном пока пусто" else "Автомобили не найдены", 15f)
                    )
                },
                onFailure = {
                    if (!account.hasSession()) { openLogin(); return@fold }
                    val error = if (term.isBlank()) "Не удалось загрузить данные. Попробуйте снова."
                    else "Не удалось выполнить поиск. Попробуйте снова."
                    status.addView(label(error, 14f).apply {
                        gravity = Gravity.CENTER
                        setPadding(dp(16), dp(16), dp(16), dp(8))
                    })
                    status.addView(action("Повторить", true, ::loadCars),
                        LinearLayout.LayoutParams(dp(140), dp(42)))
                }
            )
        }
    }

    private fun openCar(car: Car, destination: String) {
        carId = car.id.toLong()
        carBack = page
        if (destination == "reserve") startRental() else show(destination)
    }

    private fun startRental() {
        period = RentalPeriod()
        rentalBack = page
        show("reserve")
    }

    private fun signOut() {
        account.run({ runCatching { account.signOut() } }) {
            if (!isFinishing && !isDestroyed) openLogin()
        }
    }

    private fun openLogin() {
        startActivity(Intent(this, AuthActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        handler.removeCallbacks(delayedSearch)
        api.cancel()
        super.onDestroy()
    }
}
