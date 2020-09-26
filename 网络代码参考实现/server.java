public class FileServer {
    private final int port = 5203;
    private ServerSocket server_socket;   //普通消息传输服务器套接字
    private ServerSocket fileServerSocket;  // 文件传输服务器套接字

    private String path ="文件服务器/GroupFile"; //服务器文件保存路径

    public FileServer() {
        try {
            //1-初始化
            server_socket = new ServerSocket(this.port);   // 创建消息传输服务器
            fileServerSocket = new ServerSocket(8888);  //创建文件传输服务器

              System.out.println("文件服务器启动等待客户端连接.....");
            //2-每次接收一个客户端请求连接时都启用一个线程处理
            while(true) {
                Socket client_socket = server_socket.accept();
                 System.out.println("客户端:"+client_socket.getRemoteSocketAddress()+"已连接");
                new ServerThread(client_socket).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** -----------------------------------------------------------------------------------------------------
     *
     *   启用一个线程处理客户端的请求
     */
    private class ServerThread extends Thread{
        private Socket client_socket;
        private BufferedReader server_in;
        private PrintWriter server_out;

        public ServerThread(Socket client_socket) {
            try {
                //初始化
                this.client_socket = client_socket;
                server_in = new BufferedReader(new InputStreamReader(this.client_socket.getInputStream()));
                server_out = new PrintWriter(this.client_socket.getOutputStream(),true);


            } catch (IOException e) {
            }
        }

        public void run() {
            try {
                String uploadFileName = null;
                String uploadFileSize = null;
                String fromClientData ;

                while((fromClientData = server_in.readLine()) != null){
                        //把服务器文件列表返回
                        if(fromClientData.startsWith("@action=loadFileList")){
                            File dir = new File(path);
                            if (dir.isDirectory()){
                                String[] list = dir.list();
                                String filelist = "@action=GroupFileList[";
                                for (int i = 0; i < list.length; i++) {
                                    if (i == list.length-1){
                                        filelist  = filelist + list[i]+"]";
                                    }else {
                                        filelist  = filelist + list[i]+":";
                                    }
                                }
                                server_out.println(filelist);
                            }
                        }

                        //请求上传文件
                        if (fromClientData.startsWith("@action=Upload")){
                            uploadFileName = ParseDataUtil.getUploadFileName(fromClientData);
                            uploadFileSize = ParseDataUtil.getUploadFileSize(fromClientData);
                            File file = new File(path,uploadFileName);
                            //文件是否已存在
                            if (file.exists()){
                                //文件已经存在无需上传
                                server_out.println("@action=Upload[null:null:NO]");
                            }else {
                                //通知客户端开可以上传文件
                                server_out.println("@action=Upload["+uploadFileName+":"+uploadFileSize+":YES]");
                                //开启新线程上传文件
                                new HandleFileThread(1,uploadFileName,uploadFileSize).start();
                            }
                        }

                        //请求下载文件
                        if(fromClientData.startsWith("@action=Download")){
                            String fileName = ParseDataUtil.getDownFileName(fromClientData);
                            File file = new File(path,fileName);
                            if(!file.exists()){
                                server_out.println("@action=Download[null:null:文件不存在]");
                            }else {
                                //通知客户端开可以下载文件
                                server_out.println("@action=Download["+file.getName()+":"+file.length()+":OK]");
                                //开启新线程处理下载文件
                                new HandleFileThread(0,file.getName(),file.length()+"").start();
                                
                            }
                        }
                    }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         *     文件传输线程
         */
        class HandleFileThread extends Thread{
            private String filename;
            private String filesize;
            private int mode;  //文件传输模式

            public HandleFileThread(int mode,String name,String size){

                    filename = name;
                    filesize = size;
                    this.mode = mode;
            }

            public void run() {
                try {
                    Socket socket = fileServerSocket.accept();
                    //上传文件模式
                    if(mode == 1){
                        //开始接收上传
                        BufferedInputStream file_in = new BufferedInputStream(socket.getInputStream());
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(path,filename)));

                        int len;
                        byte[] arr = new byte[8192];

                        while ((len = file_in.read(arr)) != -1){
                            bos.write(arr,0,len);
                            bos.flush();
                        }
                        server_out.println("@action=Upload[null:null:上传完成]");
                        server_out.println("\n");
                        bos.close();
                    }

                    //下载文件模式
                    if(mode == 0){
                       BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(path,filename)));
                       BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                       System.out.println("服务器：开始下载");
                       int len;
                       byte[] arr =new byte[8192];
                       while((len = bis.read(arr)) != -1){
                           bos.write(arr,0,len);
                           bos.flush();
                       }

                       socket.shutdownOutput();
                    }
 					
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
	//启动程序
    public static void main(String[] args) {
        new FileServer();
    }
}
/Users/homewang/Downloads/bs-upload/src/main/java/com/baosteel/server.java