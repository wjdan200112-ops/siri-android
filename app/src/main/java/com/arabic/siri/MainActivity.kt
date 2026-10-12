package com.arabic.siri

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private lateinit var statusText: TextView
    private lateinit var heardText: TextView
    private lateinit var listenButton: Button
    private lateinit var voiceSpinner: Spinner
    private lateinit var speedBar: SeekBar
    private lateinit var voiceInfo: TextView
    private var voices: List<Voice> = emptyList()
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 32, 28, 24)
            setBackgroundColor(0xFFF4F8FF.toInt())
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
        }
        root.addView(TextView(this).apply {
            text = "سيري AI"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(0xFF174A85.toInt())
        }, params())
        root.addView(TextView(this).apply {
            text = "مساعدك الصوتي العربي"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(0xFF53677F.toInt())
        }, params())

        statusText = TextView(this).apply {
            text = "اضغط على زر الاستماع وتحدث بالعربية."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFF174A85.toInt())
            setPadding(8, 24, 8, 16)
        }
        root.addView(statusText, params())

        heardText = TextView(this).apply {
            text = "سيظهر كلامك هنا."
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(0xFF202B3B.toInt())
            setPadding(12, 18, 12, 18)
        }
        root.addView(heardText, LinearLayout.LayoutParams(-1, 0, 1f))

        listenButton = Button(this).apply {
            text = "🎙 بدء الاستماع"
            textSize = 18f
            setOnClickListener { requestPermissionAndListen() }
        }
        root.addView(listenButton, params())

        root.addView(TextView(this).apply {
            text = "الصوت العربي"
            textSize = 15f
            setTextColor(0xFF174A85.toInt())
        }, params())
        voiceSpinner = Spinner(this)
        root.addView(voiceSpinner, params())
        voiceInfo = TextView(this).apply {
            text = "يتم تحميل الأصوات المتاحة على الهاتف…"
            textSize = 12f
            setTextColor(0xFF53677F.toInt())
        }
        root.addView(voiceInfo, params())

        root.addView(TextView(this).apply {
            text = "سرعة الكلام"
            textSize = 15f
            setTextColor(0xFF174A85.toInt())
        }, params())
        speedBar = SeekBar(this).apply {
            max = 60
            progress = 30
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tts?.setSpeechRate(0.70f + progress / 100f)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        root.addView(speedBar, params())

        root.addView(Button(this).apply {
            text = "تجربة الصوت"
            setOnClickListener { speak("مرحبًا، أنا سيري. كيف يمكنني مساعدتك اليوم؟") }
        }, params())

        root.addView(TextView(this).apply {
            text = "نسخة أولية: الأوامر محدودة، ولا يوجد استماع دائم عند قفل الشاشة."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF68778A.toInt())
            setPadding(0, 12, 0, 0)
        }, params())
        setContentView(root)
    }

    private fun params() = LinearLayout.LayoutParams(-1, -2).apply {
        bottomMargin = 10
    }

    override fun onInit(result: Int) {
        if (result != TextToSpeech.SUCCESS) {
            runOnUiThread { voiceInfo.text = "تعذر تشغيل محرك تحويل النص إلى كلام." }
            return
        }
        val languageResult = tts?.setLanguage(Locale("ar")) ?: TextToSpeech.LANG_NOT_SUPPORTED
        ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED
        tts?.setSpeechRate(1.0f)
        runOnUiThread { loadArabicVoices() }
    }

    private fun loadArabicVoices() {
        voices = try {
            tts?.voices.orEmpty()
                .filter { it.locale.language.equals("ar", ignoreCase = true) }
                .distinctBy { it.name }
                .sortedBy { it.isNetworkConnectionRequired }
        } catch (_: Exception) { emptyList() }

        if (voices.isEmpty()) {
            voiceSpinner.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item,
                listOf("الصوت العربي الافتراضي")
            )
            voiceSpinner.isEnabled = false
            voiceInfo.text = "لم يتم العثور على أصوات عربية إضافية. تحقق من إعدادات تحويل النص إلى كلام في الهاتف."
            return
        }

        voiceSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            voices.map { it.name + if (it.isNetworkConnectionRequired) " (إنترنت)" else " (محلي)" }
        )
        voiceSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                try {
                    tts?.voice = voices[position]
                    ttsReady = true
                    voiceInfo.text = "تم اختيار الصوت المتاح على جهازك. قد تختلف الجودة حسب محرك النطق."
                } catch (_: Exception) {
                    voiceInfo.text = "تعذر اختيار هذا الصوت."
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun requestPermissionAndListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 200)
            return
        }
        startListening()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else if (requestCode == 200) {
            statusText.text = "يلزم السماح بالميكروفون لاستخدام الأوامر الصوتية."
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "خدمة التعرف على الكلام غير متاحة على الهاتف."
            return
        }
        listenButton.isEnabled = false
        statusText.text = "أستمع إليك الآن…"
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { speech ->
                speech.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() { statusText.text = "تفضل، أكمل كلامك…" }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { statusText.text = "أعالج كلامك…" }
                    override fun onError(error: Int) {
                        listenButton.isEnabled = true
                        statusText.text = "لم أتمكن من فهم الكلام. حاول مرة أخرى."
                    }
                    override fun onResults(results: Bundle?) {
                        listenButton.isEnabled = true
                        val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull().orEmpty().trim()
                        heardText.text = spoken.ifBlank { "لم أتعرف على الكلام." }
                        if (spoken.isNotBlank()) handleCommand(spoken)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            listenButton.isEnabled = true
            statusText.text = "تعذر تشغيل الاستماع. تحقق من إذن الميكروفون وخدمة الصوت."
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale("ar"))
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
            .replace("ى", "ي")
        val reply = when {
            command.contains("يوتيوب") -> {
                openUrl("https://www.youtube.com")
                "سأفتح يوتيوب."
            }
            command.contains("جوجل") || command.contains("ابحث") -> {
                openUrl("https://www.google.com")
                "سأفتح بحث جوجل."
            }
            command.contains("واتساب") -> {
                val launch = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (launch != null) {
                    startActivity(launch)
                    "سأفتح واتساب."
                } else "لم أجد تطبيق واتساب مثبتًا على الهاتف."
            }
            command.contains("السلام") || command.contains("مرحبا") || command.contains("اهلا") ->
                "أهلًا بك! أنا سيري، كيف أساعدك؟"
            command.contains("اسمك") -> "اسمي سيري، مساعدك الصوتي العربي."
            command.contains("مساعدة") || command.contains("ماذا تستطيع") ->
                "يمكنني فتح يوتيوب وجوجل وواتساب، والرد على بعض التحيات. هذه نسخة أولية."
            command.contains("توقفي") || command.contains("اسكتي") ->
                "حسنًا، يمكنك التحدث معي مجددًا في أي وقت."
            else ->
                "سمعتك، لكن المحادثة الذكية العامة غير مفعلة في هذه النسخة بعد."
        }
        speak(reply)
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح الرابط.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speak(message: String) {
        statusText.text = message
        if (ttsReady) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "siri_reply")
        else Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
