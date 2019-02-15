package com.myapp.insta

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.* // findviewId를 자동적으로 호출해줌

class LoginActivity : AppCompatActivity() {

    // 변수 생성
    var auth : FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()   // 변수 초기화
        bt_emailLogin.setOnClickListener {
            createAndLoginEmail()
        }
    }

    // 아이디를 생성하는 함수 // 코틀린은 함수형 언어로, 자바보다 더 간편함
    fun createAndLoginEmail(){
        auth?.createUserWithEmailAndPassword(et_email.text.toString(),
                et_password.text.toString())?.addOnCompleteListener {
            task -> // 실행 결과 값이 넘어오는 것
            if(task.isSuccessful){
                //생성 성공
                moveMainPage(auth?.currentUser) // 로그인 성공시 auth가 유저의 정보를 갖고 있음
                Toast.makeText(this, "아이디 생성 성공", Toast.LENGTH_LONG).show()
            }
            else if(task.exception?.message.isNullOrEmpty()){ // != null 과 동일
                //생성 실패
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
            else{
                // 로그인
                signEmail()
            }

        }
    }

    // 로그인 해주는 함수
    fun signEmail(){
        auth?.signInWithEmailAndPassword(et_email.text.toString(),
                et_password.text.toString())?.addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                //로그인 성공
                moveMainPage(auth?.currentUser) // 로그인 성공시 auth가 유저의 정보를 갖고 있음
                Toast.makeText(this, "로그인에 성공했습니다.", Toast.LENGTH_LONG).show()
            }
            else{
                // 로그인 실패
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // nullpointerException을 피하기 위해 '?'를 사용
    // 다음 페이지로 이동
    fun moveMainPage(user : FirebaseUser?){

        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish() // 로그인 창을 끝내고 넘어가게 됨
        }
    }
}
