package com.logs

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Config : AutoSavePluginConfig("config") {
//    // 指令
//    val HELP: MutableList<String> by value(mutableListOf(settings.HELP))
//    val add_lottery: MutableList<String> by value(mutableListOf(settings.add_lottery))
//    val remove_lottery: MutableList<String> by value(mutableListOf(settings.remove_lottery))
//    val list_lottery: MutableList<String> by value(mutableListOf(settings.list_lottery))
//    val list_lottery_members: MutableList<String> by value(mutableListOf(settings.list_lottery_members))
//    val lottery_detail: MutableList<String> by value(mutableListOf(settings.lottery_detail))
//    val enable_lottery: MutableList<String> by value(mutableListOf(settings.enable_lottery))
//    val disable_lottery: MutableList<String> by value(mutableListOf(settings.disable_lottery))

    val adminQQ: Long by value<Long>(123123123)
    val botQQ: Long by value<Long>(123456789)
    val whiteGroupList: MutableList<Long> by value(mutableListOf(123456789))

    // 非 adminQQ 是否可以创建抽奖
    val enable_for_others: Boolean by value(false)


    // 抽奖结束后，保留的时间
    val remain_hour: Long by value(Settings.remain_hour)

    // 在抽奖截止前多久进行提醒
    val remind_hour: Long by value(Settings.remind_hour)

}