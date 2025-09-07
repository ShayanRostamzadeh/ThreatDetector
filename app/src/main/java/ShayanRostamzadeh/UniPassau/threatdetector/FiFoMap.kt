/*
this class makes an instance of a filo (first in last out) map.
since the maps in kotlin store duplicates by default, the class
takes care of the duplicates prevention.
the instance of this class is used in RetrievedAppsDataManager object
to keep track of the app/package name and their IP address in use
*/

package ShayanRostamzadeh.UniPassau.threatdetector

import android.os.Build
import androidx.annotation.RequiresApi

class FiFoMap<K, V>(private val maxSize: Int) {
//    val maximumSize = maxSize
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
            val lastKey = stack.removeFirst() // FIFO: remove oldest
            map.remove(lastKey)
        }
    }

    operator fun get(key: K): V? = map[key]

    fun getAllData() = map

    fun remove(key: K) {
        stack.remove(key)
        map.remove(key)
    }

    override fun toString(): String = map.toString()
}
