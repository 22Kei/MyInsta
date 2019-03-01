package com.myapp.insta.model


data class PushDTO(
        var to : String? = null,
        var notification: Notification? = Notification())
{
    data class Notification(
        var body : String? = null,
        var title : String? = null
    )

    //okhttp는 외부 API와 통신할 때 사용
}
