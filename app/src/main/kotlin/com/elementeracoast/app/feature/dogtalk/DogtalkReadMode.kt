package com.elementeracoast.app.feature.dogtalk

enum class DogtalkReadMode(
    val label: String,
    val futureSemantics: String
) {
    KeepPrivate(
        "不需要，放着就好",
        "只放着，不给模型看。"
    ),
    WhenConfused(
        "Myri 困惑时可以看一点",
        "暂时 dormant，只表达未来许可，不自动判断困惑。"
    ),
    CurrentRoom(
        "当前窗口可以看一点",
        "未来可随当前窗口请求低权重带入。"
    ),
    ReadNow(
        "这次希望 Myri 直接读一下",
        "未来下一次发送时直接带入一次。"
    )
}
