package com.example.mastermind.notification;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.nex3z.notificationbadge.NotificationBadge;
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private int MY_PERMISSION = 1000;
    private BubblesManager bubblesManager;
    private NotificationBadge mBadge;
    private int count;

    String max;
    Date offerFromDB;
    Date firstOffer;

    EditText edtxt_ratio;
    private TextView txtv_error;

    Context context = this.getContext();

    private SharedPreferences timePrefs;

    NotificationCompat.Builder notification;
    private static final int uniqueID = 45612;

    private PendingIntent pendingIntentA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT>=23){
            if(!Settings.canDrawOverlays(MainActivity.this)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
                startActivityForResult(intent,MY_PERMISSION);
            }
        }
        else{
            Intent intent = new Intent(MainActivity.this, Service.class);
            startService(intent);
        }

        edtxt_ratio = findViewById(R.id.edtxt_ratio);
        txtv_error = findViewById(R.id.txtv_error);

        initBubble();

        timePrefs = getSharedPreferences("lastupdate", MODE_PRIVATE);
        Long LatestUpdate = timePrefs.getLong("lastupdate", 0);
        System.out.println(timePrefs.getLong("lastupdate", 0) + " beginning");
        if (!timePrefs.contains("lastupdate") || timePrefs.getLong("lastupdate",0) == 0) {
            timePrefs.edit().clear().commit();
            firstOffer = new Date(115, 01, 01, 01, 01, 00);
            timePrefs.edit().putLong("lastupdate", firstOffer.getTime()).commit();
        }

        String url ="http://10.0.2.2/android/seminars.php?";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        System.out.println(response.toString());
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            max = jsonObject.getString("maxcreate");
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            offerFromDB=format.parse(max);
                            if(isConn()) {
                                start(offerFromDB);
                                txtv_error.setVisibility(View.INVISIBLE);
                            }
                            else{
                                cancel();
                                txtv_error.setText("Error: Not Connected");
                                txtv_error.setVisibility(View.VISIBLE);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        })
        {
            protected Map<String,String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("action", "showlatestoffer");
                params.put("userid", "1");
                return params;
            }
        };
        MySingleton.getInstance(this).addToRequestQueue(stringRequest);

        count = getIntent().getIntExtra("notificationCount",0);
        addNewBubble(count);
    }

    public boolean isConn(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isWifiConn = networkInfo.isConnected();
        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isMobileConn = networkInfo.isConnected();
        Log.d("connection", "Wifi connected: " + isWifiConn);
        Log.d("connection", "Mobile connected: " + isMobileConn);
        return isWifiConn || isMobileConn;
    }

    public void start(Date date) {

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(MainActivity.this, AlarmReceiver.class);
        alarmIntent.putExtra("datefromdb",date);
        pendingIntentA = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 600, pendingIntentA);

        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
    }

    public void cancel() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntentA);
        Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
    }
    public Context getContext(){
        return context;
    }

    private void initBubble(){
        bubblesManager = new BubblesManager.Builder(this)
                .setTrashLayout(R.layout.bubble_remove)
                .setInitializationCallback(new OnInitializedCallback() {
                    @Override
                    public void onInitialized() {
                        count=0;
                    }
                }).build();
        bubblesManager.initialize();
    }

    private void addNewBubble(int count){
        BubbleLayout bubbleView = (BubbleLayout) LayoutInflater.from(this)
                .inflate(R.layout.bubble_layout,null);
        mBadge = (NotificationBadge) bubbleView.findViewById(R.id.badge);
        mBadge.setNumber(count);

        bubbleView.setOnBubbleRemoveListener(new BubbleLayout.OnBubbleRemoveListener() {
            @Override
            public void onBubbleRemoved(BubbleLayout bubble) {
                Toast.makeText(MainActivity.this, "Removed", Toast.LENGTH_SHORT).show();
            }
        });

        bubbleView.setOnBubbleClickListener(new BubbleLayout.OnBubbleClickListener() {
            @Override
            public void onBubbleClick(BubbleLayout bubble) {
                Toast.makeText(MainActivity.this,"Clicked",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this,MainActivity.class);
                startActivity(intent);

            }
        });


        bubblesManager.addBubble(bubbleView,60,20);


    }
}

