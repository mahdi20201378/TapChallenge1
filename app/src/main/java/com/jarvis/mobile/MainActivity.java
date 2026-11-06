package com.jarvis.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.content.pm.PackageManager;
import android.Manifest;
import android.graphics.Color;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.util.*;
import java.util.concurrent.*;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout root, messages;
    private EditText prompt, keyInput;
    private TextView status;
    private Button sendButton, voiceButton;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private ExecutorService worker = Executors.newSingleThreadExecutor();
    private String pendingSpeech = "";
    private static final String PREFS = "jarvis_secure";
    private static final String ALIAS = "jarvis_gemini_key";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8,18,32));
        getWindow().setNavigationBarColor(Color.rgb(8,18,32));
        buildUi();
        tts = new TextToSpeech(this, result -> { if (result == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US); });
        if (SpeechRecognizer.isRecognitionAvailable(this)) recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        addMessage("JARVIS", "Hello. I am ready. Add your own Gemini API key to begin.");
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(12)); root.setBackgroundColor(Color.rgb(8,18,32));
        setContentView(root);
        TextView title=label("J.A.R.V.I.S",28,Color.rgb(88,210,255)); title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView subtitle=label("PERSONAL AI ASSISTANT",11,Color.LTGRAY); subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);
        keyInput=new EditText(this); keyInput.setSingleLine(true); keyInput.setTextColor(Color.WHITE);
        keyInput.setHintTextColor(Color.GRAY); keyInput.setHint("Your Gemini API key"); keyInput.setInputType(129);
        root.addView(keyInput,new LinearLayout.LayoutParams(-1,dp(52)));
        Button save=new Button(this); save.setText("Save API key securely"); root.addView(save);
        save.setOnClickListener(v->{try { saveKey(keyInput.getText().toString().trim()); keyInput.setText(""); status.setText("API key saved on this device.");} catch(Exception e){status.setText("Could not save API key: "+e.getMessage());}});
        status=label("Waiting for API key",12,Color.LTGRAY); root.addView(status);
        ScrollView scroll=new ScrollView(this); messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1); sp.topMargin=dp(12); sp.bottomMargin=dp(10); root.addView(scroll,sp);
        prompt=new EditText(this); prompt.setTextColor(Color.WHITE); prompt.setHintTextColor(Color.GRAY); prompt.setHint("Ask JARVIS...");
        root.addView(prompt,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        sendButton=new Button(this); sendButton.setText("Send"); voiceButton=new Button(this); voiceButton.setText("Speak");
        actions.addView(sendButton,new LinearLayout.LayoutParams(0,dp(52),1)); actions.addView(voiceButton,new LinearLayout.LayoutParams(0,dp(52),1));
        root.addView(actions);
        sendButton.setOnClickListener(v->sendPrompt());
        voiceButton.setOnClickListener(v->startListening());
    }
    private void addMessage(String who,String text) {
        TextView t=label(who+"\n"+text,15,Color.WHITE); t.setPadding(dp(12),dp(10),dp(12),dp(10));
        t.setBackgroundColor(who.equals("JARVIS")?Color.rgb(19,49,68):Color.rgb(30,39,56));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(8); messages.addView(t,p);
    }
    private void sendPrompt() {
        String q=prompt.getText().toString().trim(); if(q.isEmpty()) return;
        prompt.setText(""); ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(prompt.getWindowToken(),0);
        addMessage("YOU",q); sendButton.setEnabled(false); status.setText("JARVIS is thinking...");
        worker.execute(()->{
            String answer;
            try { answer=askGemini(q); } catch(Exception e) { answer="Request failed: "+e.getMessage(); }
            final String result=answer;
            runOnUiThread(()->{addMessage("JARVIS",result); status.setText("Ready"); sendButton.setEnabled(true); if(tts!=null) tts.speak(result,TextToSpeech.QUEUE_FLUSH,null,"jarvis_reply");});
        });
    }
    private String askGemini(String question) throws Exception {
        String key=loadKey(); if(key==null || key.isEmpty()) throw new Exception("Please save your Gemini API key first.");
        URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="+URLEncoder.encode(key,"UTF-8"));
        HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(60000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json");
        JSONObject part=new JSONObject().put("text",question);
        JSONObject body=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(part))));
        try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode(); InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        ByteArrayOutputStream buffer=new ByteArrayOutputStream(); byte[] b=new byte[4096]; int n; while((n=stream.read(b))!=-1) buffer.write(b,0,n);
        String raw=buffer.toString("UTF-8"); c.disconnect();
        JSONObject response=new JSONObject(raw);
        if(code<200||code>=300) throw new Exception(response.optJSONObject("error")!=null?response.getJSONObject("error").optString("message","HTTP "+code):"HTTP "+code);
        return response.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text","No text response.");
    }
    private void startListening() {
        if(recognizer==null){status.setText("Speech recognition is unavailable on this device.");return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},22);return;}
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){status.setText("Listening...");}
            public void onBeginningOfSpeech(){} public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){status.setText("Processing voice...");}
            public void onError(int e){status.setText("Voice input error: "+e);}
            public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){prompt.setText(r.get(0));sendPrompt();}}
            public void onPartialResults(Bundle b){} public void onEvent(int t,Bundle b){}
        });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault()); recognizer.startListening(i);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==22&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startListening();}
    private SecretKey getSecretKey() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if(ks.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
        KeyGenerator gen=KeyGenerator.getInstance("AES","AndroidKeyStore");
        gen.init(new android.security.keystore.KeyGenParameterSpec.Builder(ALIAS,android.security.keystore.KeyProperties.PURPOSE_ENCRYPT|android.security.keystore.KeyProperties.PURPOSE_DECRYPT).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
        return gen.generateKey();
    }
    private void saveKey(String value) throws Exception {
        if(value.isEmpty()) throw new Exception("API key is empty");
        byte[] iv=new byte[12]; new SecureRandom().nextBytes(iv); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,getSecretKey(),new GCMParameterSpec(128,iv)); byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        getSharedPreferences(PREFS,0).edit().putString("iv",Base64.encodeToString(iv,Base64.NO_WRAP)).putString("data",Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply();
    }
    private String loadKey() throws Exception {
        android.content.SharedPreferences p=getSharedPreferences(PREFS,0); String ivs=p.getString("iv",null), data=p.getString("data",null); if(ivs==null||data==null)return null;
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,getSecretKey(),new GCMParameterSpec(128,Base64.decode(ivs,Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(data,Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    @Override protected void onDestroy(){if(recognizer!=null)recognizer.destroy();if(tts!=null)tts.shutdown();worker.shutdownNow();super.onDestroy();}
}
