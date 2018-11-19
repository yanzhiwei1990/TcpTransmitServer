
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by HeTingwei on 2017/12/9.
 * ���̷߳�����,ʵ�ֶ�ͻ�������
 */
public class ClientServer {

    List<ReceiveThread> receiveList = new ArrayList<>();//��������ӿͻ�����
    int MESSAGE_SIZE = 1024;//ÿ������������ݵ���󳤶�
    int num = 0;//�ͻ��˱��
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
                serverSocket = new ServerSocket(localport);//�����������׽��֣�ָ���˿ں�
                while (true) {
                    Socket socket = serverSocket.accept();//�����ͻ������ӣ������߳�
                    System.out.println("�����Ͽͻ��ˣ�" + num);
                    //�������̴߳���������Կͻ��˵���Ϣ
                    ReceiveThread receiveThread = new ReceiveThread(socket, num);
                    receiveThread.start();
                    receiveList.add(receiveThread);

                    //�пͻ��������ߣ���������֪ͨ�����ͻ���
                    /*String notice="���¿ͻ������ߣ��������߿ͻ����У��ͻ���:";
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
        Socket socket;//�ͻ��˶�Ӧ���׽���
        boolean continueReceive = true;//��ʶ�Ƿ�ά��������Ҫ����
        boolean hasDesk = false;
        boolean deskrunning = false;
        Socket desksocket;
        DeskSendThread mDeskSendThread = null;
        DeskReceiveThread mDeskReceiveThread = null;

        public ReceiveThread(Socket socket, int num) {
            this.socket = socket;
            this.num = num;
            try {
                //�������ϵĿͻ��˷��ͣ�����Ŀͻ��˱�ŵ�֪ͨ
                socket.getOutputStream().write(("��Ŀͻ��˱����" + num).getBytes());
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
            //���տͻ��˷��͵���Ϣ
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
                try {//�ر���Դ
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
                try {//�ر���Դ
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
                try {//�ر���Դ
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
    
    //������Ϣ���߳�
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
