package com.myapp.insta


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query.Direction
import com.myapp.insta.model.AlarmDTO
import com.myapp.insta.model.ContentDTO
import com.myapp.insta.model.FollowDTO
import kotlinx.android.synthetic.main.fragment_detailview.view.*
import kotlinx.android.synthetic.main.item_detail.view.*


class DetailviewFragment : Fragment() {

    var firestore : FirebaseFirestore? = null
    var user : FirebaseAuth? = null
    var fcmPush :FcmPush? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        firestore = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance()
        fcmPush = FcmPush()

        var view = LayoutInflater.from(inflater.context)
                .inflate(R.layout.fragment_detailview, container, false)
        // 리싸이클러 뷰를 가져옴
        view.detailviewfragment_recyclerview.adapter = DetailRecyclerviewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    // 홈 화면에서 리싸이클러뷰 생성하는 것
    inner class DetailRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        val contentDTOs : ArrayList<ContentDTO> // 데이터베이스 문서
        val contentUidList : ArrayList<String> // 문서 상세 내용


        // 파이어베이스에 접근
        init{

            contentDTOs = arrayListOf()
            contentUidList = arrayListOf()

            var uid = FirebaseAuth.getInstance().currentUser?.uid

            firestore?.collection("users")
                    ?.document(uid!!)?.get()?.addOnCompleteListener {
                        task ->
                        if(task.isSuccessful){
                            var userDTO = task.result?.toObject(FollowDTO::class.java)

                            if(userDTO != null){
                                getContents(userDTO?.followings!!)
                            }
                        }

                    }
        }

        // 내가 팔로잉하는 유저의 컨텐츠를 가져오기
        fun getContents(followers : MutableMap<String, Boolean>){
            firestore?.collection("images")
                    ?.orderBy("timeStamp", Direction.DESCENDING)
                    ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                        if(querySnapshot == null) return@addSnapshotListener
                        // 불러올때마다 clear를 해줘서 중복된 데이터가 쌓이지 않도록 함
                        contentDTOs.clear()
                        contentUidList.clear()

                        // snapshot이 하나만 오는 것이 아니기 때문에 for문을 돌려줌
                        for(snapshot in querySnapshot!!.documents){ // 정리되지 않은 데이터
                            var item = snapshot.toObject(ContentDTO::class.java) // 이 포맷에 맞게 데이터가 들어감

                            if(followers.keys.contains(item?.uid)){
                                contentDTOs.add(item!!)
                            }
                            contentUidList.add(snapshot.id)
                        }
                        notifyDataSetChanged() // 새로고침
                    }
        }




        // 레이아웃을 불러오는 메소드
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
           var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)

            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view!!){}

        override fun getItemCount(): Int { return contentDTOs.size }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            // holder는 각각의 아이템들을 의미함
            var viewHolder = (holder as CustomViewHolder).itemView

            viewHolder.detailviewitem_profile_textview.
                    text = contentDTOs!![position].userId // 유저 아이디
            viewHolder.detailviewitem_explain_textview
                    .text = contentDTOs!![position].explain // 설명 텍스트
            viewHolder.detailviewitem_favoritecounter_textview
                    .text ="좋아요 " + contentDTOs!![position].likeCount + "개" //좋아요 카운터 설정

            // 이미지, 쓰레드(콜백) 방식임, 작업이 끝나고 그 결과를 into에 주겠다는 것
            Glide.with(holder.itemView.context)
                    .load(contentDTOs!![position].imageUrl)
                    .into(viewHolder.detailviewitem_imageview_content)

            // 좋아요는 트랜잭션을 이용하여 구현
            // 트랜잭션은 동시에 2명 이상이 접근하지 못하도록 하는 것
            var uid = FirebaseAuth.getInstance().currentUser!!.uid
            viewHolder.detailviewitem_favorite_imageview.setOnClickListener {
                likeEvent(position)
            }


            // 좋아요를 클릭했을 때
            if(contentDTOs!![position].likes.containsKey(uid)){
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }
            // 좋아요를 클릭하지 않았을 경우
            else{
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            // 프로필 이미지를 누를때마다 사용자 정보창이 변함
            viewHolder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, fragment).commit()
            }

            viewHolder.detailviewitem_comment_imageview.setOnClickListener {v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }

       }

        // 좋아요 이벤트에 관한 함수
        private  fun likeEvent(position: Int){
            // 게시물에 대한 경로를 가져옴
            var tsDoc = firestore?.collection("images")
                    ?.document(contentUidList[position])

            firestore?.runTransaction {
                transaction ->
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                // tsDoc에 저장된 경로에 접근해서 그 게시물에 대한 정보를 가져옴
                // 다른 사용자가 접근할 수 없음 -> 트랜잭션이기 때문
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                // 사용자에 대한 고유 해시값이 있는지 확인하는 것, 있으면 좋아요를 누른 상태
                if(contentDTO!!.likes.containsKey(uid)){ //좋아요를 누른 상태 -> 누르지 않은 상태
                    contentDTO?.likeCount = contentDTO.likeCount - 1
                    contentDTO?.likes.remove(uid)


                }
                else{ //좋아요를 누르지 않은 상태 -> 누른 상태
                    contentDTO?.likes[uid!!] = true
                    contentDTO?.likeCount = contentDTO?.likeCount + 1
                    likeAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc, contentDTO) // 설정값을 변경해주겠다는 의미
            }
            // 파이어베이스의 최고 장점: 바로바로 동기화가 됨

        }

        fun likeAlarm(destinationUid:String){
            var alarmDTO = AlarmDTO()

            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = user?.currentUser?.email
            alarmDTO.uid = user?.currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("alarms")
                    .document()
                    .set(alarmDTO)

            var message = user?.currentUser?.email + getString(R.string.alarm_favorite)
            fcmPush?.sendMessage(destinationUid, "알림 메시지 입니다.", message)
        }

        // 데이터베이스에 데이터가 많아질수록 쿼리를 잘 써야함
        // 쿼리의 예시
        // whereEqualTo("user", "howl")
    }
}
