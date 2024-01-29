package com.a503.onjeong.domain.mypage.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.a503.onjeong.domain.mypage.api.PhonebookApiService
import com.a503.onjeong.domain.mypage.dto.PhonebookAllDTO
import com.example.myapplication.R
import com.example.myapplication.auth.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyPageActivity : AppCompatActivity() {
    // NetRetrofit을 생성
    val retrofit = RetrofitClient.getApiClient(this)
    // NetRetrofit의 service를 통해 호출
    val service = retrofit.create(PhonebookApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        val groupSettingBtn: Button = findViewById(R.id.groupSettingBtn)
        groupSettingBtn.setOnClickListener {
            val intent = Intent(this@MyPageActivity, GroupListActivity::class.java)
            startActivity(intent)
        }

        val phoneBookBtn: Button = findViewById(R.id.phoneBookBtn)
        phoneBookBtn.setOnClickListener {
            //연락처 동기화
            //퍼미션 허용했는지 확인 후 데이터 전송
            checkPermission()
        }


    }

    // 권한을 확인하고 연락처를 가져오는 함수
    private fun checkPermission() {
        // 사용자에게 연락처 읽기 권한이 있는지 확인
        val status =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
        Log.d("status", "$status")
        if (status == PackageManager.PERMISSION_GRANTED) {
            // 권한이 이미 허용되어 있을 때의 처리
            Log.d("테스트입니다.", "permission granted")
            // 연락처를 가져와서 서버에 전송하는 함수 호출
            sendPhonebook()
        } else {
            // 권한이 없을 때 권한 요청 다이얼로그 표시
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                100
            )
            Log.d("test", "permission requested")
        }
    }

    @SuppressLint("Range")
    private fun sendPhonebook() {
        val cr = contentResolver
        val cur =
            cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        val phonebookAllDTO = PhonebookAllDTO()
        val phonebook: MutableMap<String, String> = mutableMapOf();

        if (cur!!.count > 0) {
            var line = ""
            while (cur!!.moveToNext()) {
                var phoneNumber = ""
                val id =
                    cur!!.getInt(cur!!.getColumnIndex(ContactsContract.Contacts._ID))
                line = String.format("%4d", id)
                val name =
                    cur!!.getString(cur!!.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                line += " $name"
                if ("1" == cur!!.getString(cur!!.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) {
                    val pCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                        arrayOf(id.toString()),
                        null
                    )
                    var i = 0
                    val pCount = pCur!!.count
                    val phoneNum = arrayOfNulls<String>(pCount)
                    val phoneType = arrayOfNulls<String>(pCount)
                    while (pCur!!.moveToNext()) {
                        phoneNum[i] =
                            pCur!!.getString(pCur!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        line += " " + phoneNum[i]
                        phoneNumber = phoneNum[i].toString()
                        phoneType[i] =
                            pCur!!.getString(pCur!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))
                        i++
                    }
                }

                Log.d("GetContact", "이름 : $name 번호 : $id 전화번호 :$phoneNumber")
                phonebook.put(phoneNumber, name);
            }
        }
        phonebookAllDTO.phonebook = phonebook;
        phonebookAllDTO.userId = 1;
        //이 객체 그대로 보내면 됨
        val call = service.phonebookSave(phonebookAllDTO)
        if (call != null) {
            call.enqueue(object : Callback<Void?> {
                override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                    Log.d("성공!!!!!!!!!!!!!!!!!", "성공 : ${response.raw()}")
                }

                override fun onFailure(call: Call<Void?>, t: Throwable) {
                    Log.d("실패!!!!!!!!!!", "실패 : $t")
                }
            })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 요청 코드가 올바른지 확인하고, 권한 요청 결과를 처리
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 사용자가 권한을 부여한 경우
            Log.d("test", "permission granted")
            // 연락처를 가져와서 서버에 전송하는 함수 호출
        } else {
            // 사용자가 권한을 거부한 경우
            Log.d("test", "permission denied")
            // 권한을 거부한 상황에 대한 처리를 추가
        }
    }


}