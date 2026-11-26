package com.example.drivenext.data.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.example.drivenext.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executors

internal class Supabase(context: Context) {
    private val app = context.applicationContext
    private val host = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val prefs = app.getSharedPreferences("supabase_${host.hashCode()}", Context.MODE_PRIVATE)
    private val auth = "/auth/v1/"
    private val db = "/rest/v1/"

    companion object {
        const val REDIRECT = "com.example.drivenext://auth/callback"
        private val worker = Executors.newSingleThreadExecutor()
        private val main = Handler(Looper.getMainLooper())
    }

    fun <T> run(work: () -> T, done: (Result<T>) -> Unit) {
        worker.execute {
            val result = runCatching(work)
            main.post { done(result) }
        }
    }

    fun hasSession() = prefs.contains("refresh")

    fun signIn(email: String, password: String) {
        saveSession(json(auth + "token?grant_type=password", credentials(email, password)))
    }

    fun register(values: Map<String, String>) {
        val email = values.getValue("email").trim()
        if (registrationPending(email)) {
            signIn(email, values.getValue("password"))
        } else {
            val reply = json(auth + "signup", credentials(email, values.getValue("password")))
            prefs.edit().putString("registration", email).apply()
            check(reply.has("access_token")) {
                "Подтверди почту по ссылке из письма, вернись сюда и снова нажми «Далее»."
            }
            saveSession(reply)
        }

        val profile = obj(
            "first_name" to values["first_name"], "surname" to values["surname"],
            "patronymic" to values["patronymic"], "birth" to values["birth"],
            "gender" to values["gender"], "licence" to values["licence"], "issued" to values["issued"]
        )
        for (key in listOf("avatar", "licence_photo", "passport_photo")) {
            values[key]?.let { upload(key, Uri.parse(it)) }
        }
        json(db + "registration_profiles", profile.put("id", user().getString("id")),
            token = access(), prefer = "resolution=merge-duplicates")
        updateMetadata(obj("name" to "${values["first_name"]} ${values["surname"]}",
            "gender" to values["gender"], "has_avatar" to values.containsKey("avatar")))
        prefs.edit().remove("registration").apply()
    }

    fun registrationPending(email: String? = null): Boolean {
        val pending = prefs.getString("registration", null) ?: return false
        return email == null || pending.equals(email, ignoreCase = true)
    }

    fun googleUrl(): Uri = Uri.parse(host + auth + "authorize").buildUpon()
        .appendQueryParameter("provider", "google")
        .appendQueryParameter("redirect_to", REDIRECT)
        .appendQueryParameter("code_challenge", challenge("google"))
        .appendQueryParameter("code_challenge_method", "s256")
        .build()

    fun recover(email: String) {
        val path = Uri.parse(auth + "recover").buildUpon()
            .appendQueryParameter("redirect_to", REDIRECT).build()
        json(path.toString(), obj("email" to email, "code_challenge" to challenge("recovery"),
            "code_challenge_method" to "s256"))
    }

