package com.myapp.insta

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.myapp.insta.model.PushDTO
import okhttp3.*
import java.io.IOException

class FcmPush(){
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAA7DK5E90:APA91bHG03Wi_6bbIaJ_N86YsZjK14XtPfiR0iPwrdR976_rb7_EprswIc4XIU5O1l8qccuBkKT4C8MwSWMVPSehrW_yPXIMJRw63T4k8skbZ7IQKGBMTk15K8ie6gF8skzbCH3-L2Ji"
    // serverKey는 push를 날릴수 있는 권한을 가진 키

    var okHttpClient : OkHttpClient? = null
    var gson : Gson? = null

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid : String, title : String?, message : String?){
        FirebaseFirestore.getInstance().collection("pushTokens")
                .document(destinationUid)
                .get() // 한번만 불러옴, snapshot은 계속 지켜보는 것
                .addOnCompleteListener {task ->
                    if(task.isSuccessful){
                        var token = task.result!!["pushToken"].toString()

                        var pushDTO = PushDTO()
                        pushDTO.to = token
                        pushDTO.notification?.title = title
                        pushDTO.notification?.body = message

                        // 객체화된 것을 JSON 형태로 변환
                        var body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                        var request = Request.Builder()
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Authorization", "key="+serverKey)
                                .url(url)
                                .post(body)
                                .build()

                        // 세팅된 값을 okHttpClient에 넣어줌
                        okHttpClient?.newCall(request)?.enqueue(object : Callback{
                            override fun onFailure(call: Call, e: IOException) {
                                // 인터넷 연결에 실패했을 때
                            }

                            override fun onResponse(call: Call, response: Response) {
                                // 인터넷 연결에 성공했을 때
                                println(response?.body()?.string())
                            }

                        })
                    }
                }
    }
}