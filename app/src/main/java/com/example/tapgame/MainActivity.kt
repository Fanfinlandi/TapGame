package com.example.tapgame

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.random.Random



class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: ConstraintLayout

    private lateinit var tapButton: Button

    private lateinit var timeText: TextView
    private lateinit var coinsText: TextView
    private lateinit var shopButton: ImageButton
    private lateinit var mAdView: AdView
    private lateinit var lastScoreText: TextView
    private lateinit var trophyButton: ImageButton

    private var score = 0
    private var coins = 0
    private var timeLeft = 30_000L
    private var gameRunning = false
    private var timer: CountDownTimer? = null
    private var lastScore = 0

    private var totalTaps = 0
    private var highestScore = 0
    private var timeFreezeActivations = 0
    private var superSpeedActivations = 0

    // CHANGED: Added counters for booster activations
    private var doubleTapActivations = 0
    private var scoreFrenzyActivations = 0

    // CHANGED: Existing achievements
    private var achievementTapper = false
    private var achievementSpeedMaster = false
    private var achievementCoinCollector = false

    // CHANGED: New achievement variables
    private var achievementTimeTraveler = false
    private var achievementHypersonic = false
    private var achievementDoubleTapPro = false
    private var achievementMasterTapper = false
    private var achievementUltimateCollector = false
    private var achievementCenturyClub = false // ADDED
    private var achievementScoringSpree = false // ADDED

    private var doubleTapActive = false
    private var extendedTimeActive = false
    private var superSpeedActive = false
    private var timeFreezeActive = false
    private var scoreFrenzyActive = false

    private val handler = Handler(Looper.getMainLooper())
    private var isSuperSpeedRunning = false
    private val superSpeedRunnable = object : Runnable {
        override fun run() {
            moveTapButton()
            if (isSuperSpeedRunning) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    private var isTimeFreezeRunning = false
    private val timeFreezeRunnable = Runnable {
        isTimeFreezeRunning = false
        timer?.cancel()
        startCountDown(timeLeft)
    }

    private val PREFS_NAME = "MyGamePrefs"
    private val KEY_COINS = "coins"
    private val KEY_LAST_SCORE = "last_score"
    private val KEY_TOTAL_TAPS = "total_taps"
    private val KEY_HIGHEST_SCORE = "highest_score"
    private val KEY_TIME_FREEZE_ACTIVATIONS = "time_freeze_activations"
    private val KEY_SUPER_SPEED_ACTIVATIONS = "super_speed_activations"
    private val KEY_DOUBLE_TAP_ACTIVATIONS = "double_tap_activations" // ADDED
    private val KEY_SCORE_FRENZY_ACTIVATIONS = "score_frenzy_activations" // ADDED

    private val KEY_ACHIEVEMENT_TAPPER = "achievement_tapper"
    private val KEY_ACHIEVEMENT_SPEED_MASTER = "achievement_speed_master"
    private val KEY_ACHIEVEMENT_COIN_COLLECTOR = "achievement_coin_collector"
    private val KEY_ACHIEVEMENT_TIME_TRAVELER = "achievement_time_traveler"
    private val KEY_ACHIEVEMENT_HYPERSONIC = "achievement_hypersonic"
    private val KEY_ACHIEVEMENT_DOUBLE_TAP_PRO = "achievement_double_tap_pro"
    private val KEY_ACHIEVEMENT_MASTER_TAPPER = "achievement_master_tapper"
    private val KEY_ACHIEVEMENT_ULTIMATE_COLLECTOR = "achievement_ultimate_collector"
    private val KEY_ACHIEVEMENT_CENTURY_CLUB = "achievement_century_club" // ADDED
    private val KEY_ACHIEVEMENT_SCORING_SPREE = "achievement_scoring_spree" // ADDED

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rootLayout = findViewById(R.id.main) // Twój ConstraintLayout w activity_main.xml
        applySeasonTheme()

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        tapButton = findViewById(R.id.tapButton)
        timeText = findViewById(R.id.timeText)
        coinsText = findViewById(R.id.coinsText)
        shopButton = findViewById(R.id.shopButton)
        lastScoreText = findViewById(R.id.lastScoreText)
        trophyButton = findViewById(R.id.trophyButton)

        loadGameData()
        updateUI()

        tapButton.setOnClickListener {
            if (gameRunning) {
                totalTaps++
                var pointsToAdd = 1
                if (doubleTapActive) {
                    pointsToAdd = 2
                    // CHANGED: Increment the counter only when the booster is active
                    doubleTapActivations++
                }
                if (scoreFrenzyActive) {
                    pointsToAdd = 5
                    // CHANGED: Increment the counter only when the booster is active
                    scoreFrenzyActivations++
                }

                score += pointsToAdd
                tapButton.text = "$score"

                // CHANGED: Achievements are now checked at the end of the round
            } else {
                startGame()
            }
        }

        shopButton.setOnClickListener {
            showShopDialog()
        }

        trophyButton.setOnClickListener {
            showAchievementsDialog()
        }

        startGame()
    }

    private fun startGame() {
        score = 0
        gameRunning = true
        tapButton.text = "0"
        tapButton.isEnabled = true

        val params = tapButton.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        if (params != null) {
            params.topToTop = R.id.main
            params.bottomToBottom = R.id.main
            params.leftToLeft = R.id.main
            params.rightToRight = R.id.main
            tapButton.requestLayout()
        }

        isSuperSpeedRunning = false
        handler.removeCallbacks(superSpeedRunnable)
        isTimeFreezeRunning = false
        handler.removeCallbacks(timeFreezeRunnable)

        if (superSpeedActive) {
            superSpeedActivations++
            isSuperSpeedRunning = true
            handler.post(superSpeedRunnable)
            superSpeedActive = false
        }
        if (timeFreezeActive) {
            timeFreezeActivations++
            isTimeFreezeRunning = true
            timer?.cancel()
            Toast.makeText(this, "Time is frozen for 5 seconds!", Toast.LENGTH_SHORT).show()
            handler.postDelayed(timeFreezeRunnable, 5000)
            timeFreezeActive = false
        }

        if (extendedTimeActive) {
            timeLeft = 40_000L
            extendedTimeActive = false
        } else {
            timeLeft = 30_000L
        }

        timer?.cancel()
        startCountDown(timeLeft)

        doubleTapActive = false
        scoreFrenzyActive = false
    }

    private fun startCountDown(time: Long) {
        timer = object : CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeText.text = "Time: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                endGame()
            }
        }.start()
    }

    private fun endGame() {
        timeText.text = "End!"
        Toast.makeText(this@MainActivity, "Your Score: $score", Toast.LENGTH_LONG).show()

        if (score > highestScore) {
            highestScore = score
            Toast.makeText(this@MainActivity, "NEW HIGH SCORE: $highestScore!", Toast.LENGTH_LONG).show()
        }

        coins += score / 10
        lastScore = score

        // CHANGED: Check achievements at the end of the round
        checkAchievements()
        updateUI()
        saveGameData()

        gameRunning = false
        tapButton.text = "Play again!"
        tapButton.isEnabled = true

        isSuperSpeedRunning = false
        handler.removeCallbacks(superSpeedRunnable)
    }

    private fun showShopDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.shop_dialog, null)
        dialogBuilder.setView(view)
        val alertDialog = dialogBuilder.create()

        val present1 = view.findViewById<Button>(R.id.present1)
        val present2 = view.findViewById<Button>(R.id.present2)
        val present3 = view.findViewById<Button>(R.id.present3)
        val closeButton = view.findViewById<Button>(R.id.closeButton)

        present1.setOnClickListener { buyBooster(alertDialog, 100) }
        present2.setOnClickListener { buyBooster(alertDialog, 100) }
        present3.setOnClickListener { buyBooster(alertDialog, 100) }

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun buyBooster(dialog: AlertDialog, cost: Int) {
        if (coins >= cost) {
            coins -= cost
            activateRandomBooster()
            dialog.dismiss()
            updateUI()
            saveGameData()
        } else {
            Toast.makeText(this, "You don't have enough coins!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun activateRandomBooster() {
        val boosterType = Random.nextInt(5)
        when (boosterType) {
            0 -> {
                Toast.makeText(this, "You have bought booster: Double Points! Active in the next game.", Toast.LENGTH_SHORT).show()
                doubleTapActive = true
            }
            1 -> {
                Toast.makeText(this, "You have bought booster: Extended Time! Active in the next game.", Toast.LENGTH_SHORT).show()
                extendedTimeActive = true
            }
            2 -> {
                Toast.makeText(this, "You have bought booster: Super Speed! Active in the next game.", Toast.LENGTH_SHORT).show()
                superSpeedActive = true
            }
            3 -> {
                Toast.makeText(this, "You have bought booster: Time Freeze! Active in the next game.", Toast.LENGTH_SHORT).show()
                timeFreezeActive = true
            }
            4 -> {
                Toast.makeText(this, "You have bought booster: Score Frenzy! Active in the next game.", Toast.LENGTH_SHORT).show()
                scoreFrenzyActive = true
            }
        }
    }

    private fun moveTapButton() {
        val parent = tapButton.parent as View
        val parentWidth = parent.width - tapButton.width
        val parentHeight = parent.height - tapButton.height

        if (parentWidth > 0 && parentHeight > 0) {
            val randomX = Random.nextInt(parentWidth)
            val randomY = Random.nextInt(parentHeight)

            val layoutParams = tapButton.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (layoutParams != null) {
                layoutParams.leftMargin = randomX
                layoutParams.topMargin = randomY
                tapButton.requestLayout()
            }
        }
    }

    private fun saveGameData() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putInt(KEY_COINS, coins)
        editor.putInt(KEY_LAST_SCORE, lastScore)
        editor.putInt(KEY_TOTAL_TAPS, totalTaps)
        editor.putInt(KEY_HIGHEST_SCORE, highestScore)
        editor.putInt(KEY_TIME_FREEZE_ACTIVATIONS, timeFreezeActivations)
        editor.putInt(KEY_SUPER_SPEED_ACTIVATIONS, superSpeedActivations)
        editor.putInt(KEY_DOUBLE_TAP_ACTIVATIONS, doubleTapActivations) // ADDED
        editor.putInt(KEY_SCORE_FRENZY_ACTIVATIONS, scoreFrenzyActivations) // ADDED

        editor.putBoolean(KEY_ACHIEVEMENT_TAPPER, achievementTapper)
        editor.putBoolean(KEY_ACHIEVEMENT_SPEED_MASTER, achievementSpeedMaster)
        editor.putBoolean(KEY_ACHIEVEMENT_COIN_COLLECTOR, achievementCoinCollector)
        editor.putBoolean(KEY_ACHIEVEMENT_TIME_TRAVELER, achievementTimeTraveler)
        editor.putBoolean(KEY_ACHIEVEMENT_HYPERSONIC, achievementHypersonic)
        editor.putBoolean(KEY_ACHIEVEMENT_DOUBLE_TAP_PRO, achievementDoubleTapPro)
        editor.putBoolean(KEY_ACHIEVEMENT_MASTER_TAPPER, achievementMasterTapper)
        editor.putBoolean(KEY_ACHIEVEMENT_ULTIMATE_COLLECTOR, achievementUltimateCollector)
        editor.putBoolean(KEY_ACHIEVEMENT_CENTURY_CLUB, achievementCenturyClub) // ADDED
        editor.putBoolean(KEY_ACHIEVEMENT_SCORING_SPREE, achievementScoringSpree) // ADDED

        editor.apply()
    }

    private fun loadGameData() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        coins = sharedPrefs.getInt(KEY_COINS, 0)
        lastScore = sharedPrefs.getInt(KEY_LAST_SCORE, 0)
        totalTaps = sharedPrefs.getInt(KEY_TOTAL_TAPS, 0)
        highestScore = sharedPrefs.getInt(KEY_HIGHEST_SCORE, 0)
        timeFreezeActivations = sharedPrefs.getInt(KEY_TIME_FREEZE_ACTIVATIONS, 0)
        superSpeedActivations = sharedPrefs.getInt(KEY_SUPER_SPEED_ACTIVATIONS, 0)
        doubleTapActivations = sharedPrefs.getInt(KEY_DOUBLE_TAP_ACTIVATIONS, 0) // ADDED
        scoreFrenzyActivations = sharedPrefs.getInt(KEY_SCORE_FRENZY_ACTIVATIONS, 0) // ADDED

        achievementTapper = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_TAPPER, false)
        achievementSpeedMaster = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_SPEED_MASTER, false)
        achievementCoinCollector = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_COIN_COLLECTOR, false)
        achievementTimeTraveler = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_TIME_TRAVELER, false)
        achievementHypersonic = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_HYPERSONIC, false)
        achievementDoubleTapPro = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_DOUBLE_TAP_PRO, false)
        achievementMasterTapper = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_MASTER_TAPPER, false)
        achievementUltimateCollector = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_ULTIMATE_COLLECTOR, false)
        achievementCenturyClub = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_CENTURY_CLUB, false) // ADDED
        achievementScoringSpree = sharedPrefs.getBoolean(KEY_ACHIEVEMENT_SCORING_SPREE, false) // ADDED
    }

    private fun updateUI() {
        coinsText.text = "Coins: $coins$"
        lastScoreText.text = "Last Score: $lastScore"
    }

    // CHANGED: New function to check all achievements at the end of a round
    private fun checkAchievements() {
        // Per-round achievements
        if (score >= 500 && !achievementSpeedMaster) {
            achievementSpeedMaster = true
            Toast.makeText(this, "Achievement unlocked: Speed Master (500 points in one round)!", Toast.LENGTH_LONG).show()
        }
        if (score >= 100 && !achievementCenturyClub) {
            achievementCenturyClub = true
            Toast.makeText(this, "Achievement unlocked: Century Club (100 points in one round)!", Toast.LENGTH_LONG).show()
        }

        // Cumulative achievements
        if (totalTaps >= 100 && !achievementTapper) {
            achievementTapper = true
            Toast.makeText(this, "Achievement unlocked: Tapper (100 taps)!", Toast.LENGTH_LONG).show()
        }
        if (totalTaps >= 1000 && !achievementMasterTapper) {
            achievementMasterTapper = true
            Toast.makeText(this, "Achievement unlocked: Master Tapper (1000 taps)!", Toast.LENGTH_LONG).show()
        }
        if (coins >= 1000 && !achievementCoinCollector) {
            achievementCoinCollector = true
            Toast.makeText(this, "Achievement unlocked: Coin Collector (1000 coins)!", Toast.LENGTH_LONG).show()
        }
        if (coins >= 5000 && !achievementUltimateCollector) {
            achievementUltimateCollector = true
            Toast.makeText(this, "Achievement unlocked: Ultimate Collector (5000 coins)!", Toast.LENGTH_LONG).show()
        }
        if (timeFreezeActivations >= 10 && !achievementTimeTraveler) {
            achievementTimeTraveler = true
            Toast.makeText(this, "Achievement unlocked: Time Traveler (10 Time Freezes)!", Toast.LENGTH_LONG).show()
        }
        if (superSpeedActivations >= 10 && !achievementHypersonic) {
            achievementHypersonic = true
            Toast.makeText(this, "Achievement unlocked: Hypersonic (10 Super Speeds)!", Toast.LENGTH_LONG).show()
        }
        if (doubleTapActivations >= 5 && !achievementDoubleTapPro) {
            achievementDoubleTapPro = true
            Toast.makeText(this, "Achievement unlocked: Double Tap Pro (5 activations)!", Toast.LENGTH_LONG).show()
        }
        if (scoreFrenzyActivations >= 5 && !achievementScoringSpree) {
            achievementScoringSpree = true
            Toast.makeText(this, "Achievement unlocked: Scoring Spree (5 activations)!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAchievementsDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.achievements_dialog, null)
        dialogBuilder.setView(view)
        val alertDialog = dialogBuilder.create()

        val achievementsListLayout = view.findViewById<LinearLayout>(R.id.achievementsListLayout)
        val closeButton = view.findViewById<Button>(R.id.closeAchievementsButton)

        addAchievementItem(achievementsListLayout, "Tapper", "Hit the button 100 times", achievementTapper)
        addAchievementItem(achievementsListLayout, "Speed Master", "Get a score of 500 in one round", achievementSpeedMaster)
        addAchievementItem(achievementsListLayout, "Coin Collector", "Collect 1000 coins", achievementCoinCollector)
        addAchievementItem(achievementsListLayout, "Time Traveler", "Activate Time Freeze 10 times", achievementTimeTraveler)
        addAchievementItem(achievementsListLayout, "Hypersonic", "Activate Super Speed 10 times", achievementHypersonic)
        addAchievementItem(achievementsListLayout, "Master Tapper", "Hit the button 1000 times", achievementMasterTapper)
        addAchievementItem(achievementsListLayout, "Ultimate Collector", "Collect 5000 coins", achievementUltimateCollector)
        addAchievementItem(achievementsListLayout, "Century Club", "Get a score of 100 in one round", achievementCenturyClub)
        addAchievementItem(achievementsListLayout, "Double Tap Pro", "Activate Double Tap 5 times", achievementDoubleTapPro)
        addAchievementItem(achievementsListLayout, "Scoring Spree", "Activate Score Frenzy 5 times", achievementScoringSpree)

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun addAchievementItem(parent: LinearLayout, title: String, description: String, isCompleted: Boolean) {
        val itemLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val checkBox = CheckBox(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isChecked = isCompleted
            isEnabled = false
        }

        val textLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 0, 0, 0)
        }

        val titleTextView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = title
            textSize = 18f
        }

        val descTextView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = description
            textSize = 14f
        }

        textLayout.addView(titleTextView)
        textLayout.addView(descTextView)
        itemLayout.addView(checkBox)
        itemLayout.addView(textLayout)
        parent.addView(itemLayout)
    }
    private fun applySeasonTheme() {
        val season = getSeason()
        when (season) {
            "wiosna" -> {
               /* rootLayout.setBackgroundResource(R.drawable.bg_wiosna)
                tapButton.setBackgroundResource(R.drawable.btn_wiosna)*/
            }
            "lato" -> {
              /*  rootLayout.setBackgroundResource(R.drawable.bg_lato)
                tapButton.setBackgroundResource(R.drawable.btn_lato)*/
            }
            "jesień" -> {
               /* rootLayout.setBackgroundResource(R.drawable.bg_jesien)*/
               /* tapButton.setBackgroundResource(R.drawable.btn_jesien)*/
            }
            "zima" -> {
             /*   rootLayout.setBackgroundResource(R.drawable.bg_zima)
                tapButton.setBackgroundResource(R.drawable.btn_zima)*/
            }
        }
    }

    private fun getSeason(): String {
        val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        return when (month) {
            in 3..5 -> "wiosna"
            in 6..8 -> "lato"
            in 9..11 -> "jesień"
            else -> "zima"
        }
    }

}

private fun ConstraintLayout.LayoutParams.removeRule(centerInParent: Int) {}
