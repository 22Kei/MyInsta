package com.myapp.insta

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.* // findviewId를 자동적으로 호출해줌
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import java.util.*


class LoginActivity : AppCompatActivity() {

    // 변수 생성
    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 1000
    var callbackManager : CallbackManager ?= null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()   // 변수 초기화
        bt_emailLogin.setOnClickListener {
            createAndLoginEmail()
        }
        bt_googleSign.setOnClickListener {
            googleLogin()
        }
        bt_facebookLogin.setOnClickListener {
            facebookLogin()
        }

        // 구글 로그인에 필요한 것
        var  gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1014463271901-pk2uvbkemrtpnkm3fdnth3q8qh5a51hk.apps.googleusercontent.com") // 구글로그인에 접근할 수 있도록 해주는 키값
                .requestEmail()
                .build() // 코드를 마친다는 의미
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //printHashKey(this)

        callbackManager = CallbackManager.Factory.create() // callbackManager 초기화
    }


    // 페이스북 로그인 구현할 때 해시키값 얻는 것
    // https://stackoverflow.com/questions/7506392/how-to-create-android-facebook-key-hash/25524657#25524657
    /*fun printHashKey(pContext: Context) {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i("22K", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("22K", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("22K", "printHashKey()", e)
        }

    }
*/

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

    // 구글 로그인 기능
    fun googleLogin(){

        var signInItent = googleSignInClient?.signInIntent
        startActivityForResult(signInItent, GOOGLE_LOGIN_CODE)
    }

    // 구글 로그인에 성공한 데이터를 파이어베이스로 넘겨줌
    fun firebaseAuthWithGoogle(account : GoogleSignInAccount){

        // 인증서를 만듦
        var credential = GoogleAuthProvider.getCredential(account.idToken, null)
        // 구글 아이디로 만들어지게 됨
        auth?.signInWithCredential(credential)?.addOnCompleteListener {task ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser)
            }
        }
    }

    fun facebookLogin(){
        LoginManager.getInstance()
                .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        // 로그인의 결과를 받아서 성공, 실패, 취소를 반환
        // 이것을 구글과 동일하게 Firebase로 넘겨줘야 함
        LoginManager.getInstance()
                .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
                    override fun onSuccess(result: LoginResult?) {
                        handleFacebookAccessToken(result?.accessToken)
                    }

                    override fun onCancel() {

                    }

                    override fun onError(error: FacebookException?) {

                    }

                })
    }

    // 페이스북 로그인 결과를 Firebase로 넘겨줌
    fun handleFacebookAccessToken(token: AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)?.addOnCompleteListener {
            //결과값을 받는 부분 == callback
            task ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser);
            }
        }
    }

    // 자동로그인 기능
    override fun onResume() {
        super.onResume()
        moveMainPage(auth?.currentUser)
    }


    // 구글, 페이스북 로그인의 결과값을 가져옴
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 로그인의 결과를 callbackManager로 넘겨줌
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_LOGIN_CODE){
            // 구글에서 성공한 데이터가 넘어옴
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess){
                // 정상적으로 구글 로그인이 성공 했을 경우
                var account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            }
        }
    }
}
