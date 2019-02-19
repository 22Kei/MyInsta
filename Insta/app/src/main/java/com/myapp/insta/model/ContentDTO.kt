package com.myapp.insta.model

// data model은 데이터를 주고받기 쉽도록 틀 같은것을 만드는 것
data class ContentDTO(var explain: String? = null,
                      var imageUrl : String? = null,
                      var uid : String? = null, // uid는 유저에 대한 고유번호
                      var userId : String? = null,
                      var timeStamp : Long? = null,
                      var likeCount : Int = 0,
                      var likes : Map<String, Boolean> = HashMap()){
    data class  Comment(var uid : String? = null,
                        var userId : String? = null,
                        var comment : String? = null,
                        var timeStamp: Long? = null)
}
