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
import com.example.drivenext.data.auth.LocalAccountRepository
import com.example.drivenext.presentation.auth.AuthActivity

class CatalogActivity : AppCompatActivity() {
    private val api = CatalogApi()
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("local_account", MODE_PRIVATE) }
    private var query = ""
    private var page = "home"
    private var selectedCar: Car? = null
    private var carBack = "home"
    private lateinit var content: LinearLayout
    private lateinit var status: LinearLayout
    private lateinit var list: RecyclerView
    private lateinit var adapter: CarAdapter
    private val delayedSearch = Runnable {
        if (query.isBlank()) loadCars() else openResults()
    }

    private val avatarPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                prefs.edit().putString("avatar", uri.toString()).apply()
                show("profile")
            } catch (_: SecurityException) {
                toast("Не удалось сохранить фото.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = prefs.getString("last_query", "").orEmpty()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (page) {
                    "home" -> finish()
                    "results" -> show("home")
                    "profile", "bookings", "add" -> show("settings")
                    "details", "reserve" -> show(carBack)
                    else -> openHome()
                }
            }
        })
        show(if (query.isBlank()) "home" else "results")
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
        val nav = catalogNavigation(page) { destination ->
            if (destination == "home") openHome() else show(destination)
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(56)))
        setContentView(root)
        window.statusBarColor = if (page == "home") Color.rgb(246, 242, 252) else Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val pages = CatalogPages(this, content, prefs, ::show,
            { avatarPicker.launch(arrayOf("image/*")) }, ::signOut)
        when (page) {
            "home", "results" -> catalog()
            "settings" -> pages.settings()
            "profile" -> pages.profile()
            "details" -> selectedCar?.let { pages.carPage(it, false, carBack) }
            "reserve" -> selectedCar?.let { pages.carPage(it, true, carBack) }
            "bookings" -> pages.bookings()
            "add" -> pages.addCar()
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
        val term = query.trim()
        status.removeAllViews()
        status.addView(ProgressBar(this), LinearLayout.LayoutParams(dp(40), dp(48)))
        adapter.show(emptyList())
        api.load(term) { result ->
            status.removeAllViews()
            result.fold(
                onSuccess = { cars ->
                    adapter.show(cars)
                    if (cars.isEmpty()) status.addView(label("Автомобили не найдены", 15f))
                },
                onFailure = {
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
        selectedCar = car
        carBack = page
        show(destination)
    }

    private fun signOut() {
        LocalAccountRepository(applicationContext).signOut()
        startActivity(Intent(this, AuthActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        handler.removeCallbacks(delayedSearch)
        api.close()
        super.onDestroy()
    }
}
