
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientServer {

    int MESSAGE_SIZE = 4096;//每次允许接受数据的最大长度
    int num = 0;//客户端编号
    int remoteport = 19909;
    int desk3389port = 19908;//3389;
    
    Socket clientsocket = null;
    Socket desk3389socket = null;
    InputStream clientin = null;
    InputStream desk3389in = null;
    OutputStream clientout = null;
    OutputStream desk3389out = null;
    ClientToDeskThread clientthread = null;
    Desk3389ToClientThread desk3389thread = null;

    public static void main(String[] args) {
    	ClientServer mServer = new ClientServer();
    }

    public ClientServer() {
    	ServerHandle mServerHandle = new ServerHandle();
    	mServerHandle.start();
    }
    
    class ServerHandle extends Thread {
    	
    	public ServerHandle() {
    		
    	}
    	
    	@Override
        public void run() {
    		super.run();
    		clientthread = new ClientToDeskThread();
            clientthread.start();
        }
    }
    
  //from client to desk
    private class ClientToDeskThread extends Thread {

        boolean running = true;
        boolean connected = false;

        public ClientToDeskThread() {
        	
        }
        
        public void stoprun() {
        	running = false;
        	try {
        		if (clientin != null) {
        			clientin.close();
        		}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	try {
        		if (clientout != null) {
        			clientout.close();
        			
        		}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	try {
        		if (clientsocket != null) {
        			clientsocket.close();
    			}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	clientsocket = null;
        	clientin = null;
        	clientout = null;
        	desk3389thread.stoprun();
        	run();
        }
        
        @Override
        public void run() {
            super.run();
            try {
            	clientsocket = new Socket("opendiylib.com", remoteport);//用来监听的套接字，指定端口号
            	clientin = clientsocket.getInputStream();
            	clientout = clientsocket.getOutputStream();
            	
            	clientout.write("type:desktop-command:connect-password:qwertyuiopasdfghjklzxcvbnm-".getBytes("UTF-8"));
            	byte[] buff = new byte[MESSAGE_SIZE];
            	byte[] command = new byte[128];
	            int num = -1;
	            String receivemsg = null;
	            while (running) {
	            	if ((num = clientin.read(buff)) != -1) {
	            		Arrays.fill(command, (byte)(0));
            			System.arraycopy(buff, 0, command, 0, num > command.length ? command.length : num);
            			receivemsg = new String(command, "UTF-8");
            			receivemsg = (receivemsg != null ? receivemsg.substring(0, num > command.length ? command.length : num) : null);
            			System.out.println("ClientToDeskThread receivemsg = " + receivemsg);
            			if (desk3389socket == null) {
            				if (receivemsg != null) {
            					String[] result = receivemsg.split("-");
                    			if (result != null && result.length >= 3) {
                    				String type_temp = null;
                    				String command_temp = null;
                    				String passward_temp = null;
                    				int length = ((result.length > 3) ? 3 : result.length);
                    				for (int i = 0; i < length; i++) {
                    					if (i == 0) {
                    						type_temp = result[0].substring(5);
                    					} else if (i == 1) {
                    						command_temp = result[1].substring(8);
                    					} else if (i == 2) {
                    						passward_temp = result[2].substring(9);
                    					}
                    				}
                    				if ("qwertyuiopasdfghjklzxcvbnm".equals(passward_temp) && "online".equals(command_temp)) {
                    					connected = true;
                    					desk3389thread = new Desk3389ToClientThread();
                    					desk3389thread.start();
                    					System.out.println("start desk3389thread");
                    				}
                    			}
            				}
            				
            			} else {
            				System.out.println("send to desk3389");
            				if (desk3389out != null) {
            					desk3389out.write(buff, 0, num);
    	            			//clientout.flush();
    	            		}
            			}
	            	} else {
	            		//stoprun();
	            		break;
	            	}
	            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("ClientToDeskThread exception");
			} finally {
				stoprun();
            }
            //stoprun();
        }
    }
    
    //from desk to client
    class Desk3389ToClientThread extends Thread {

        boolean running = true;
        boolean connected = false;

        public Desk3389ToClientThread() {
            
        }

        public void stoprun() {
        	running = false;
        	connected = false;
        	try {
        		if (desk3389in != null) {
        			desk3389in.close();
        		}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	try {
        		if (desk3389out != null) {
        			desk3389out.close();
        		}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	try {
        		if (desk3389socket != null) {
        			desk3389socket.close();
    			}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	desk3389socket = null;
        	desk3389out = null;
        	desk3389in = null;
        }
        
        @Override
        public void run() {
            super.run();
            try {
            	desk3389socket = new Socket("127.0.0.1", desk3389port);//用来监听的套接字，指定端口号
            	desk3389in = desk3389socket.getInputStream();
            	desk3389out = desk3389socket.getOutputStream();
	            byte[] buff = new byte[MESSAGE_SIZE];
	            byte[] command = new byte[128];
	            int num = -1;
	            String receivemsg = null;
	            while (running) {
	            	if ((num = desk3389in.read(buff)) != -1) {
	            		Arrays.fill(command, (byte)(0));
            			System.arraycopy(buff, 0, command, 0, num > command.length ? command.length : num);
            			receivemsg = new String(command, "UTF-8");
            			receivemsg = (receivemsg != null ? receivemsg.substring(0, num > command.length ? command.length : num) : null);
            			System.out.println("Desk3389ToClientThread receivemsg = " + receivemsg);
	            		if (clientout != null) {
	            			clientout.write(buff, 0, num);
	            			//clientout.flush();
	            		}
	            	} else {
	            		break;
	            	}
	            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Desk3389ToClientThread exception");
			} finally {
				stoprun();
            }
        }
    }
    
}
