package dev.aleian.pockethid.ui

import android.content.SharedPreferences
import java.lang.reflect.Proxy

object FakePrefs {
    val map = mutableMapOf<String, Any>()
    
    fun createFakePrefs(): SharedPreferences {
        val editor = Proxy.newProxyInstance(
            FakePrefs::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "putBoolean" -> { map[args[0] as String] = args[1] as Boolean; proxy }
                "putInt" -> { map[args[0] as String] = args[1] as Int; proxy }
                "putFloat" -> { map[args[0] as String] = args[1] as Float; proxy }
                "putLong" -> { map[args[0] as String] = args[1] as Long; proxy }
                "putString" -> { map[args[0] as String] = args[1] as String; proxy }
                "apply", "commit" -> true
                "clear" -> { map.clear(); proxy }
                else -> proxy
            }
        } as SharedPreferences.Editor

        return Proxy.newProxyInstance(
            FakePrefs::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getBoolean" -> map[args[0] as String] ?: args[1] as Boolean
                "getInt" -> map[args[0] as String] ?: args[1] as Int
                "getFloat" -> map[args[0] as String] ?: args[1] as Float
                "getLong" -> map[args[0] as String] ?: args[1] as Long
                "getString" -> map[args[0] as String] ?: args[1] as String
                "edit" -> editor
                else -> null
            }
        } as SharedPreferences
    }
}
