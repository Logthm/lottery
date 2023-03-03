package com.logs

import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.selectMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.info
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.text.Format
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.HashMap


object Lottery : KotlinPlugin(
    JvmPluginDescription(
        id = "com.logs.lottery",
        name = "lottery",
        version = "0.1.1",
    ) {
        author("Logs")
    }
) {

    override fun onEnable() {
        logger.info { "Lottery Plugin Loaded" }
        // 创建存储区
        val lotteryFolder = dataFolder.resolve("lottery")
        when {
            lotteryFolder.exists() -> logger.info("Lottery Folder: ${lotteryFolder.path}")
            else -> {
                logger.info("Can't find lottery folder")
                lotteryFolder.mkdirs()
                logger.info("Creat lottery folder: ${lotteryFolder.path}")

            }
        }
        val imgFolder = dataFolder.resolve("img")
        when {
            imgFolder.exists() -> logger.info("img Folder: ${imgFolder.path}")
            else -> {
                logger.info("Can't find img folder")
                imgFolder.mkdirs()
                logger.info("Creat img folder: ${imgFolder.path}")

            }
        }
        // 载入配置文件
        reloadPluginConfig(Config)
        // 填写后，自动清除示例
        if (Config.whiteGroupList.size > 1) {
            Config.whiteGroupList.remove(123456789)
        }

        val waitForDeleteImgIdList = mutableListOf<Int>()

        launch {
            // TODO: 私聊 join / quit 功能
            // 实现指令
            globalEventChannel().subscribeAlways<FriendMessageEvent> {
                if (message.contentEquals("/lot version"))
                    friend.sendMessage(Lottery.description.version.toString())
            }

            globalEventChannel().subscribeAlways<GroupMessageEvent> {
                // 群内管理员消息
                if (sender.id == Config.adminQQ || sender.permission.isOperator()) {
                    if (message.contentEquals(Settings.enable_lottery)) {
                        if (Config.whiteGroupList.contains(group.id)) {
                            group.sendMessage("该功能已在该群启用，无需重复启用")
                        } else {
                            Config.whiteGroupList.add(group.id)
                            group.sendMessage("启用成功")
                        }
                    }
                    if (message.contentEquals(Settings.disable_lottery)) {
                        if (Config.whiteGroupList.contains(group.id)) {
                            Config.whiteGroupList.remove(group.id)
                            group.sendMessage("关闭成功")
                        } else {
                            group.sendMessage("该功能未启用，无需关闭")
                        }

                    }
                }
                // 白名单群组内消息
                if (group.id in Config.whiteGroupList) {
                    // /lot help
                    if (message.contentEquals(Settings.HELP)) {
                        group.sendMessage(Settings.HELP_INFO)
                    }

                    // /lot add
                    if (message.contentToString().startsWith(Settings.add_lottery)) {
                        if (sender.id == Config.adminQQ || Config.enable_for_others) {
                            // 创建lot类
                            val description: String
                            val endTime: Date
                            val type: Int
                            val winnerNum: Int
                            val sdf: Format = SimpleDateFormat("yyyy-MM-dd-hh-mm")
                            val id = idGenerator(lotteryFolder)
                            val imgNames: MutableList<String> = mutableListOf()
                            val prize = HashMap<String, Int>()

                            group.sendMessage("请在120秒内发送该抽奖的描述信息\n（支持图片、文字以及@）")
                            val message1: MessageChain = selectMessages {
                                has<Image> { message }
                                has<PlainText> { message }
                                has<At> { message }
                                has<AtAll> { message }
                                has<Face> { message }
                                timeout(120_000) { group.sendMessage("未收到消息，请重新创建！"); null }
                            } ?: return@subscribeAlways
                            val descriptionChain = MessageChainBuilder()
                            var imgIndex = 0
                            for (element in message1) {
                                // 处理图片
                                if (element is Image) {
                                    // 下载图片
                                    val imgUrl = URL(element.queryUrl())
                                    imgNames.add(downloadImg(imgUrl, imgFolder, 3, id, imgIndex))
                                    descriptionChain.add(Image(element.imageId))
                                    imgIndex++
                                }
                                if (element is PlainText) {
                                    descriptionChain.add(element)
                                }
                                if (element is At) {
                                    descriptionChain.add(element)
                                }
                                if (element is AtAll) {
                                    descriptionChain.add(element)
                                }
                                if (element is Face) {
                                    descriptionChain.add(element)
                                }
                            }
                            // 序列化为 json String
                            description = descriptionChain.build().serializeToJsonString()



                            group.sendMessage("请在30秒内发送该抽奖的截止时间\n时间格式为 年-月-日-时-分")
                            val message2: String = selectMessages {
                                has<PlainText> { message.contentToString() }
                                timeout(30_000) {
                                    waitForDeleteImgIdList.add(id)
                                    group.sendMessage("未收到消息，请重新创建！"); null
                                }
                            } ?: return@subscribeAlways
                            try {
                                endTime = sdf.parseObject(message2) as Date
                            } catch (e: Exception) {
                                group.sendMessage("时间格式错误，请重新创建！")
                                waitForDeleteImgIdList.add(id)
                                return@subscribeAlways
                            }
                            val currentDateTime = LocalDateTime.now()
                            val endDateTime = LocalDateTime.ofInstant(endTime.toInstant(), ZoneId.systemDefault())
                            if (currentDateTime.isAfter(endDateTime)) {
                                group.sendMessage("截止时间不能早于现在时间，请重新创建！")
                                waitForDeleteImgIdList.add(id)
                                return@subscribeAlways
                            }

                            group.sendMessage("请在30秒内发送该抽奖的抽选类型（仅回复数字）\n0：中奖人可重复 1：中奖人不可重复\n（中奖人不可重复时，若参与人数小于奖品数，将在覆盖每一个人的前提下产生重复中奖）")
                            val message3: String = selectMessages {
                                "0" { message.contentToString() }
                                "1" { message.contentToString() }
                                default { message.contentToString() }
                                timeout(30_000) {
                                    waitForDeleteImgIdList.add(id)
                                    group.sendMessage("未收到消息，请重新创建！"); null
                                }
                            } ?: return@subscribeAlways
                            try {
                                type = Integer.parseInt(message3)
                            } catch (e: Exception) {
                                group.sendMessage("格式错误，请重新创建！")
                                waitForDeleteImgIdList.add(id)
                                return@subscribeAlways
                            }

//                            group.sendMessage("请在30秒内发送该抽奖的奖品数量（仅回复数字）")
//                            val message4: String = selectMessages {
//                                has<PlainText> { message.contentToString() }
//                                timeout(30_000) { group.sendMessage("未收到消息，请重新创建！"); null }
//                            } ?: return@subscribeAlways
//                            try {
//                                winnerNum = Integer.parseInt(message4)
//                            } catch (e: Exception) {
//                                group.sendMessage("格式错误，请重新创建！")
//                                return@subscribeAlways
//                            }

                            group.sendMessage("请在120秒内发送抽奖奖品列表（一行一项，仅支持文字）\n注：可在每一行末尾添加 *n 来指定个数，如”色纸*3“会抽取三份色纸")
                            val message4: String = selectMessages {
                                has<PlainText> { message.contentToString() }
                                timeout(120_000) {
                                    waitForDeleteImgIdList.add(id)
                                    group.sendMessage("未收到消息，请重新创建！"); null
                                }
                            } ?: return@subscribeAlways
                            try {
                                message4.split("\n").forEach {
                                    var prizeNum: Int
                                    val prizeContent: String
                                    if (it.matches(Regex("^.+\\*[0-9]+$"))) {
                                        prizeNum = it.substring(it.lastIndexOf("*") + 1).toInt()
                                        prizeContent = it.substring(0, it.lastIndexOf("*"))
                                    } else {
                                        prizeNum = 1
                                        prizeContent = it
                                    }
                                    if (prize.contains(prizeContent)) {
                                        prizeNum += prize.get(prizeContent)!!
                                        prize.put(prizeContent, prizeNum)
                                    } else {
                                        prize.put(prizeContent, prizeNum)
                                    }
                                }
                                winnerNum = prize.size
                            } catch (e: Exception) {
                                group.sendMessage("格式错误，请重新创建！")
                                waitForDeleteImgIdList.add(id)
                                return@subscribeAlways
                            }

                            // 存储信息
                            val temp = Lot(
                                id,
                                group.id,
                                sender.id,
                                description,
                                endTime,
                                type,
                                winnerNum,
                                mutableListOf(),
                                mutableListOf(),
                                imgNames,
                                prize,
                                false
                            )

                            withContext(Dispatchers.IO) {
                                FileWriter(lotteryFolder.path + "/" + temp.lotId + ".json")
                            }.run {
                                GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm").create().toJson(temp, this)
                                close()
                            }

                            group.sendMessage("创建成功，抽奖id为$id")
                        }
                    }

                    // /lot remove <抽奖编号>
                    if (message.contentToString().startsWith(Settings.remove_lottery)) {
                        try {
                            val f = File(
                                "${lotteryFolder.path}/" + message.contentToString()
                                    .removePrefix(Settings.remove_lottery + " ") + ".json"
                            )
                            if (f.isFile) {
                                val temp = fileToData(f)

                                if (group.id != temp.group) {
                                    group.sendMessage("参数错误！")
                                    return@subscribeAlways
                                }

                                if (sender.id == Config.adminQQ || sender.id == temp.creator || sender.permission.isOperator()) {
                                    f.delete()
                                    deleteImg(temp.lotId, imgFolder)
                                    group.sendMessage("删除成功")
                                } else {
                                    group.sendMessage("没有删除权限")
                                }
                            } else {
                                group.sendMessage("编号不存在！")
                            }
                        } catch (e: Exception) {
                            group.sendMessage("参数错误！")
                        }
                    }

                    // /lot list
                    if (message.contentToString().startsWith(Settings.list_lottery)) {
                        val sdf: Format = SimpleDateFormat("yyyy-MM-dd hh:mm")
                        val builder = MessageChainBuilder()
                        var hasLot = false
                        if (lotteryFolder.listFiles().isNotEmpty()) {
                            for (file in lotteryFolder.listFiles()!!) {
                                val temp = fileToData(file)
                                if (temp.group == group.id) {
                                    hasLot = true
                                    val currentDateTime = LocalDateTime.now()
                                    val endDateTime =
                                        LocalDateTime.ofInstant(temp.endTime.toInstant(), ZoneId.systemDefault())
                                    if (currentDateTime.isBefore(endDateTime)) {
                                        builder.add(
                                            "抽奖编号：${temp.lotId}\n" +
                                                    "创建人：${group.get(temp.creator)?.nameCardOrNick}(${temp.creator})\n" +
                                                    "截止日期：${sdf.format(temp.endTime)}\n" +
                                                    "---\n"
                                        )
                                    } else {
                                        builder.add(
                                            "[已结束]\n" +
                                                    "抽奖编号：${temp.lotId}\n" +
                                                    "创建人：${group.get(temp.creator)?.nameCardOrNick}(${temp.creator})\n" +
                                                    "截止日期：${sdf.format(temp.endTime)}\n" +
                                                    "---\n"
                                        )
                                    }
                                }
                            }
                        }
                        if (!hasLot)
                            builder.add("抽奖列表为空")
                        val msg = builder.build()
                        group.sendMessage(msg)
                    }

                    // /lot members <抽奖编号>
                    if (message.contentToString().startsWith(Settings.list_lottery_members)) {
                        try {
                            val f = File(
                                "${lotteryFolder.path}/" + message.contentToString()
                                    .removePrefix(Settings.list_lottery_members + " ") + ".json"
                            )
                            val id = message.contentToString().removePrefix(Settings.list_lottery_members + " ")
                            val temp = fileToData(f)
                            val builder = MessageChainBuilder()

                            if (group.id != temp.group) {
                                group.sendMessage("参数错误！")
                                return@subscribeAlways
                            }

                            if (temp.members.size > 0) {
                                builder.add("抽奖 $id 的参与名单如下：\n")
                                for (memberID in temp.members) {
                                    builder.add("${group.get(memberID.toLong())?.nameCardOrNick}($memberID)\n")
                                }
                            } else {
                                builder.add("抽奖 $id 暂时无人参与\n")
                            }
                            val msg = builder.build()
                            group.sendMessage(msg)
                        } catch (e: Exception) {
                            group.sendMessage("参数错误！")
                        }
                    }

                    // /lot detail <抽奖编号>
                    if (message.contentToString().startsWith(Settings.lottery_detail)) {
                        try {
                            val sdf: Format = SimpleDateFormat("yyyy-MM-dd hh:mm")
                            val f = File(
                                "${lotteryFolder.path}/" + message.contentToString()
                                    .removePrefix(Settings.lottery_detail + " ") + ".json"
                            )
                            val id = message.contentToString().removePrefix(Settings.lottery_detail + " ")
                            val temp = fileToData(f)
                            val builder = MessageChainBuilder()
                            val currentDateTime = LocalDateTime.now()
                            val endDateTime =
                                LocalDateTime.ofInstant(temp.endTime.toInstant(), ZoneId.systemDefault())

                            if (group.id != temp.group) {
                                group.sendMessage("参数错误！")
                                return@subscribeAlways
                            }

                            if (currentDateTime.isAfter(endDateTime)) {
                                builder.add("[已结束]\n")
                                builder.add("抽奖编号：$id\n")
                                builder.add("截止日期：\n")
                                builder.add(sdf.format(temp.endTime) + "\n")
                                builder.add("描述：\n")
                                // 图文描述
                                val d = MessageChain.deserializeFromJsonString(temp.description)
                                var imgIndex = 0
                                for (element in d) {
                                    if (element is Image) {
                                        try {
                                            val image: Image

                                            withContext(Dispatchers.IO) {
                                                File("${imgFolder.path}/${temp.imgNames.get(imgIndex)}").toExternalResource()
                                            }.run {
                                                image = group.uploadImage(this)
                                                close()
                                            }
                                            builder.add(image)
                                            imgIndex++
                                        } catch (e: Exception) {
                                            builder.add("[图片加载失败]")
                                            imgIndex++
                                        }
                                    } else {
                                        builder.add(element)
                                    }
                                }

                                if (temp.winners.size > 0) {
                                    builder.add("\n中奖名单：\n")
                                    val winnerIndex = 0
                                    for (map in temp.prize) {
                                        builder.add(map.key + "：\n")
                                        for (i in 0 until map.value) {
                                            builder.add(
                                                "${group.get(temp.winners.get(winnerIndex))?.nameCardOrNick}(${
                                                    temp.winners.get(
                                                        winnerIndex
                                                    )
                                                })\n"
                                            )
                                        }
                                    }
                                } else {
                                    builder.add("无人中奖\n")
                                }
                            } else {
                                builder.add("抽奖编号：$id\n")
                                builder.add("截止日期：\n")
                                builder.add(sdf.format(temp.endTime) + "\n")
                                builder.add("描述：\n")
                                // 图文描述
                                val d = MessageChain.deserializeFromJsonString(temp.description)
                                var imgIndex = 0
                                for (element in d) {
                                    if (element is Image) {
                                        try {
                                            val image: Image
                                            withContext(Dispatchers.IO) {
                                                File("${imgFolder.path}/${temp.imgNames.get(imgIndex)}").toExternalResource()
                                            }.run {
                                                image = group.uploadImage(this)
                                                close()
                                            }
                                            builder.add(image)
                                            imgIndex++
                                        } catch (e: Exception) {
                                            builder.add("[图片加载失败]")
                                            imgIndex++
                                        }
                                    } else {
                                        builder.add(element)
                                    }
                                }
                            }
                            val msg = builder.build()
                            group.sendMessage(msg)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            group.sendMessage("参数错误！")
                        }
                    }

                    // /lot join <抽奖编号>
                    if (message.contentToString().startsWith(Settings.join_lottery)) {
                        try {
                            val f = File(
                                "${lotteryFolder.path}/" + message.contentToString()
                                    .removePrefix(Settings.join_lottery + " ") + ".json"
                            )
                            val temp = fileToData(f)
                            val currentDateTime = LocalDateTime.now()
                            val endDateTime = LocalDateTime.ofInstant(temp.endTime.toInstant(), ZoneId.systemDefault())
                            val builder = MessageChainBuilder()

                            if (group.id != temp.group) {
                                group.sendMessage("参数错误！")
                                return@subscribeAlways
                            }

                            if (currentDateTime.isAfter(endDateTime)) {
                                builder.add("抽奖已结束，无法加入")
                            } else if (temp.members.contains(sender.id.toString())) {
                                builder.add("已加入此抽奖")
                            } else {
                                temp.members.add(sender.id.toString())

                                withContext(Dispatchers.IO) {
                                    FileWriter(lotteryFolder.path + "/" + temp.lotId + ".json")
                                }.run {
                                    GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm").create().toJson(temp, this)
                                    close()
                                }

                                builder.add("加入成功！")
                            }
                            val msg = builder.build()
                            group.sendMessage(message.quote() + msg)
                        } catch (e: Exception) {
                            group.sendMessage("参数错误！")
                        }
                    }

                    // /lot quit <抽奖编号>
                    if (message.contentToString().startsWith(Settings.quit_lottery)) {
                        try {
                            val f = File(
                                "${lotteryFolder.path}/" + message.contentToString()
                                    .removePrefix(Settings.quit_lottery + " ") + ".json"
                            )
                            val temp = fileToData(f)
                            val currentDateTime = LocalDateTime.now()
                            val endDateTime = LocalDateTime.ofInstant(temp.endTime.toInstant(), ZoneId.systemDefault())
                            val builder = MessageChainBuilder()

                            if (group.id != temp.group) {
                                group.sendMessage("参数错误！")
                                return@subscribeAlways
                            }

                            if (currentDateTime.isAfter(endDateTime)) {
                                builder.add("抽奖已结束")
                            } else if (temp.members.contains(sender.id.toString())) {
                                temp.members.remove(sender.id.toString())

                                withContext(Dispatchers.IO) {
                                    FileWriter(lotteryFolder.path + "/" + temp.lotId + ".json")
                                }.run {
                                    GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm").create().toJson(temp, this)
                                    close()
                                }


                                builder.add("退出成功！")
                            } else {
                                builder.add("未参与此抽奖")
                            }
                            val msg = builder.build()
                            group.sendMessage(message.quote() + msg)
                        } catch (e: Exception) {
                            group.sendMessage("参数错误！")
                        }
                    }

                    // /lot end <抽奖编号>
                    if (message.contentToString().startsWith(Settings.end_lottery)) {
                        val f = File(
                            "${lotteryFolder.path}/" + message.contentToString()
                                .removePrefix(Settings.end_lottery + " ") + ".json"
                        )
                        if (f.exists()) {
                            val temp = fileToData(f)
                            val currentDateTime = LocalDateTime.now()

                            if (group.id != temp.group) {
                                group.sendMessage("参数错误！")
                                return@subscribeAlways
                            }

                            if (sender.id == Config.adminQQ || sender.id == temp.creator || sender.permission.isOperator()) {
                                // 将抽奖结束时间设为当前时间且禁用结束前提示
                                val newTemp = Lot(
                                    temp.lotId,
                                    temp.group,
                                    temp.creator,
                                    temp.description,
                                    Date.from(currentDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                                    temp.type,
                                    temp.winnerNum,
                                    temp.members,
                                    temp.winners,
                                    temp.imgNames,
                                    temp.prize,
                                    true
                                )


                                withContext(Dispatchers.IO) {
                                    FileWriter(lotteryFolder.path + "/" + temp.lotId + ".json")
                                }.run {
                                    GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm").create().toJson(newTemp, this)
                                    close()
                                }

                                val tempTask = LotTask(lotteryFolder, imgFolder, waitForDeleteImgIdList)
                                tempTask.run()

                            } else {
                                group.sendMessage("仅有该抽奖的创建人可以手动结束此抽奖")
                            }
                        } else {
                            group.sendMessage("编号不存在！")
                        }
                    }
                }
            }
            // 循环任务
            launch {
                val task = LotTask(lotteryFolder, imgFolder, waitForDeleteImgIdList)
                Timer().schedule(task, Date(), 10_000)
            }
        }
    }
}