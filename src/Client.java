
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by HeTingwei on 2017/12/9.
 * ���߳̿ͻ���
 */
public class Client {
    private Socket socket;
    private static final int MESSAGE_SIZE = 4096;//ÿ������������ݵ���󳤶�
    private InputStream in;
    private OutputStream out;
    private IMessageCallback mServerIMessageCallback;
    private IMessageCallback mClientIMessageCallback = new IMessageCallback() {

		@Override
		public void rece(byte[] data, int length) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void send(byte[] data, int length) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void disconnect() {
			// TODO Auto-generated method stub
			
		}
    	
    };

    public Client (IMessageCallback callback) {
    	this.mServerIMessageCallback = callback;
    }
    
    public IMessageCallback getClientCallback() {
    	return mClientIMessageCallback;
    }
    
    public void connectRemoteDesktopClient() {
        try {
            socket = new Socket("127.0.0.1", 3389);
            if (socket.isConnected() == true) {
                System.out.println("���ӳɹ�");
                new Thread() {//����һ���������ݵ��߳�
                    @Override
                    public void run() {
                        super.run();
                        
                        try {
                            in = socket.getInputStream();
                            byte[] b;
                            while (true) {
                                b = new byte[MESSAGE_SIZE];
                                int length = 0;
                                if ((length = in.read(b)) != -1) {
                                	mServerIMessageCallback.send(b, length);
                                } else {
                                	mServerIMessageCallback.disconnect();
                                }
                                System.out.println("���յ��������Ϣ��" + new String(b, "UTF-8"));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }.start();
                new Thread() {//����һ���������ݵ��߳�
                    @Override
                    public void run() {
                        super.run();
                        try {
                            out = socket.getOutputStream();
                            while (true) {
                                
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }.start();
            }
            OutputStream out = null;
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String str = scanner.nextLine();
                out = socket.getOutputStream();
                out.write(str.getBytes());
                out.flush();
                if (str.equals("end")) {
                    System.exit(0);//�رտͻ���
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
