package com.logs

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import kotlin.random.Random

fun downloadImg(url: URL, imgFolder: File, reTryCnt: Int, lotID: Int, imgIndex: Int): String {
    return if (reTryCnt > 0) try {

        val extension = getExtension(url)
        val fileName = "${lotID}_$imgIndex$extension"
        val path = "${imgFolder.path}/$fileName"

        FileOutputStream(path).channel.transferFrom(Channels.newChannel(url.openStream()), 0, Long.MAX_VALUE)
        fileName
    } catch (e: Exception) {
        downloadImg(url, imgFolder, reTryCnt - 1, lotID, imgIndex)
    } else "err"
}

fun getExtension(url: URL): String {
//    val b = byteArray(10)
//    fin.read(b)
//    return if (b[0] == 'G'.code.toByte() && b[1] == 'I'.code.toByte() && b[2] == 'F'.code.toByte())
//        ".gif"
//    else if (b[6] == 'J'.code.toByte() && b[7] == 'F'.code.toByte() && b[8] == 'I'.code.toByte() && b[9] == 'F'.code.toByte())
//        ".jpg"
//    else if (b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() && b[3] == 'G'.code.toByte())
//        ".png"
//    else
//        ".jpg"

    val fin = url.openStream()
    val b = IntArray(10)
    for (i in 0 until 10) {
        b[i] = fin.read()
    }
    fin.close()

    return if (b[0] == 'G'.code && b[1] == 'I'.code && b[2] == 'F'.code)
        ".gif"
    else if (b[6] == 'J'.code && b[7] == 'F'.code && b[8] == 'I'.code && b[9] == 'F'.code)
        ".jpg"
    else if (b[1] == 'P'.code && b[2] == 'N'.code && b[3] == 'G'.code)
        ".png"
    else
        ".jpg"
}


// 生成非重复抽奖id
fun idGenerator(lotteryFolder: File): Int {
    var num = 0
    var duplicate = true
    while (duplicate) {
        duplicate = false
        num = Random.nextInt(10000, 99999)
        if (lotteryFolder.listFiles().isNotEmpty()) {
            for (file in lotteryFolder.listFiles()!!) {
                if (num.toString() == file.path)
                    duplicate = true
            }
        } else continue
    }
    return num
}

// 抽奖
fun roll(lot: Lot): List<Long> {
    val winners = mutableListOf<Long>()
    // 无人参与
    if (lot.members.size <= 0) {
        return winners
    }
    // 可重复
    else if (lot.type == 0) {
        var cnt = 0
        while (cnt < lot.winnerNum) {
            //参与人数大于 1
            if (lot.members.size > 1) {
                val i = Random.nextInt(0, lot.members.size)
                winners.add(lot.members[i].toLong())
                cnt++
            }
            //参与人数为 1
            else {
                winners.add(lot.members[0].toLong())
                cnt++
            }
        }
    }
    // 不可重复
    else {
        var cnt = 0
        //参与人数大于奖品数
        if (lot.members.size > lot.winnerNum) {
            while (cnt < lot.winnerNum) {
                val i = Random.nextInt(0, lot.members.size)
                if (!winners.contains(lot.members[i].toLong())) {
                    winners.add(lot.members[i].toLong())
                    cnt++
                }
            }
        }
        // 参与人数大于 1 且小于奖品数（可能产生重复中奖）
        else if (lot.members.size > 1) {
            // 每个成员的中奖次数
            val repeatTimes = HashMap<Long, Long>()
            for (member in lot.members) {
                repeatTimes.put(member.toLong(), 0)
            }
            // 到达最大中奖次数的成员数
            val maxMemberNum = 0
            // 确定每个成员最少可重复中奖次数
            val minRepeat = (lot.winnerNum / lot.members.size).toLong()
            // 确定有多少成员可中奖 minRepeat+1 次
            val maxRepeatMember = (lot.winnerNum % lot.members.size).toLong()

            while (cnt < lot.winnerNum) {
                val i = Random.nextInt(0, lot.members.size)
                // 该成员中奖次数小于最小中奖数，直接中奖
                if (repeatTimes.get(lot.members[i].toLong())!! < minRepeat) {
                    winners.add(lot.members[i].toLong())
                    repeatTimes.put(lot.members[i].toLong(), repeatTimes.get(lot.members[i].toLong())!! + 1)
                    cnt++
                }
                // 该成员中奖次数等于于最小中奖数，需要到达最大中奖次数的成员数不足，才能加入
                else if (maxMemberNum < maxRepeatMember && repeatTimes.get(lot.members[i].toLong())!! == minRepeat) {
                    winners.add(lot.members[i].toLong())
                    repeatTimes.put(lot.members[i].toLong(), repeatTimes.get(lot.members[i].toLong())!! + 1)
                    cnt++
                }
            }
        }
        //参与人数为 1
        else {
            while (cnt < lot.winnerNum) {
                winners.add(lot.members[0].toLong())
                cnt++
            }
        }

    }
    return winners
}

// 文件转Lot
fun fileToData(f: File): Lot {
    val jsonStr: String = f.readText()
    return GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm").create().fromJson(jsonStr, Lot::class.java)
}

fun endLot(lot: Lot): Lot {
    return Lot(
        lot.lotId,
        lot.group,
        lot.creator,
        lot.description,
        lot.endTime,
        lot.type,
        lot.winnerNum,
        lot.members,
        lot.winners,
        lot.imgNames,
        lot.prize,
        lot.hasRemind,
        true
    )
}

fun remindLot(lot: Lot): Lot {
    return Lot(
        lot.lotId,
        lot.group,
        lot.creator,
        lot.description,
        lot.endTime,
        lot.type,
        lot.winnerNum,
        lot.members,
        lot.winners,
        lot.imgNames,
        lot.prize,
        true,
        lot.isEnd
    )
}

// 删除图片
fun deleteImg(lotID: Int, imgFolder: File) {
    for (img in imgFolder.listFiles()) {
        if (img.name.startsWith(lotID.toString())) {
            img.delete()
        }
    }
}