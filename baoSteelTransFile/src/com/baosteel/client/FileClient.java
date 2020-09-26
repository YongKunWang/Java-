package com.baosteel.client;

import com.baosteel.server.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileClient {

    private final int BUFFER_SIZE = 1024;
    // 主界面窗体
    private Frame main;
    // 第一行
    private Label labelIp;
    private TextField textFieldIp;
    private Button buttonIpTest;
    // 第二行
    private Label labelFile;
    private TextField textFieldFilePath;
    private Button buttonFileDis;
    private JFileChooser fileChooser;

    // 第三行 启动按钮
    private Button buttonLaunch;
    // 第四行
    private TextArea textArea;

    // 布局之外的成员变量
    private Socket client_socket;
    private PrintStream client_out;
    private BufferedReader client_in;

    private String ip;
    private int port;

    private File rootDir;

    private Map<String, String> imageMap = new HashMap<>();

    FileClient() {
        //1. 界面初始化操作
        initVariable();
        //2. 连接服务器
        //connectServer();
    }

    private void initVariable() {
        // 主界面布局
        main = new Frame("监控文件转移-Client");
        main.setBounds(300,300,400,340);
        main.setLayout(new FlowLayout());
        main.setResizable(false);

        // 第一行
        labelIp = new Label("待连接的网络地址：");
        textFieldIp = new TextField(20);
        textFieldIp.setText("127.0.0.1:20000");
        buttonIpTest = new Button("测试");

        main.add(labelIp);
        main.add(textFieldIp);
        main.add(buttonIpTest);

        // 第二行
        labelFile = new Label("待选择的文件路径：");
        textFieldFilePath = new TextField(20);
        buttonFileDis = new Button("浏览");
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        main.add(labelFile);
        main.add(textFieldFilePath);
        main.add(buttonFileDis);
        // 第三行 启动按钮
        buttonLaunch = new Button("监控文件转移开始");
        main.add(buttonLaunch);
        // 第四行
        textArea = new TextArea(11,44);
        main.add(textArea);
        myEvent();
        main.setVisible(true);
    }
    private void myEvent(){
        // 主窗体的关闭监控时间
        main.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        // 测试按键的监控时间
        buttonIpTest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("测试网络连接...");
                connectServer();
            }
        });
        // 浏览按键的监控事件
        buttonFileDis.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 打开文件选择器
                fileChooser.showOpenDialog(main);
                // 获得文件夹路径
                rootDir = fileChooser.getSelectedFile();
