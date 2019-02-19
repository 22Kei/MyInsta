package com.myapp.insta


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
import com.myapp.insta.model.ContentDTO
import kotlinx.android.synthetic.main.fragment_detailview.view.*
import kotlinx.android.synthetic.main.item_detail.view.*


class DetailviewFragment : Fragment() {

    var firestore : FirebaseFirestore? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        firestore = FirebaseFirestore.getInstance()

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

            firestore?.collection("images")
                    ?.orderBy("timeStamp")
                    ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                        // 불러올때마다 clear를 해줘서 중복된 데이터가 쌓이지 않도록 함
                        contentDTOs.clear()
                        contentUidList.clear()

                        // snapshot이 하나만 오는 것이 아니기 때문에 for문을 돌려줌
                        for(snapshot in querySnapshot!!.documents){ // 정리되지 않은 데이터
                            var item = snapshot.toObject(ContentDTO::class.java) // 이 포맷에 맞게 데이터가 들어감
                            contentDTOs.add(item!!)
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

        inner class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view!!){}

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
        }
    }


}
