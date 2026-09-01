package com.elementeracoast.app.feature.dogtalk

import com.elementeracoast.app.core.model.RoomType

enum class DogtalkScope {
    Main,
    Radio,
    Lighthouse;

    companion object {
        fun from(roomType: RoomType): DogtalkScope = when (roomType) {
            RoomType.Main -> Main
            RoomType.Radio -> Radio
            RoomType.Lighthouse -> Lighthouse
        }
    }
}