//                System.out.println(fileChooser.getSelectedFile());
                // 写入到文本框中
                textFieldFilePath.setText(rootDir.toString());
                // 显示文件数据到textArea中
                textArea.setText("");
                String[] names = rootDir.list();
                for (String name:names){
                    textArea.append(name + "\r\n");
                }
            }
        });
        // 监控文件启动按键的监控事件
        buttonLaunch.addActionListener(new ActionListener() {
            FindNewFile findNewFile = null;
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setText("");
                if (rootDir == null){
                    textArea.setText("未选择文件夹,请进行其他操作..");
                }
                else {
                    textArea.setText("您选择的文件夹为：" + rootDir.toString() + "\r\n");
                    if (findNewFile == null){
                        findNewFile = new FindNewFile(rootDir);
                        findNewFile.start();
                    }
                }
            }
        });
    }
    private class FindNewFile extends Thread {
        private File rootDir;
        FindNewFile(File rootDir){
            this.rootDir = rootDir;
        }
        public void run(){
            while (true){
                find();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        /**
         * 寻找到最新的文件 并且开启传输！
         */
        private void find(){
            File[] portNames = rootDir.listFiles();
            for (File portName :portNames){
                if (portName.getName().equals("10") || portName.getName().equals("26")){
                    File newDateDir = findNewDateDir(portName);
                    File newDateImage = findNewDateFile(newDateDir);
                    // 到这里，找见最新的文件了，现在的问题是持续检测新文件，当检测时间<< 文件生成间隔时，会导致检测的新文件一直是同一个文件
                    // 解决方案：
                    // 1. 创建map key = portName.getName; value = newDateImage.toString();
                    // 2. 初始状态为空，一轮检测后，新检测的文件放入到map中，然后直接上传
                    // 3. 当检测一轮以后，在开启一轮检测后，需要比对map中的key-value!

                    // map的格式说明：10: 2019-01-09/xxxx.jpg
                    if (!imageMap.containsKey(portName.getName())){
                        // 不存在port，直接存入并且开启文件上传！
//                        System.out.println("未存在port口，开启上传....");
                        sendFileInfo(newDateImage);
                        imageMap.put(portName.getName(), newDateImage.toString());
                    }else {
                        // port存在的话，需要进一步
                        if (imageMap.containsKey(portName.getName())){
//                            System.out.println("port口存在，进一步判断...");

                            if (imageMap.get(portName.getName()).equals(newDateImage.toString())){
//                                System.out.println("port口的最新文件为同一个");
                            }else {
                                if (!(imageMap.get(portName.getName()).equals(newDateImage.toString()))){
//                                    System.out.println("port口的最新文件不是同一个...");
//                                    System.out.println("开启上传...");
                                    sendFileInfo(newDateImage);
                                    imageMap.put(portName.getName(),newDateImage.toString());
                                }
                            }
                        }
                    }
                }
                // 每一次循环间隔1S作为过渡时间！
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 发送文件信息
         * @param newDateImage
         */
        private void sendFileInfo(File newDateImage){
            // 文件名进行拆解 为 path : file  eg: 10/2019-01-09 : xxx.jpg
            String[] names = newDateImage.toString().split("/");
            String path = names[names.length -3] + "/" + names[names.length-2];
            String file = names[names.length-1];
            client_out.println("@action=Upload[" + path + ":" + file + ":"+ "null]");
//            System.out.println(path + " : " + file);
        }
        /**
         * 寻找最新的文件夹
         * @param portName
         * @return
         */
        private File findNewDateDir(File portName){
            // 得到所有的日期文件！
            File[] dateNames = portName.listFiles();
            // 寻找最新的文件夹,根据本日的时间！
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String currentDate = format.format(date);
            currentDate = "2019-01-09";
//        System.out.println(newDirName);
            // 确定最新的文件存在！
            for (File dateName : dateNames) {
                if (currentDate.equals(dateName.getName())) {
//                System.out.println("寻找到最新的监控文件：" + currentDate);
                    return new File(portName.toString(), currentDate);
                }
            }
            return null;
        }

        /**
         * 寻找最新文件夹下最新的文件部分！
         * @param newDir
         * @return
         */
        private File findNewDateFile(File newDir){
            File[] images = newDir.listFiles();
            Arrays.sort(images, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return (int) (o2.lastModified() - o1.lastModified());
                }
            });
            File newImageFile = images[0];
            return newImageFile;
        }
    }

    /**
     * 连接服务器
     */
    private void connectServer() {
        this.ip = textFieldIp.getText().split(":")[0];
        this.port = Integer.parseInt(textFieldIp.getText().split(":")[1]);
        //连接服务器
        try {
            //初始化
            // 用来进行客户端与服务器之间命令的监听部分！
            client_socket = new Socket(ip, port);
            client_out = new PrintStream(client_socket.getOutputStream(), true);
            client_in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            client_out.println("@action=test");
            // 客户端监听来自于服务器端的消息
            new ClientThread().start();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            buttonIpTest.setEnabled(false);
        }
    }
    private  class ClientThread extends Thread{
        String fromServer_data = null;
        String uploadFilePath = null;
        String uploadFileName = null;
        String uploadFileResult = null;
        public void run(){
            try{

                while ((fromServer_data = client_in.readLine()) != null){
                    if (fromServer_data.startsWith("@action=test")){
                        textArea.setText(fromServer_data);
                    }
                    if (fromServer_data.startsWith("@action=Upload")){
                        uploadFilePath = Utils.getUpLoadFilePath(fromServer_data);
                        uploadFileName = Utils.getUpLoadFileName(fromServer_data);
                        uploadFileResult = Utils.getUpLoadFileResult(fromServer_data);
                        if (uploadFileResult.equals("OK")){
                            System.out.println("开启客户端文件上传线程...");
                            new HandleFileThread(uploadFilePath, uploadFileName).start();
                        }
                    }
                    System.out.println("客户端获得从服务器端来的消息: " + fromServer_data);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    class HandleFileThread extends Thread{
        private String uploadFilePath = null;
        private String uploadFileName = null;
        HandleFileThread(String uploadFilePath, String uploadFileName){
            this.uploadFilePath = uploadFilePath;
            this.uploadFileName = uploadFileName;
        }

        public void run(){
            File file = new File(rootDir + "/" + uploadFilePath + "/" + uploadFileName);
            Socket socket = null;
            BufferedInputStream bis = null;
//            System.out.println(rootDir + "/" + uploadFilePath + "/" + uploadFileName);
            try{
                socket = new Socket(ip, port+1);
                bis = new BufferedInputStream(new FileInputStream(file));
                BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                int len = 0;
                int i = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                System.out.println("开始上传文件--文件大小为：" + file.length());
                while ((len = bis.read(buffer)) != -1){
                    bos.write(buffer,0 ,len);
                    bos.flush();
                }
                socket.shutdownOutput();
                System.out.println("文件上传完成...");
            }catch (IOException e){
                e.printStackTrace();
            } finally {
                try{
                    if (socket != null){
                        socket.close();
                    }
                    if (bis != null){
                        bis.close();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        }
    }
    public static void main(String[] args) {
        new FileClient();
    }
}
