package com.logs

object Settings {
    const val HELP = "/lot help"
    const val add_lottery = "/lot add"
    const val remove_lottery = "/lot remove"
    const val end_lottery = "/lot end"
    const val list_lottery = "/lot list"
    const val list_lottery_members = "/lot member"
    const val lottery_detail = "/lot detail"
    const val enable_lottery = "/lot enable"
    const val disable_lottery = "/lot disable"
    const val join_lottery = "/lot join"
    const val quit_lottery = "/lot quit"

    const val remain_hour: Long = 72
    const val remind_hour: Long = 1

    val HELP_INFO =
        """
            显示本帮助信息
            /lot help
            ---
            加入指定的抽奖
            /lot join <抽奖编号>
            退出指定的抽奖
            /lot quit <抽奖编号>
            ---
            列出正在进行或刚刚结束的抽奖
            /lot list
            查询指定抽奖的详情（描述、中奖名单等）
            /lot detail <抽奖编号>
            查询参与指定抽奖的群员
            /lot member <抽奖编号>
            ---
            添加一个抽奖
            /lot add
            删除一个抽奖（需为抽奖创建者或管理员）
            /lot remove <抽奖编号>
            手动结束一个抽奖（需为抽奖创建者或管理员）
            /lot end <抽奖编号>
        """.trimIndent()

    val HELP_FOR_ADMIN =
        """
            ---
            在群内启用抽奖功能（仅管理员可用）
            /lot enable
            在群内禁用抽奖功能（仅管理员可用）
            /lot disable
            ---
        """.trimIndent()
}