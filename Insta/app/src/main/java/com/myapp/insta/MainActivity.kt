package com.myapp.insta

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.MenuItem
import android.view.View
import com.facebook.login.Login
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    var PICK_PROFILE_FROM_ALBUM = 10

    //snapshot은 DB를 실시간으로 보다가 바뀐 부분이 있으면 이벤트를 발생시키는 것임
    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        setToolbarDefault()
        when(item.itemId){
            //인터페이스임
            R.id.action_home -> {
                var detailviewFragment = DetailviewFragment()
                // 화면을 바꾸기 위한 트랙잭션을 처리해주어야 함
                supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, detailviewFragment)
                        .commit()
                return true;
            }

            R.id.action_search -> {
                var gridFragment = GridFragment()

                supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, gridFragment)
                        .commit()
                return true;
            }

            R.id.action_addPhoto -> {

                if(ContextCompat.checkSelfPermission(this,
                                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }

                return true;
            }

            R.id.action_likeAlarm -> {
                var alertFragment = AlertFragment()

                supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, alertFragment)
                        .commit()
                return true;
            }

            R.id.action_account -> {
                var userFragment = UserFragment()
                // fragment 사용시 반드시 bundle 필요
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var bundle = Bundle()
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle //fragment로 bundle을 넘겨줌
                // activity 일 경우
                /*var intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("destinationUid", "dudgh1002@naver.com")
                startActivity(intent)*/

                supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, userFragment)
                        .commit()

                return true;
            }
        }
        // return true 일 때는 버튼이 작동, false일 때는 버튼이 작동 안함
        return false
     }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //bt_navigation을 누르면 this를 통해서 소통하는 결과값이 onNavigationItemSelected로 넘어가게 됨
        bottom_navigation.setOnNavigationItemSelectedListener(this)

        bottom_navigation.selectedItemId = R.id.action_home // 로딩됐을때 홈에서 시작하도록 함

        //사진을 갖고 올수 있는 권한을 받아옴
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            var imageUri = data?.data
            // 이미지를 쌓고 싶으면 날짜를 이용
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            var storageRef = FirebaseStorage.getInstance().reference
                    .child("userProfileImages").child(uid!!)


            storageRef.putFile(imageUri!!).addOnSuccessListener {
                storageRef.downloadUrl.addOnCompleteListener { task ->
                    var url = task.result.toString()
                    var map = HashMap<String, Any>()

                    map["image"] = url // [키값] = 주소값
                    FirebaseFirestore.getInstance().collection("profileImages")
                            .document(uid).set(map)
                }
            }


        }
    }

    fun setToolbarDefault(){
        toolbar_btn_back.visibility = View.GONE // 없애주겠다는 의미, 공간 자체가 사라짐
        toolbar_username.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }



}
