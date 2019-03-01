package com.myapp.insta


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment //v4가 아닌 그냥 app은 더 이상 지원을 안함
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.insta.model.AlarmDTO
import com.myapp.insta.model.ContentDTO
import com.myapp.insta.model.FollowDTO
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {

    var fragmentView : View? = null
    var PICK_PROFILE_FROM_ALBUM = 10
    var firestore : FirebaseFirestore? = null
    var currentUid : String? = null // 현재 나의 uid
    var selectUid : String? = null // 내가 선택한 uid
    var auth : FirebaseAuth? = null
    var fcmPush : FcmPush? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // 자신의 uid
        currentUid = FirebaseAuth.getInstance().currentUser?.uid
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fcmPush = FcmPush()
        fragmentView = LayoutInflater.from(inflater.context)
                .inflate(R.layout.fragment_user, container, false)

        if(arguments != null){
            selectUid = arguments!!.getString("destinationUid")

            if(selectUid != null && selectUid == currentUid){
                // 나의 유저 페이지
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    activity?.finish() // 상단의 activity 종료
                    startActivity(Intent(activity, LoginActivity::class.java))
                    auth?.signOut() // 저장되있던 유저 정보가 날아감
                }
            }
            else{
                // 제 3자의 유저 페이지
                fragmentView!!.account_btn_follow_signout.text = getString(R.string.follow)


                var mainActivity = (activity as MainActivity)
                mainActivity.toolbar_title_image.visibility = View.GONE
                mainActivity.toolbar_btn_back.visibility = View.VISIBLE
                mainActivity.toolbar_username.visibility = View.VISIBLE
                mainActivity.toolbar_username.text = arguments!!.getString("userId")
                mainActivity.toolbar_btn_back.setOnClickListener {
                    mainActivity.bottom_navigation.selectedItemId = R.id.action_home
                }

                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    requestFollow()
                }
            }
        }

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            // fragment는 context를 갖고 있지 않기 때문에 activity를 넣어 주어야 함
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)
        //spancount는 가로 이미지의 갯수
        getProfileImages()
        getFollower()
        getFollowing()

        // 상위뷰로 이동하는 방법 getActivity, 코틀린에서는 activity

        return fragmentView
    }

    // fragment에는 activityResult를 사용할 수 없다.
    // fragment는 activity에 소속된 하위 view임
    // 따라서 결과는 fragment가 소속된 activity에 결과가 넘어감
    // 여기서는 mainActivity로 넘어감


    // 팔로우 요청
    fun requestFollow(){
        var tsDocFollowing = firestore!!.collection("users").document(currentUid!!)

        firestore?.runTransaction {transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)

            // 아무도 팔로우 하지 않았을 경우
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[selectUid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(selectUid)){
                // 선택한 유저를 이미 팔로우 하고 있을 경우 = 다른 유저가 나를 팔로우 취소
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(selectUid)
            }
            else{
                // 내가 선택한 유저를 팔로우 하지 않았을 경우 = 다른 유저가 나를 팔로잉함
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[selectUid!!] = true
            }

            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        // 상대방의 팔로워수 수정
        var tsDocFollower = firestore!!.collection("users").document(selectUid!!)
        firestore?.runTransaction {transaction ->
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)

            if(followDTO == null){
                //아무도 팔로잉하지 않았을 경우
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction //runTransaction을 종료시키겠다는 의미
            }

            if(followDTO!!.followers.containsKey(currentUid!!)){
                // 선택한 유저를 내가 팔로잉 하고 있을 경우 = 취소
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUid!!)

            }
            else{
                // 선택한 유저를 팔로잉 하지 않았을 경우 = 팔로잉 함
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUid!!] = true
                followerAlarm(selectUid!!)
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }

    }


    // 파이어베이스에서 프로필 사진 가져오기
    fun getProfileImages(){

        firestore?.collection("profileImages")?.document(selectUid!!)
                ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

                    if(documentSnapshot == null)
                        return@addSnapshotListener // 스냅샷 충돌 방지

                   if(documentSnapshot?.data != null){
                        var url = documentSnapshot?.data!!["image"]
                        Glide.with(activity!!)
                                .load(url)
                                .apply(RequestOptions().circleCrop()) // 이미지를 동그랗게 함
                                .into(fragmentView!!.account_iv_profile)
                    }
                }
    }

    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()

        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms")
                .document()
                .set(alarmDTO)

        var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        fcmPush?.sendMessage(destinationUid, "알림 메시지 입니다.", message)
    }

    fun getFollower(){
        firestore?.collection("users")?.document(selectUid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener

            val followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount.toString()

            if(followDTO?.followers?.containsKey(currentUid)!!){
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
            }
            else{
                if(selectUid != currentUid){
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                }
            }

        }
    }

    fun getFollowing(){
        firestore?.collection("users")?.document(selectUid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            val followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            fragmentView?.account_tv_following_count?.text = followDTO?.followingCount.toString()
        }
    }


    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO>

        init{
            contentDTOs = ArrayList()
            // 자신의 이미지만을 불러옴
            firestore?.collection("images")
                    ?.whereEqualTo("uid", selectUid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                        if(querySnapshot == null) return@addSnapshotListener

                        contentDTOs.clear()
                        for(snapshot in querySnapshot.documents){
                            contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                        }

                        fragmentView?.account_iv_post_count?.text = contentDTOs.size.toString()
                        notifyDataSetChanged()
                    }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            var width = resources.displayMetrics.widthPixels / 3
            var imageView = ImageView(parent.context)

            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                    .apply(RequestOptions.centerCropTransform()).into(imageView)
        }


    }

}
