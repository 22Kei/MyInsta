package com.myapp.insta


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.insta.model.ContentDTO
import kotlinx.android.synthetic.main.fragment_grid.view.*  // findviewId가 필요 없어짐

class GridFragment : Fragment() {

    var mainView : View? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mainView = inflater.inflate(R.layout.fragment_grid, container, false)
        mainView?.gridfragment_recyclerview?.adapter = GridFragmentRecyclerViewAdapter()
        mainView?.gridfragment_recyclerview?.layoutManager = GridLayoutManager(activity, 3)

        return mainView
    }
    //gridFragment에서는 이미지뷰 하나밖에 없기 때문에 바로 만들어줌
    inner class GridFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO>

        // 데이터베이스 접근
        init{

            contentDTOs = arrayListOf()

            FirebaseFirestore.getInstance().collection("images")
                    .orderBy("timeStamp").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        for(snapshot in querySnapshot!!.documents){
                            var contentDTO = snapshot.toObject(ContentDTO::class.java)
                            contentDTOs.add(contentDTO!!)
                        }
                        notifyDataSetChanged()
                    }
        }

        // 새로 고침하게 되면 아래에 있는 것들이 다 다시 돌음
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            // 이미지에 대한 폭을 가져와서 3으로 나눔
            var width = resources.displayMetrics.widthPixels / 3

            var imageView = ImageView(parent.context)
            imageView.layoutParams =LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        private inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun getItemCount(): Int { return contentDTOs.size }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView

            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                    .apply(RequestOptions().centerCrop()) // 이미지를 중앙에 맞춤
                    .into(imageView)
        }

    }


}
