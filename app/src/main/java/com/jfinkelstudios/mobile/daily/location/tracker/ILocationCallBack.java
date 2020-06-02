package com.jfinkelstudios.mobile.daily.location.tracker;

interface ILocationCallBack {
    void onLatLng(String lat, String lng);

    void onTime(String time);

}
