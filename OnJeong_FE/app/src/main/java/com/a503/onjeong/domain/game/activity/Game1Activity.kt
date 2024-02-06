package com.a503.onjeong.domain.game.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.RectF
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.a503.onjeong.R
import com.a503.onjeong.domain.game.activity.Game1Lobby
import com.a503.onjeong.domain.game.api.GameApiService
import com.a503.onjeong.domain.game.dto.UserGameDto
import com.a503.onjeong.domain.game.dto.UserGameResponseDto
import com.a503.onjeong.global.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.abs

class Game1Activity : AppCompatActivity() {
    private lateinit var start: Button
    private lateinit var exit: Button
    private lateinit var rank: Button
    private lateinit var timeTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var gameMarkTextView: TextView
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var imageViews: List<ImageView>
    private var selectedIndex = -1

    private val gameImages = listOf(
        R.drawable.game_image1,
        R.drawable.game_image3,
        R.drawable.game_image5,
        R.drawable.game_image7,
        R.drawable.game_image9,
        R.drawable.game_image11
    )
    private var imageNum = (0..48).toMutableList()
    private lateinit var sharedPreferences: SharedPreferences    
    private var userId: Long = 0
    private var score: Long = 0

    // 각 ImageView의 좌표 범위를 저장할 리스트
    private val imageViewCoordinates = mutableListOf<RectF>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game1_ready)
        // 유저이름 가져옴
        sharedPreferences = getSharedPreferences("mySharedPreferences", Context.MODE_PRIVATE)
        userId = sharedPreferences.getLong("userId", 0L)
        // 종료버튼
        exit = findViewById(R.id.end)
        exit.setOnClickListener {
            var intent = Intent(this, Game1Lobby::class.java)
            startActivity(intent)
        }
        // 시작버튼
        start = findViewById(R.id.start)
        start.setOnClickListener {
            setContentView(R.layout.activity_game1_start)
            initializeViews(49)
        }
    }

    private fun startTimer(gameTime: Int) {
        countDownTimer = object : CountDownTimer(gameTime.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timeTextView.text = "$secondsLeft 초"
            }

            override fun onFinish() {
                timeTextView.text = "끝"
               endGame()
            }
        }.start()
    }

    private fun initializeViews(size: Int) {
        timeTextView = findViewById(R.id.time_text)
        scoreTextView = findViewById(R.id.score_text)
        gameMarkTextView = findViewById(R.id.game_mark)
        gameMarkTextView.text = "팡게임"
        val imageIds = mutableListOf<Int>()
        for (i in 1..49) {
            val resourceId = resources.getIdentifier("game1_block$i", "id", packageName)
            imageIds.add(resourceId)
        }

        imageViews = mutableListOf()
        for (i in 0 until size) {
            val imageView = findViewById<ImageView>(imageIds[i])
            (imageViews as MutableList<ImageView>).add(imageView)
        }
        // 이미지뷰의 좌표를 RectF로 저장
        imageViews.forEach { imageView ->
            val rect = RectF()
            imageView.viewTreeObserver.addOnGlobalLayoutListener {
                val location = IntArray(2)
                imageView.getLocationOnScreen(location)
                rect.set(
                    location[0].toFloat(),
                    location[1].toFloat(),
                    (location[0] + imageView.width).toFloat(),
                    (location[1] + imageView.height).toFloat()
                )
            }
            imageViewCoordinates.add(rect)
        }

        // 이미지뷰에 터치 리스너 등록
        for (i in imageViews.indices) {
            imageViews[i].setOnTouchListener(MyTouchListener(i))
        }
        setRandomColors()
        blockCheck()
        startTimer(100000)
        score = 0
        scoreTextView.text = "$score 점"
    }

    private fun setRandomColors() {
        for ((index, imageView) in imageViews.withIndex()) {
            val randomImage = gameImages.random()
            imageNum[index] = randomImage
            imageView.setImageResource(randomImage)
        }
    }

    private fun blockCheck(): Boolean {
        // 가로로 3개 이상인 블록 찾아서 색을 바꿈
        var flag: Boolean = false
        for (i in 0 until 7) {
            for (j in 0 until 5) {
                for (k in 3..7) {
                    if (j + k > 7) {
                        continue
                    }
                    val horizontalBlocks = (0 until k).map { i * 7 + j + it }
                    if (checkColor(horizontalBlocks)) {
                        flag = true
                        changeColor(horizontalBlocks)
                        continue
                    }
                }
            }
        }
        // 세로로 3개 이상인 블록 찾아서 색을 바꿈
        for (i in 0 until 5) {
            for (j in 0 until 7) {
                for (k in 3..7) {
                    if (i + k > 7) {
                        continue
                    }
                    val verticalBlocks = (0 until k).map { (i + it) * 7 + j }
                    if (checkColor(verticalBlocks)) {
                        flag = true
                        changeColor(verticalBlocks)
                        continue
                    }
                }
            }
        }
        // 만약 터지는 블럭이 있으면 true 반환
        if (flag) {
            return true
        }
        return false
    }

    private fun checkColor(blocks: List<Int>): Boolean {
        val firstColor = imageNum[blocks[0]]
        for (block in blocks) {
            if (imageNum[block] != firstColor) {
                return false
            }
        }
        return true
    }

    private fun changeColor(blocks: List<Int>) {
        score = score + (blocks.size * 10)
        scoreTextView.text = "$score 점"
        for (index in blocks.indices) {
            val randomImage = gameImages.random()
            imageNum[blocks[index]] = randomImage
            Handler(Looper.getMainLooper()).postDelayed({
                imageViews[blocks[index]].setImageResource(randomImage)
            }, 100)
        }
    }

    // 두개의 색의위치를 바꾸는 메서드
    private fun switch(index1: Int, index2: Int) {
        val temp = imageNum[index1]
        imageNum[index1] = imageNum[index2]
        imageNum[index2] = temp
        imageViews[index1].setImageResource(imageNum[index1])
        imageViews[index2].setImageResource(imageNum[index2])
    }

    // 각자의 색깔을 움직이기 위한 메서드
    inner class MyTouchListener(private val index: Int) : View.OnTouchListener {
        // 주변이미지 저장할 배열 매 클릭마다 초기화
        private val aroundImageView = mutableListOf<RectF>()
        private val aroundImageViewIndex = mutableListOf<Int>()
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 사용자가 화면을 처음 터치했을 때
                    selectedIndex = index
                    // 클릭된 이미지뷰의 주변 이미지뷰를 
                    // aroundImageView에 넣음
                    
                    var tmp = index-1
                    if(tmp>=0 && index % 7 != 0){
                        addAroundView(tmp)
                    }
                    tmp = index+1
                    if(tmp<=48 && tmp % 7 !=0) {
                        addAroundView(tmp)
                    }
                    tmp = index-7
                    if(tmp>=0) {
                        addAroundView(tmp)
                    }
                    tmp = index+7
                    if(tmp<=48) {
                        addAroundView(tmp)
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    // 사용자가 손가락을 화면에서 뗐을 때
                    // 선택된 이미지뷰와 가장 가까운 이미지뷰 찾기
                    val closestIndex = findClosestImageView(event.rawX, event.rawY)
                    if (closestIndex != -1 && selectedIndex != closestIndex) {
                        var tmp = selectedIndex
                        // 선택된 이미지뷰와 가장 가까운 이미지뷰와 위치 교환
                        switch(selectedIndex, closestIndex) // 두개 바꾼이후 터지는지 확인
                        if (!blockCheck()) {
                            // 점수가 오르지 않으면 0.1초후 다시 원위치
                            Handler(Looper.getMainLooper()).postDelayed({
                                // 왜 switch 안에 selectedIndex를 넣으면 -1로 초기화될까
                                switch(tmp, closestIndex)
                            }, 100)
                        }
                    }
                    selectedIndex = -1
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                }
            }
            return false
        }
        private fun addAroundView(index : Int) {
            println(index)
            aroundImageViewIndex.add(index)
            aroundImageView.add(imageViewCoordinates.get(index))
        }
        // 선택된 좌표와 가장 가까운 이미지뷰의 인덱스 찾기
        private fun findClosestImageView(x: Float, y: Float): Int {
            var closestDistance = Float.MAX_VALUE
            var closestIndex = -1

            for ((i, rect) in aroundImageView.withIndex()) {
                val distance = calculateDistance(x, y, rect.centerX(), rect.centerY())
                if (distance < closestDistance) {
                    closestDistance = distance
                    closestIndex = i
                }
            }
            return aroundImageViewIndex.get(closestIndex)
        }

        // 두 좌표 사이의 거리 계산
        private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            return Math.hypot(x1.toDouble() - x2.toDouble(), y1.toDouble() - y2.toDouble())
                .toFloat()
        }
    }


    private fun sendScore(userId: Long, gameId: Long, score: Long) {
        // NetRetrofit을 생성
        val retrofit = RetrofitClient.getApiClient(this)
        // NetRetrofit의 service를 통해 호출
        val service = retrofit.create(GameApiService::class.java)
        // UserGameDto 생성
        val userGameDto = UserGameDto(userId, gameId, score)
        var call = service.saveScore(userGameDto)

        call.enqueue(object : Callback<UserGameResponseDto> {
            override fun onResponse(
                call: Call<UserGameResponseDto>,
                response: Response<UserGameResponseDto>
            )  {
                if (response.isSuccessful) {
                    // 성공적으로 서버에 전송된 경우
                    // 추가적인 작업 수행
                    val userGameInfo = response.body()
                    if (userGameInfo != null) {
                        // 순서대로 1. 내점수  2. 플레이어 이름  3. 플레이어 최고점수
                        var tmpName : String = userGameInfo.userName
                        var tmpScore : String = userGameInfo.userGameScore.toString()
                        findViewById<TextView>(R.id.result_name).text = "유저 이름 : $tmpName "
                        findViewById<TextView>(R.id.result_high_score).text = "최고 점수 : $tmpScore 점"

                    }
                } else {
                    // 스프링에서 정보 불러오기 실패 시 호출
                    Log.d("실패", "실패 : ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<UserGameResponseDto>, t: Throwable) {
                TODO("Not yet implemented")
            }


        })
    }
    private fun endGame() {
        setContentView(R.layout.activity_game_result)
        findViewById<TextView>(R.id.result_score).text = "현재 점수 : $score 점"
        sendScore(userId, 1, score)

        exit = findViewById(R.id.game_exit)
        rank = findViewById(R.id.game_rank)
        exit.setOnClickListener {
            val intent = Intent(this, Game1Lobby::class.java)
            startActivity(intent)
        }
        rank.setOnClickListener {
            val intent = Intent(this, GameRankActivity::class.java)
            startActivity(intent)
        }
    }
}
