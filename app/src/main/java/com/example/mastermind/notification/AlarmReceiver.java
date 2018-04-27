package com.example.mastermind.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
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
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by mastermind on 4/4/2018.
 */

public class AlarmReceiver extends BroadcastReceiver {


    Date offerDB;
    ArrayList<Date> dates = new ArrayList();
    NotificationCompat.Builder notification;
    private static final int uniqueID = 45612;
    Date currentTime = new Date();
    Date lastUpdate;
    int notCount;
    SharedPreferences timePrefs;
    SharedPreferences.Editor editor;


    @Override
    public void onReceive(final Context context, final Intent intent) {

        timePrefs = context.getSharedPreferences("lastupdate", MODE_PRIVATE);
        editor = timePrefs.edit();
        notCount = 0;


        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://10.0.2.2/android/seminars.php?";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        System.out.println(response.toString());
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            System.out.println(jsonObject.toString());
                            JSONArray jsonArray = jsonObject.getJSONArray("offers");
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObjectOffers = jsonArray.getJSONObject(i);

                                Date d = format.parse(String.valueOf(jsonObjectOffers.getString("creationtime")));
                                dates.add(d);

                            }
                            if (checkForOffers(context, intent) > 0) {
                                Intent intentBackToMain = new Intent(context, MainActivity.class);
                                intentBackToMain.putExtra("notificationCount", checkForOffers(context, intent));
                                context.startActivity(intentBackToMain);
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
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("action", "showoffers");
                params.put("userid", "1");
                return params;
            }
        };
        queue.add(stringRequest);


        offerDB = (Date) intent.getSerializableExtra("datefromdb");
        System.out.println(offerDB.toString() + "from the intent");


        notification = new NotificationCompat.Builder(context, "notification");
        notification.setAutoCancel(true);

        notification.setSmallIcon(R.drawable.newlauncher);
        notification.setTicker("This is the ticker");
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("Here is the title");
        notification.setCategory(NotificationCompat.CATEGORY_REMINDER);
        notification.setDefaults(Notification.DEFAULT_ALL);
        notification.setPriority(Notification.PRIORITY_MAX);
        notification.setVibrate(new long[0]);
        notification.setBadgeIconType(NotificationCompat.BADGE_ICON_NONE);


    }

    public int checkForOffers(Context context, Intent intent) {
        if (offerDB.getTime() > timePrefs.getLong("lastupdate", 0)) {
            lastUpdate = new Date(timePrefs.getLong("lastupdate", 0));
            System.out.println(offerDB.toString() + "max offer from db");
            System.out.println(lastUpdate.toString() + "max from preferences");
            for (Date d : dates) {
                System.out.println("last    " + lastUpdate.getTime() + " " + lastUpdate.toString());
                System.out.println("current " + currentTime.getTime() + " " + currentTime.toString());
                System.out.println("offer   " + d.getTime() + " " + d.toString());
                if (currentTime.after(d) && lastUpdate.before(d)) {
                    notCount++;
                    lastUpdate = d;
                }
            }


            editor.putLong("lastupdate", lastUpdate.getTime()).apply();
            System.out.println(timePrefs.getLong("lastupdate", 0) + " at the end of alarmreceiver ");

        }
        return notCount;
    }
}

