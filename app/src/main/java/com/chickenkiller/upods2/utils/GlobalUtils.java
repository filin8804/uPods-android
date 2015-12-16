package com.chickenkiller.upods2.utils;

import android.content.Context;
import android.net.ConnectivityManager;

import com.chickenkiller.upods2.controllers.app.UpodsApplication;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alonzilberman on 8/14/15.
 */
public class GlobalUtils {

    public static String parserDateToUS(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z");
            Date inputDate = dateFormat.parse(date);
            dateFormat = new SimpleDateFormat("MMM dd,yyyy hh:mm a");
            date = dateFormat.format(inputDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String parserDateToMonth(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z");
            Date inputDate = dateFormat.parse(date);
            dateFormat = new SimpleDateFormat("MMM\ndd");
            date = dateFormat.format(inputDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String getCleanFileName(String str) {
        str = str.toLowerCase();
        return str.replaceAll("[^a-zA-Z0-9]+", "_");
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static boolean isInternetConnected() {
        ConnectivityManager cm = (ConnectivityManager) UpodsApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    /**
     * Returns a list with all links contained in the input
     */
    public static List<String> extractUrls(String text) {
        String lines[] = text.split("\\s+");
        List<String> containedUrls = new ArrayList<String>();
        String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher urlMatcher = pattern.matcher(line);
            while (urlMatcher.find()) {
                containedUrls.add(line.substring(urlMatcher.start(0), urlMatcher.end(0)));
            }
        }
        return containedUrls;
    }

    public static boolean isUrlReachable(String urlToCheck) {
        try {
            URL url = new URL(urlToCheck);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(1000);
            int code = connection.getResponseCode();
            if (code == 200) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