    private fun challenge(flow: String): String {
        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val verifier = base64(random)
        prefs.edit().putString("verifier", verifier).putString("flow", flow).apply()
        return base64(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
    }

    fun exchange(uri: Uri): Boolean {
        val code = uri.getQueryParameter("code")
            ?: error(uri.getQueryParameter("error_description") ?: "Вход не завершён.")
        val verifier = prefs.getString("verifier", null) ?: error("Начни вход заново.")
        val recovery = prefs.getString("flow", "") == "recovery"
        saveSession(json(auth + "token?grant_type=pkce", obj("auth_code" to code, "code_verifier" to verifier)))
        prefs.edit().remove("verifier").remove("flow")
            .putBoolean("needs_password", recovery).apply()
        return recovery
    }

    fun needsPassword() = prefs.getBoolean("needs_password", false)

    fun changePassword(password: String) {
        json(auth + "user", obj("password" to password), "PUT", access())
        prefs.edit().remove("needs_password").apply()
    }

    fun user() = json(auth + "user", token = access())

    fun updateMetadata(data: JSONObject) {
        json(auth + "user", obj("data" to data), "PUT", access())
    }

    fun signOut() {
        try {
            if (hasSession()) json(auth + "logout?scope=local", JSONObject(), token = access())
        } finally {
            prefs.edit().remove("access").remove("refresh").remove("expiry")
                .remove("needs_password").remove("user_id").apply()
        }
    }

    fun cars(query: String) = array(db + "rpc/search_cars", obj("term" to query))

    fun bookings() = array(db + "bookings?select=car:cars(model,brand,price)&order=created_at.desc")

    fun book(id: String) {
        json(db + "bookings", obj("car_id" to id.toLong()), token = access(),
            prefer = "resolution=ignore-duplicates")
    }

    fun saveAvatar(uri: Uri) {
        upload("avatar", uri)
        updateMetadata(obj("has_avatar" to true))
    }

    fun avatar(): ByteArray = request("/storage/v1/object/authenticated/${photoPath("avatar")}", token = access())

    private fun upload(name: String, uri: Uri) {
        val image = ImageDecoder.decodeBitmap(ImageDecoder.createSource(app.contentResolver, uri)) { decoder, info, _ ->
            val size = info.size
            val scale = 1600f / maxOf(size.width, size.height)
            if (scale < 1f) decoder.setTargetSize(
                (size.width * scale).toInt().coerceAtLeast(1), (size.height * scale).toInt().coerceAtLeast(1)
            )
        }
        val data = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 88, data)
        request("/storage/v1/object/${photoPath(name)}", "POST", access(),
            data.toByteArray(), "image/jpeg", upsert = true)
    }

    private fun photoPath(name: String) = "drive-private/${prefs.getString("user_id", "")}/$name.jpg"

    private fun saveSession(reply: JSONObject) {
        prefs.edit().putString("access", reply.getString("access_token"))
            .putString("refresh", reply.getString("refresh_token"))
            .putString("user_id", reply.getJSONObject("user").getString("id"))
            .putLong("expiry", System.currentTimeMillis() + reply.optLong("expires_in", 3600) * 1000)
            .apply()
    }

    private fun access(): String {
        val refresh = prefs.getString("refresh", null) ?: error("Войди в аккаунт.")
        if (System.currentTimeMillis() + 60000 >= prefs.getLong("expiry", 0)) {
            try {
                saveSession(json(auth + "token?grant_type=refresh_token", obj("refresh_token" to refresh)))
            } catch (error: SupabaseError) {
                if (error.status == 400 || error.status == 401) prefs.edit().clear().apply()
                throw error
            }
        }
        return prefs.getString("access", "").orEmpty()
    }

    private fun credentials(email: String, password: String) = obj("email" to email, "password" to password)

    private fun json(path: String, body: JSONObject? = null, method: String = if (body == null) "GET" else "POST",
                     token: String? = null, prefer: String? = null): JSONObject {
        val text = request(path, method, token, body?.toString()?.toByteArray(), prefer = prefer)
            .toString(Charsets.UTF_8)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun array(path: String, body: JSONObject? = null) = JSONArray(
        request(path, if (body == null) "GET" else "POST", access(), body?.toString()?.toByteArray())
            .toString(Charsets.UTF_8)
    )

    private fun request(path: String, method: String = "GET", token: String? = null, body: ByteArray? = null,
                        type: String = "application/json", prefer: String? = null, upsert: Boolean = false): ByteArray {
        check(host.startsWith("https://") && BuildConfig.SUPABASE_KEY.isNotBlank()) {
            "Укажи URL и публичный ключ Supabase в local.properties."
        }
        val connection = URL(host + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            prefer?.let { connection.setRequestProperty("Prefer", it) }
            if (upsert) connection.setRequestProperty("x-upsert", "true")
            body?.let {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", type)
                connection.outputStream.use { stream -> stream.write(it) }
            }
            val status = connection.responseCode
            if (status in 200..299) return connection.inputStream.use { it.readBytes() }
            val message = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val error = runCatching { JSONObject(message) }.getOrNull()
            val detail = listOf("msg", "message", "error_description", "error")
                .firstNotNullOfOrNull { key -> error?.optString(key)?.takeIf(String::isNotBlank) }
            throw SupabaseError(status, detail ?: "Ошибка Supabase ($status).")
        } finally {
            connection.disconnect()
        }
    }

    private fun base64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    private fun obj(vararg pairs: Pair<String, Any?>) = JSONObject(mapOf(*pairs))
}

internal class SupabaseError(val status: Int, message: String) : Exception(message)
