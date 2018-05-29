package org.zywx.wbpalmstar.plugin.uexvideo.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.zywx.wbpalmstar.plugin.uexvideo.vo.Lyric;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Administrator on 2018/4/27.
 */

public class JsonUtils {
    public static <T> List<T> json2List(String lyrics, Class<T[]> clazz) {
        Gson gson = new Gson();
        T[] array = gson.fromJson(lyrics, clazz);
        return Arrays.asList(array);
    }

    public static <T> ArrayList<T> jsonToArrayList(String json, Class<T> clazz) {
        Type type = new TypeToken<ArrayList<JsonObject>>() {
        }.getType();
        ArrayList<JsonObject> jsonObjects = new Gson().fromJson(json, type);

        ArrayList<T> arrayList = new ArrayList<>();
        for (JsonObject jsonObject : jsonObjects) {
            arrayList.add(new Gson().fromJson(jsonObject, clazz));
        }
        return arrayList;
    }

    public static void main(String[] args) {
        String lyrics = "\"[\n" +
                "                        {\n" +
                "                            \"lyricStartTime\": \"7.56\",\n" +
                "                            \"lyricEndTime\": \"8.49\",\n" +
                "                            \"lyricChineseText\": \"\",\n" +
                "                            \"lyricEnglishText\": \"Good morning.\",\n" +
                "                            \"lyricUuidProperty\": \"b1131082f4f8486fa70cd6c1ad5aa899\",\n" +
                "                            \"recordAble\": \"true\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"lyricStartTime\": \"9.74\",\n" +
                "                            \"lyricEndTime\": \"11.58\",\n" +
                "                            \"lyricChineseText\": \"\",\n" +
                "                            \"lyricEnglishText\": \"Good morning, Bobby.\",\n" +
                "                            \"lyricUuidProperty\": \"7e4f6e965dca4b728b73bb710171e17c\",\n" +
                "                            \"recordAble\": \"true\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"lyricStartTime\": \"15.18\",\n" +
                "                            \"lyricEndTime\": \"16.76\",\n" +
                "                            \"lyricChineseText\": \"\",\n" +
                "                            \"lyricEnglishText\": \"Good morning, Sam.\",\n" +
                "                            \"lyricUuidProperty\": \"60d582cae07d4beababa867f0cead165\",\n" +
                "                            \"recordAble\": \"true\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"lyricStartTime\": \"17.83\",\n" +
                "                            \"lyricEndTime\": \"18.84\",\n" +
                "                            \"lyricChineseText\": \"\",\n" +
                "                            \"lyricEnglishText\": \"Good morning.\",\n" +
                "                            \"lyricUuidProperty\": \"aea434fcf3fb445d9ce7e7fb759ed2a4\",\n" +
                "                            \"recordAble\": \"true\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"lyricStartTime\": \"24.16\",\n" +
                "                            \"lyricEndTime\": \"26.03\",\n" +
                "                            \"lyricChineseText\": \"\",\n" +
                "                            \"lyricEnglishText\": \">Hi, I'm Bobby.\",\n" +
                "                            \"lyricUuidProperty\": \"70c00d6f75a844c5a49d50722308b0ee\",\n" +
                "                            \"recordAble\": \"true\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"lyricStartTime\": \"27.23\",\n" +
                "                            \"lyricEndTime\": \"29.16\",\n" +
                "                            \"lyricChineseText\": \"\",\n" +
                "                            \"lyricEnglishText\": \">Hi, I'm Sam.\",\n" +
                "                            \"lyricUuidProperty\": \"aaa9e86f46224bc7af6ed9cbe8843afc\",\n" +
                "                            \"recordAble\": \"true\"\n" +
                "                        }\n" +
                "                    ]\"";
        List<Lyric> lyricList = json2List(lyrics, Lyric[].class);

        for (Lyric ly : lyricList) {
            System.out.println(ly.getLyricEnglishText());
        }
    }

}
