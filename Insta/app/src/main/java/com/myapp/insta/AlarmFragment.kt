package com.myapp.insta


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query.Direction
import com.myapp.insta.model.AlarmDTO
import kotlinx.android.synthetic.main.item_comment.view.*


class AlarmFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        var view = inflater.inflate(R.layout.fragment_alarm, container, false)
        var recyclerview = view.findViewById<RecyclerView>(R.id.alarmfragment_recyclerview)
        recyclerview.adapter = AlarmRecyclerViewAdapter()
        recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class AlarmRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        val alarmDTOlist = ArrayList<AlarmDTO>()
        init{
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            FirebaseFirestore.getInstance().collection("alarms")
                    .whereEqualTo("destinationUid", uid)
                    .orderBy("timestamp", Direction.DESCENDING)
                    .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        alarmDTOlist.clear() //데이터가 쌓이지 않도록 함
                        if(querySnapshot == null) return@addSnapshotListener
                        for(snapshot in querySnapshot.documents!!){
                            alarmDTOlist.add(snapshot.toObject(AlarmDTO::class.java)!!)
                        }
                        notifyDataSetChanged()
                    }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_comment, parent, false)

            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view!!)

        override fun getItemCount(): Int {
            return alarmDTOlist.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val commentTextview = holder.itemView.commentViewItem_textview_profile

            when(alarmDTOlist[position].kind){

                0 -> {
                    val str_0 = alarmDTOlist[position].userId + getString(R.string.alarm_favorite)
                    commentTextview.text = str_0
                }
                1 -> {
                    val str_1 = alarmDTOlist[position].userId +
                            getString(R.string.alarm_who) + "\"" +
                            alarmDTOlist[position].message + "\"" +
                            getString(R.string.alarm_comment)
                    commentTextview.text = str_1
                }
                2 -> {
                    val str_2 = alarmDTOlist[position].userId + getString(R.string.alarm_follow)
                    commentTextview.text = str_2
                }
            }
        }

    }



}
