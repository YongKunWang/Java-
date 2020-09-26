package com.baosteel.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {
    private int BUFFER_SIZE = 1024;
    private int port;
    private ServerSocket serverSocket;
    private ServerSocket fileServerSocket;

    private String path = "/Users/homewang/Documents/img/save"; //路径名预定前缀

    FileServer(int port){
        this.port = port;
        try {
            //1. 初始化
            serverSocket = new ServerSocket(this.port);
            fileServerSocket = new ServerSocket(this.port+1);
            System.out.println("文件服务器启动，等待客户端连接...");
            //2. 每次接受一个客户端请求连接时，都会启用一个线程
            while (true){
                Socket client_socket = serverSocket.accept();
                System.out.println("客户端：" + client_socket.getRemoteSocketAddress() + "已经连接");
                new FileServerThread(client_socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理客户端文字请求的
     */
    private class FileServerThread extends Thread{
        private Socket client_socket;
        private BufferedReader server_in;
        private PrintWriter server_Out;

        FileServerThread(Socket client_socket){
            try {
                this.client_socket = client_socket;
                server_in = new BufferedReader(new InputStreamReader(this.client_socket.getInputStream()));
                server_Out = new PrintWriter(this.client_socket.getOutputStream(),true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run(){
            String fromClientData = null;
            String uploadFilePath = null;
            String uploadFileName = null;
            try{
                while ((fromClientData = server_in.readLine()) != null) {

                    if(fromClientData.startsWith("@action=test")){
                        server_Out.println("@action=test[OK]");
                    } else if (fromClientData.startsWith("@action=Upload")){
                        uploadFilePath = Utils.getUpLoadFilePath(fromClientData);
                        uploadFileName = Utils.getUpLoadFileName(fromClientData);
                        System.out.println(uploadFilePath + " ：" + uploadFileName);
                        server_Out.println("@action=Upload[" + uploadFilePath + ":" + uploadFileName +":OK]");
                        System.out.println("开启服务器端文件上传线程...");
                        new HandleFileThread(uploadFilePath,uploadFileName).start();
                    }else{
                        System.out.println("接收到的客户端数据：" + fromClientData);
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    class HandleFileThread extends Thread{
        private String uploadFilePath;
        private String uploadFileName;
        HandleFileThread(String uploadFilePath, String uploadFileName){
            this.uploadFilePath = uploadFilePath;
            this.uploadFileName = uploadFileName;
        }
        public void run(){
            BufferedInputStream file_in = null;
            File fileDir = new File(path + "/" + uploadFilePath);
            if (!fileDir.exists()){
                fileDir.mkdirs();
            }
            File file = new File(fileDir,uploadFileName);
            System.out.println(file.toString());
            Socket socket = null;
            BufferedOutputStream bos =null;
            try{
                socket = fileServerSocket.accept();
                file_in = new BufferedInputStream(socket.getInputStream());
                bos = new BufferedOutputStream(new FileOutputStream(file));

                int len = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((len = file_in.read(buffer)) != -1){
                    bos.write(buffer,0,len);
                    bos.flush();
                }
                System.out.println("文件上传完成...");
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                try{
                    if (socket!= null){
                        socket.close();
                    }
                    if (bos != null){
                        bos.close();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

        }
    }
    public static void main(String[] args) {
        new FileServer(20000);
    }
}
