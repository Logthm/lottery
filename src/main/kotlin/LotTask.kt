package com.logs

import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChainBuilder
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class LotTask(private val lotteryFolder: File, private val imgFolder: File, private val waitForDeleteImgIdList: MutableList<Int>) : TimerTask() {
    override fun run() {
        runBlocking {
            try {
                if (lotteryFolder.listFiles().isNotEmpty()) {
                    for (file in lotteryFolder.listFiles()!!) {
                        val temp = fileToData(file)
                        val currentDateTime = LocalDateTime.now()
                        val endDateTime = LocalDateTime.ofInstant(temp.endTime.toInstant(), ZoneId.systemDefault())
                        val bot = Bot.getInstance(Config.botQQ)
                        val g = bot.getGroup(temp.group)
                        // 截止抽奖并开奖
                        if (!temp.isEnd && currentDateTime.isAfter(endDateTime)) {
                            val builder = MessageChainBuilder()
                            builder.add("编号为 ${temp.lotId} 的抽奖已结束\n")
                            if (temp.members.size > 0) {
                                builder.add("共有 ${temp.members.size} 人参加本次抽奖，中奖名单如下：\n")

                                val winnerHashSet = HashSet<Long>()
                                for (winner in roll(temp)) {
                                    winnerHashSet.add(winner)
                                    temp.winners.add(winner)
                                }

                                val winnerIndex = 0
                                for (map in temp.prize) {
                                    builder.add(map.key + "：\n")
                                    for (i in 0 until map.value) {
                                        builder.add(
                                            "${g?.get(temp.winners.get(winnerIndex))?.nameCardOrNick}(${
                                                temp.winners.get(
                                                    winnerIndex
                                                )
                                            })\n"
                                        )
                                    }
                                }

                                // 将获奖名单写入json
                                withContext(Dispatchers.IO) {
                                    FileWriter(lotteryFolder.path + "/" + temp.lotId + ".json")
                                }.run {
                                    GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm").create().toJson(endLot(temp), this)
                                    close()
                                }

                                builder.add("恭喜 ")
                                for (winner in winnerHashSet) {
                                    builder.add(At(winner))
                                }
                                builder.add("获奖")
                            } else {
                                builder.add("无人参与本次抽奖")

                                withContext(Dispatchers.IO) {
                                    FileWriter(lotteryFolder.path + "/" + temp.lotId + ".json")
                                }.run {
                                    GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm").create().toJson(endLot(temp), this)
                                    close()
                                }
                            }
                            val msg = builder.build()
                            g?.sendMessage(msg)
                        }
                        // 提示抽奖即将结束
                        if (!temp.hasRemind && currentDateTime.isAfter(endDateTime.minusHours(Config.remind_hour))) {

                            withContext(Dispatchers.IO) {
                                FileWriter(lotteryFolder.path + "/" + temp.lotId + ".json")
                            }.run {
                                GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm").create().toJson(remindLot(temp), this)
                                close()
                            }

                            val builder = MessageChainBuilder()
                            builder.add("编号为 ${temp.lotId} 的抽奖还有不到${Config.remind_hour}小时结束")
                            val msg = builder.build()
                            g?.sendMessage(msg)
                        }
                        // 删除过期抽奖（须在最后执行以避免保留时间为0时无法抽奖）
                        if (currentDateTime.isAfter(endDateTime.plusHours(Config.remain_hour))) {
                            file.delete()
                            deleteImg(temp.lotId, imgFolder)
                        }
                    }
                    if (waitForDeleteImgIdList.isNotEmpty()) {
                        for (file in imgFolder.listFiles()) {
                            for (id in waitForDeleteImgIdList) {
                                if (file.name.startsWith(id.toString())) {
                                    file.delete()
                                }
                            }
                        }
                        waitForDeleteImgIdList.clear()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}