package ShayanRostamzadeh.UniPassau.threatdetector

import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.max

class FiloMap<K, V>(private val maxSize: Int) {
    val maximumSize = maxSize
     val map = mutableMapOf<K, V>()
    private val stack = mutableListOf<K>() // acts like a stack

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun put(key: K, value: V) {
        if (map.containsKey(key)) {
            stack.remove(key)
        }
        stack.add(key)
        map[key] = value

        if (map.size > maxSize) {
            val lastKey = stack.removeLast() // FILO: remove newest
            map.remove(lastKey)
        }
    }

    fun get(key: K): V? = map[key]

    fun remove(key: K) {
        stack.remove(key)
        map.remove(key)
    }

    override fun toString(): String = map.toString()
}
