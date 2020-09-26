package com.baosteel.server;

public class Utils {
    /**
     *
     *    上传文件消息格式： @action=Upload["filePath":"fileName":"result"]
     参数说明：
     *          filePath   要上传的文件的路径
     *          fileName   要上传的文件的名称
     *          fileSize   要上传的文件的大小
     */
    public static String getUpLoadFilePath(String data){
        return data.substring(data.indexOf("[")+1, data.indexOf(":"));
    }
    public static String getUpLoadFileName(String data){
        return data.substring(data.indexOf(":")+1, data.lastIndexOf(":"));
    }
    public static String getUpLoadFileResult(String data){
        return data.substring(data.lastIndexOf(":")+1, data.length() - 1);
    }
}
