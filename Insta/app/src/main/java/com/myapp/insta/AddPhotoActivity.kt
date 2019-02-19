package com.myapp.insta

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.myapp.insta.model.ContentDTO
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {

    val PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri?= null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        // 이미지를 누르면 다시 앨범이 나오게 하는 것
        addphoto_image.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
        }

        addphoto_bt_image.setOnClickListener {
            contentUpload()
        }
    }

    // 모든 결과들이 모임, 따라서 requestCode로 구분함
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 앨범을 실행한 결과
        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                //앨범에서 사진을 선택했을 때
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)
            }
            else if(resultCode == Activity.RESULT_CANCELED){
                //취소시 화면을 종료하는 것
                finish()
            }
        }
    }

    // 이미지 업로드
    fun contentUpload(){

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "PNG_" + timeStamp + "_.png"
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)


        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnCompleteListener {task ->
                Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
                // 업로드된 이미지
                var uri = task.result.toString()

                // uri는 파일 경로, url은 http 주소
                // uri가 url을 포괄하는 개념

                var contentDTO = ContentDTO()
                contentDTO.imageUrl = uri!!.toString() // 이미지 주소
                contentDTO.uid = auth?.currentUser?.uid // 유저의 uid
                contentDTO.explain = addphoto_edit_explain.text.toString() // 게시물 설명
                contentDTO.userId = auth?.currentUser?.email // 유저 아이디
                contentDTO.timeStamp = System.currentTimeMillis() // 게시물 업로드 시간

                // 서버에 데이터를 넣음
                firestore?.collection("images")?.document()?.set(contentDTO)
                // 콜렉션은 경로같은 것(테이블)

                setResult(Activity.RESULT_OK)
                finish()
            }
        }

    }

}
