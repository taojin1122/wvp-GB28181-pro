package com.genersoft.iot.vmp.media.zlm.dto.hook;

import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;

public class HookResultForOnPublish extends HookResult{

    private boolean enable_audio;
    private boolean enable_mp4;
    private int mp4_max_second;
    private String mp4_save_path;
    private String stream_replace;
    private Integer modify_stamp;

    public HookResultForOnPublish() {
    }

    public static HookResultForOnPublish SUCCESS(){
        return new HookResultForOnPublish(0, "success");
    }

    public static HookResultForOnPublish getInstance(ResultForOnPublish resultForOnPublish){
        HookResultForOnPublish successResult = new HookResultForOnPublish(0, "success");
        // 转协议时是否开启音频
        successResult.setEnable_audio(resultForOnPublish.isEnable_audio());
        // 是否允许mp4录制
        successResult.setEnable_mp4(resultForOnPublish.isEnable_mp4());
        // 该流是否开启时间戳覆盖(0:绝对时间戳/1:系统时间戳/2:相对时间戳)
        successResult.setModify_stamp(resultForOnPublish.getModify_stamp());
        // 是否修改流id, 通过此参数可以自定义流id(譬如替换ssrc)
        successResult.setStream_replace(resultForOnPublish.getStream_replace());
        // mp4录制切片大小，单位秒
        successResult.setMp4_max_second(resultForOnPublish.getMp4_max_second());
        // mp4录制文件保存根目录，置空使用默认
        successResult.setMp4_save_path(resultForOnPublish.getMp4_save_path());
        return successResult;
    }

    public HookResultForOnPublish(int code, String msg) {
        setCode(code);
        setMsg(msg);
    }

    public boolean isEnable_audio() {
        return enable_audio;
    }

    public void setEnable_audio(boolean enable_audio) {
        this.enable_audio = enable_audio;
    }

    public boolean isEnable_mp4() {
        return enable_mp4;
    }

    public void setEnable_mp4(boolean enable_mp4) {
        this.enable_mp4 = enable_mp4;
    }

    public int getMp4_max_second() {
        return mp4_max_second;
    }

    public void setMp4_max_second(int mp4_max_second) {
        this.mp4_max_second = mp4_max_second;
    }

    public String getMp4_save_path() {
        return mp4_save_path;
    }

    public void setMp4_save_path(String mp4_save_path) {
        this.mp4_save_path = mp4_save_path;
    }

    public String getStream_replace() {
        return stream_replace;
    }

    public void setStream_replace(String stream_replace) {
        this.stream_replace = stream_replace;
    }

    public Integer getModify_stamp() {
        return modify_stamp;
    }

    public void setModify_stamp(Integer modify_stamp) {
        this.modify_stamp = modify_stamp;
    }

    @Override
    public String toString() {
        return "HookResultForOnPublish{" +
                "enable_audio=" + enable_audio +
                ", enable_mp4=" + enable_mp4 +
                ", mp4_max_second=" + mp4_max_second +
                ", mp4_save_path='" + mp4_save_path + '\'' +
                ", stream_replace='" + stream_replace + '\'' +
                ", modify_stamp='" + modify_stamp + '\'' +
                '}';
    }
}
