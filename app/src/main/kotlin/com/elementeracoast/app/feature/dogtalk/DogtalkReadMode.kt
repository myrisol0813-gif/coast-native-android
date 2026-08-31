package com.elementeracoast.app.feature.dogtalk

enum class DogtalkReadMode(
    val label: String,
    val futureSemantics: String
) {
    KeepPrivate(
        label = "不需要，放着就好",
        futureSemantics = "只放着，不给模型看。"
    ),
    WhenConfused(
        label = "Myri 困惑时可以看一点",
        futureSemantics = "暂时 dormant，只表达未来许可；当前不会自动判断困惑，也不会提交给模型。"
    ),
    ReadNow(
        label = "这次希望 Myri 直接读一下",
        futureSemantics = "未来接入后只带入下一次发送一次，随后降回不需要。"
    )
}
