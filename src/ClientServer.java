
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by HeTingwei on 2017/12/9.
 * 多线程服务器,实现多客户端聊天
 */
public class ClientServer {

    List<ReceiveThread> receiveList = new ArrayList<>();//存放已连接客户端类
    int MESSAGE_SIZE = 1024;//每次允许接受数据的最大长度
    int num = 0;//客户端编号
    int localport = 19909;

    /*public static void main(String[] args) {
    	ClientServer mServer = new ClientServer();
    }*/

    public ClientServer() {
    	ServerHandle mServerHandle = new ServerHandle();
    	mServerHandle.start();
    }
    
    class ServerHandle extends Thread {
    	
    	ServerSocket serverSocket = null;
    	
    	public ServerHandle() {
    		
    	}
    	
    	@Override
        public void run() {
    		super.run();
            try {
                serverSocket = new ServerSocket(localport);//用来监听的套接字，指定端口号
                while (true) {
                    Socket socket = serverSocket.accept();//监听客户端连接，阻塞线程
                    System.out.println("连接上客户端：" + num);
                    //在其他线程处理接收来自客户端的消息
                    ReceiveThread receiveThread = new ReceiveThread(socket, num);
                    receiveThread.start();
                    receiveList.add(receiveThread);

                    //有客户端新上线，服务器就通知其他客户端
                    /*String notice="有新客户端上线，现在在线客户端有：客户端:";
                    for (ReceiveThread thread : receiveList) {
                        notice = notice + "" + thread.num;
                    }
                    for (ReceiveThread thread : receiveList) {
                        new SendThread(thread.socket, notice).start();
                    }*/
                    num++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    

    class ReceiveThread extends Thread {
        int num;
        Socket socket;//客户端对应的套接字
        boolean continueReceive = true;//标识是否还维持连接需要接收
        boolean hasDesk = false;
        boolean deskrunning = false;
        Socket desksocket;
        DeskSendThread mDeskSendThread = null;
        DeskReceiveThread mDeskReceiveThread = null;

        public ReceiveThread(Socket socket, int num) {
            this.socket = socket;
            this.num = num;
            try {
                //给连接上的客户端发送，分配的客户端编号的通知
                socket.getOutputStream().write(("你的客户端编号是" + num).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stoprun() {
        	continueReceive = false;
        	if (mDeskSendThread != null) {
        		mDeskSendThread.stoprun();
        	}
        	if (mDeskReceiveThread != null) {
        		mDeskReceiveThread.stoprun();
        	}
        }
        
        @Override
        public void run() {
            super.run();
            //接收客户端发送的消息
            try {
                byte[] b = new byte[MESSAGE_SIZE];
                int num = -1;
                while (continueReceive) {
                	if (desksocket == null) {
                		desksocket = new Socket("127.0.0.1", 3389);
                		hasDesk = true;
                		mDeskSendThread = new DeskSendThread(desksocket, socket);
                		mDeskReceiveThread = new DeskReceiveThread(desksocket, socket);
                		mDeskReceiveThread.start();
                		mDeskSendThread.start();
                	}
                	if (!desksocket.isConnected() || !socket.isConnected()) {
                		stoprun();
                		receiveList.remove(this);
                	}
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {//关闭资源
                    if (socket != null) {
                        socket.close();
                    }
                    if (desksocket != null) {
                    	desksocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private class DeskSendThread extends Thread {
        Socket mdesksocket;
        Socket remotesocket;
        boolean running = true;
        InputStream remotein;
        OutputStream deskout;

        public DeskSendThread(Socket desksocket, Socket remotesocket) {
            this.mdesksocket = desksocket;
            this.remotesocket = remotesocket;
        }

        public void stoprun() {
        	running = false;
        }
        
        @Override
        public void run() {
            super.run();
            try {
				remotein = remotesocket.getInputStream();
				deskout = mdesksocket.getOutputStream();
	            byte[] buff = new byte[MESSAGE_SIZE];
	            int num = -1;
	            while (running) {
	            	if ((num = remotein.read(buff)) != -1) {
	            		deskout.write(buff, 0, num);
	            		//deskout.flush();
	            	} else {
	            		stoprun();
	            	}
	            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
                try {//关闭资源
                    if (remotein != null) {
                    	remotein.close();
                    }
                    if (deskout != null) {
                    	deskout.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    class DeskReceiveThread extends Thread {
        Socket mdesksocket;
        Socket remotesocket;
        boolean running = true;
        InputStream deskin;
        OutputStream remoteout;

        public DeskReceiveThread(Socket desksocket, Socket remotesocket) {
            this.mdesksocket = desksocket;
            this.remotesocket = remotesocket;
        }

        public void stoprun() {
        	running = false;
        }
        
        @Override
        public void run() {
            super.run();
            try {
            	deskin = mdesksocket.getInputStream();
            	remoteout = remotesocket.getOutputStream();
	            byte[] buff = new byte[MESSAGE_SIZE];
	            int num = -1;
	            while (running) {
	            	if ((num = deskin.read(buff)) != -1) {
	            		remoteout.write(buff, 0, num);
	            		//deskout.flush();
	            	} else {
	            		stoprun();
	            	}
	            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
                try {//关闭资源
                    if (deskin != null) {
                    	deskin.close();
                    }
                    if (remoteout != null) {
                    	remoteout.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    //发送消息的线程
    class SendThread extends Thread {
        Socket socket;
        String str;

        public SendThread(Socket socket, String str) {
            this.socket = socket;
            this.str = str;
        }

        @Override
        public void run() {
            super.run();
            try {
                socket.getOutputStream().write(str.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
