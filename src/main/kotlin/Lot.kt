package com.logs

import java.util.*
import kotlin.collections.HashMap

//interface JSONConvertable {
//    fun toJSON(): String = Gson().toJson(this)
//}
//
//inline fun <reified T: JSONConvertable> String.toObject(): T = Gson().fromJson(this, T::class.java)

data class Lot(
    val lotId: Int,
    val group: Long,
    val creator: Long,
    val description: String,
    val endTime: Date,
    val type: Int, // 0：中奖人可重复 1：中奖人不可重复
    val winnerNum: Int,
    val members: MutableList<String>,
    val winners: MutableList<Long>,
    val imgNames: MutableList<String>,
    val prize: HashMap<String, Int>,
    val hasRemind: Boolean,
    val isEnd: Boolean = false
)