package com.example.chatgpt3

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.messages.MessagesList
import com.stfalcon.chatkit.messages.MessagesListAdapter
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {
    lateinit var sendButton:ImageButton
    lateinit var micBtn:ImageButton
    lateinit var editText: EditText
    lateinit var messageList: MessagesList
    lateinit var us:User
    lateinit var chatgpt:User
    lateinit var adapter:MessagesListAdapter<Message>
    lateinit var tts : TextToSpeech
    lateinit var speechRecognizer : SpeechRecognizer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendButton = findViewById(R.id.imageButton)
        micBtn = findViewById(R.id.imageButton3)
        editText = findViewById(R.id.editTextTextPersonName)
        messageList = findViewById(R.id.messagesList)

        var imageLoader: ImageLoader = object:ImageLoader{
            override fun loadImage(imageView: ImageView?, url: String?, payload: Any?) {
                Picasso.get().load(url).into(imageView)
            }

        }
         adapter = MessagesListAdapter<Message>("1", imageLoader)
         messageList.setAdapter(adapter)

        us = User("1","Umam","")
        chatgpt = User("2","ChatGPT","")

        sendButton.setOnClickListener {
            var message : Message = Message("m1",editText.text.toString(),us,Calendar.getInstance().time,"")
            adapter.addToStart(message,true)
            if (editText.text.toString().startsWith("generate image")){
                generateImages(editText.text.toString())
            } else {
                performAction(editText.text.toString())
            }
            editText.text.clear()
        }

        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if (it != TextToSpeech.ERROR){
                tts.setLanguage(Locale.UK)
            }
        })

        if (ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO),121)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        var intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        micBtn.setOnClickListener {
            speechRecognizer.startListening(intent)
        }

        speechRecognizer.setRecognitionListener(object :RecognitionListener{
            override fun onReadyForSpeech(p0: Bundle?) {
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(p0: Float) {
            }

            override fun onBufferReceived(p0: ByteArray?) {
            }

            override fun onEndOfSpeech() {
            }

            override fun onError(p0: Int) {
            }

            override fun onResults(results: Bundle?) {
                var arraysOfRes = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                var message : Message = Message("m1",arraysOfRes!!.get(0),us,Calendar.getInstance().time,"")
                adapter.addToStart(message,true)
                if (arraysOfRes!!.get(0).startsWith("generate image")){
                    generateImages(arraysOfRes!!.get(0))
                } else {
                    performAction(arraysOfRes!!.get(0))
                }
            }

            override fun onPartialResults(p0: Bundle?) {
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    var isTTS : Boolean = false
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.voice){
            if (isTTS){
                isTTS = false
                item.setIcon(R.drawable.ic_baseline_voice_over_off_48)
                tts.stop()
            } else {
                isTTS = true
                item.setIcon(R.drawable.ic_baseline_record_voice_over_48)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun performAction(input:String){
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.openai.com/v1/completions"

        val jsonObject = JSONObject()
        jsonObject.put("model", "text-davinci-003")
//        jsonObject.put("model", "gpt-3.5-turbo")
        jsonObject.put("prompt",input)
//        jsonObject.put("model", "text-davinci-003")
        jsonObject.put("temperature", 0)
        jsonObject.put("max_tokens", 300)
//        jsonObject.put("presence_penalty", 0.6)
//        jsonObject.put("model", "gpt-3.5-turbo")
// Request a string response from the provided URL.
        val stringRequest = object :JsonObjectRequest(
            Request.Method.POST, url,jsonObject,
            Response.Listener<JSONObject> { response ->
                // Display the first 500 characters of the response string.
                var answer = response.getJSONArray("choices").getJSONObject(0).getString("text")
//                resultTv.text = answer
                var message : Message = Message("m2",answer.trim(),chatgpt,Calendar.getInstance().time,"")
                adapter.addToStart(message,true)
                if (isTTS) {
                    tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            },
            Response.ErrorListener { })
        {
            override fun getHeaders(): MutableMap<String, String> {
                var map = HashMap<String,String>()
                map.put("Content-Type", "application/json")
                map.put("Authorization", "Bearer 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")

                return map
            }
        }

        stringRequest.setRetryPolicy(object : RetryPolicy{
            override fun getCurrentTimeout(): Int {
                return 60000
            }

            override fun getCurrentRetryCount(): Int {
                return 15
            }

            override fun retry(error: VolleyError?) {

            }

        })

// Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    fun generateImages(input:String){
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.openai.com/v1/images/generations"

        val jsonObject = JSONObject()
//        jsonObject.put("model", "text-davinci-003")
//        jsonObject.put("model", "gpt-3.5-turbo")
        jsonObject.put("prompt",input)
//        jsonObject.put("model", "text-davinci-003")
        jsonObject.put("n", 2)
        jsonObject.put("size", "1024x1024")
//        jsonObject.put("presence_penalty", 0.6)
//        jsonObject.put("model", "gpt-3.5-turbo")
// Request a string response from the provided URL.
        val stringRequest = object :JsonObjectRequest(
            Request.Method.POST, url,jsonObject,
            Response.Listener<JSONObject> { response ->
                // Display the first 500 characters of the response string.
                var jsonArray = response.getJSONArray("data")
                for (i in 0..jsonArray.length()-1){
                    var answer = jsonArray.getJSONObject(i).getString("url")
                    var message : Message = Message("m2","image",chatgpt,Calendar.getInstance().time,answer)
                    adapter.addToStart(message,true)
                }
            },
            Response.ErrorListener { })
        {
            override fun getHeaders(): MutableMap<String, String> {
                var map = HashMap<String,String>()
                map.put("Content-Type", "application/json")
                map.put("Authorization", "Bearer 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")

                return map
            }
        }

        stringRequest.setRetryPolicy(object : RetryPolicy{
            override fun getCurrentTimeout(): Int {
                return 60000
            }

            override fun getCurrentRetryCount(): Int {
                return 15
            }

            override fun retry(error: VolleyError?) {

            }

        })

// Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
}