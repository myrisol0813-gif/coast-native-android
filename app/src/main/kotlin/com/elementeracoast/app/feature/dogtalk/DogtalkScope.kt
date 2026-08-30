package com.elementeracoast.app.feature.dogtalk

import com.elementeracoast.app.core.model.ChatScope

enum class DogtalkScope {
    Main,
    Radio,
    Lighthouse;

    companion object {
        fun from(chatScope: ChatScope): DogtalkScope = when (chatScope) {
            ChatScope.Main -> Main
            ChatScope.Radio -> Radio
            ChatScope.Lighthouse -> Lighthouse
        }
    }
}
