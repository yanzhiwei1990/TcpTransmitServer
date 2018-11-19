
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by HeTingwei on 2017/12/9.
 * 多线程服务器,实现多客户端聊天
 */
public class HostServer {

    List<ReceiveThread> receiveList = new ArrayList<>();//存放已连接客户端类
    int MESSAGE_SIZE = 4096;//每次允许接受数据的最大长度
    int num = 0;//客户端编号
    int hostport = 19909;
    
    ServerSocket serverSocket = null;
	Socket desksocket;
    Socket clientsocket;
    String sockettype = null;
    boolean desksocketonline = false;
    boolean clientsocketonline = false;
    DeskToClientThread mDeskSendThread = null;
    ClientToDeskThread mClientSendThread = null;
    InputStream deskin = null;
    InputStream clientin = null;
    OutputStream deskout = null;
    OutputStream clientout = null;

    public static void main(String[] args) {
    	HostServer mServer = new HostServer();
    }

    public HostServer() {
    	ServerHandle mServerHandle = new ServerHandle();
    	mServerHandle.start();
    }
    
    class ServerHandle extends Thread {
    	
    	public ServerHandle() {
    		
    	}
    	
    	@Override
        public void run() {
    		super.run();
            try {
                serverSocket = new ServerSocket(hostport);//用来监听的套接字，指定端口号
                while (true) {
                    Socket socket = serverSocket.accept();//监听客户端连接，阻塞线程
                    System.out.println("连接上客户端：" + num);
                    //在其他线程处理接收来自客户端的消息
                    ReceiveThread receiveThread = new ReceiveThread(socket, num);
                    receiveThread.start();
                    //receiveList.add(receiveThread);
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
        InputStream in;
        String currentsockettype = null;
        String currentsocketcommand = null;

        public ReceiveThread(Socket socket, int num) {
            this.socket = socket;
            this.num = num;
        }

        public void stoprun() {
        	continueReceive = false;
        	if (mDeskSendThread != null) {
        		mDeskSendThread.stoprun();
        	}
        	if (mClientSendThread != null) {
        		mClientSendThread.stoprun();
        	}
        }
        
        @Override
        public void run() {
            super.run();
            //接收客户端发送的消息
            try {
                byte[] b = new byte[MESSAGE_SIZE];
                byte[] command = new byte[128];
                Arrays.fill(b, (byte)('\0'));
                Arrays.fill(command, (byte)('\0'));
                int num = -1;
                String receivemsg = null;
                in = socket.getInputStream();
                while (continueReceive) {
            		receivemsg = null;
            		num = in.read(b, 0, 128);
            		//read for socket type or message: type:*-command:*-password:qwertyuiopasdfghjklzxcvbnm- 
            		//type:desktop-command:connect-  type:client-command:connect-
            		System.out.println("receivemsg num = " + num);
            		if (num != -1) {
            			Arrays.fill(command, (byte)('\0'));
            			System.arraycopy(b, 0, command, 0, num);
            			receivemsg = new String(command, "UTF-8");
            			receivemsg = (receivemsg != null ? receivemsg.substring(0, num) : null);
            			System.out.println("receivemsg = " + receivemsg);
            		} else {
            			System.out.println("close current socket: " + currentsockettype);
            			if ("desktop".equals(currentsockettype)) {
            				stopDeskSocketMatch();
            			} else if ("client".equals(currentsockettype)) {
            				stopClientSocketMatch();
            			} else {
            				
            			}
            			break;
            		}
            		if ((currentsockettype == null && receivemsg == null)  || receivemsg != null) {
            			String[] result = receivemsg.split("-");
            			if (result != null && result.length >= 3) {
            				//currentsockettype = null;
            				//currentsocketcommand = null;
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
            				System.out.println("type_temp:" + type_temp + ",command_temp:" + command_temp + ",passward_temp:" + passward_temp);
            				if ("qwertyuiopasdfghjklzxcvbnm".equals(passward_temp) && ("client".equals(type_temp) || "desktop".equals(type_temp))) {
            					currentsockettype = type_temp;
            					currentsocketcommand = command_temp;
            				} else if (currentsockettype == null) {
            					System.out.println("type or passward erro");
            					break;
            				}
            				//System.out.println("currentsockettype = " + currentsockettype + ", currentsocketcommand = " + currentsocketcommand);
            				//update connect
            				if (currentsockettype != null) {
                    			if ("desktop".equals(currentsockettype) && "connect".equals(currentsocketcommand)) {
                    				if (desksocket != null) {
                    					desksocket.close();
                    					desksocket = null;
                    				}
                    				stopDeskSocketMatch();
                    				desksocket = socket;
                    				if (desksocket != null) {
                    					deskout = desksocket.getOutputStream();
                    					deskin = in;//desksocket.getInputStream();
                    					//deskin.skip(num);
                    					deskout.write("desksocket online".getBytes("UTF-8"));
                    					if (clientout != null) {
                    						clientout.write("desksocket online".getBytes("UTF-8"));
                    					}
                    				}
                    				//restartDesktopSocketMatch();
                    				startDesktopMatch();
                    				System.out.println("desksocket online");
                    				break;
                    			} else if ("client".equals(currentsockettype) && "connect".equals(currentsocketcommand)) {
                    				if (clientsocket != null) {
                    					clientsocket.close();
                    					clientsocket = null;
                    				}
                    				stopClientSocketMatch();
                    				clientsocket = socket;
                    				if (clientsocket != null) {
                    					clientout = clientsocket.getOutputStream();
                    					clientin = in;//clientsocket.getInputStream();
                    					//clientin.skip(num);
                    					clientout.write("clientsocket online".getBytes("UTF-8"));
                    					if (deskout != null) {
                    						deskout.write("clientsocket online".getBytes("UTF-8"));
                    					}
                    				}
                    				//restartClientSocketMatch();
                    				startClientMatch();
                    				System.out.println("clientsocket online");
                    				break;
                    			}
                    			/*if (desksocket != null && clientsocket != null) {
                    				if (desksocket.isConnected() && clientsocket.isConnected()) {
                    					System.out.println("remote desk ready to control desk");
                    				} else {
                    					System.out.println("remote desk status erro");
                    				}
                    			}*/
                    			
                    		} else {
                    			System.out.println("invalid socket");
                    			break;
                    		}
            			} else if (currentsockettype != null && (receivemsg.equals("close") || receivemsg.equals("exit"))) {
            				System.out.println(currentsockettype + " receive close command:" + receivemsg);
            				if ("desktop".equals(currentsockettype)) {
                				stopDeskSocketMatch();
                			} else if ("client".equals(currentsockettype)) {
                				stopClientSocketMatch();
                			}
                			break;
            			} 
            		} else {
            			break;
            		}
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            	//receiveList.remove(this);
            	//if (!("client".equals(currentsockettype)) && !("desktop".equals(currentsockettype))) {
            		/*try {//关闭资源
                        if (socket != null) {
                            socket.close();
                        }
                        if (in != null) {
                        	in.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
            		System.out.println("end socket type = " + currentsockettype + ", command = " + currentsocketcommand);
            	//}
            }
        }
    }
    
    
    
    public void restartDesktopSocketMatch() {
    	stopDeskSocketMatch();
    	startDesktopMatch();
    }
    
    public void restartClientSocketMatch() {
    	stopClientSocketMatch();
    	startClientMatch();
    }
    
    public void stopDeskSocketMatch() {
    	if (mDeskSendThread != null) {
			mDeskSendThread.stoprun();
			mDeskSendThread = null;
			System.out.println("stop mDeskSendThread as started");
		} else {
			System.out.println("mDeskSendThread is already stopped");
		}
    }
    
    public void stopClientSocketMatch() {
		if (mClientSendThread != null) {
			mClientSendThread.stoprun();
			mClientSendThread = null;
			System.out.println("stop mDeskReceiveThread as started");
		} else {
			System.out.println("mDeskReceiveThread is already stopped");
		}
    }
    
    public void startDesktopMatch() {
    	if (mDeskSendThread != null) {
			System.out.println("desktop match is already running");
			return;
		}

    	mDeskSendThread = new DeskToClientThread();
    	mDeskSendThread.start();
		System.out.println("start new connect desktop match");
    }
    
    public void startClientMatch() {
    	if (mClientSendThread != null) {
			System.out.println("client match is already running");
			return;
		}

    	mClientSendThread = new ClientToDeskThread();
    	mClientSendThread.start();
		System.out.println("start new connect client match");
    }
    
    //from client to desk
    private class ClientToDeskThread extends Thread {
        //Socket mdesksocket;
        //Socket remotesocket;
        boolean running = true;
        //InputStream remotein;
        //OutputStream deskout;

        public ClientToDeskThread(/*Socket desksocket, Socket remotesocket*/) {
            //this.mdesksocket = desksocket;
            //this.remotesocket = remotesocket;
        }

        /*public void setDeskSocket(Socket socket) {
        	mdesksocket = socket;
        	if (deskout != null) {
        		try {
					deskout.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	try {
				deskout = mdesksocket.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }*/
        
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
        }
        
        @Override
        public void run() {
            super.run();
            try {
            	if (clientin == null) {
            		System.out.println("clientin null");
            		return;
            	}
	            byte[] buff = new byte[MESSAGE_SIZE];
	            int num = -1;
	            while (running) {
	            	if (clientin == null) {
	            		continue;
	            	}
	            	if ((num = clientin.read(buff)) != -1) {
	            		if (deskout != null) {
	            			deskout.write(buff, 0, num);
		            		//deskout.flush();
	            		}
	            	} else {
	            		//stoprun();
	            		break;
	            	}
	            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("ClientToDeskThread end");
			} finally {
				stoprun();
            }
            //stoprun();
        }
    }
    
    //from desk to client
    class DeskToClientThread extends Thread {
        //Socket mdesksocket;
        //Socket remotesocket;
        boolean running = true;
        //InputStream deskin;
        //OutputStream remoteout;

        public DeskToClientThread(/*Socket desksocket, Socket remotesocket*/) {
            //this.mdesksocket = desksocket;
            //this.remotesocket = remotesocket;
        }

        public void stoprun() {
        	running = false;
        	try {
        		if (deskin != null) {
        			deskin.close();
        		}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	try {
        		if (deskout != null) {
        			deskout.close();
        		}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	try {
        		if (desksocket != null) {
    				desksocket.close();
    			}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	desksocket = null;
        	deskout = null;
        	deskin = null;
        }
        
        @Override
        public void run() {
            super.run();
            try {
            	if (deskin == null) {
            		System.out.println("deskin null");
            		return;
            	}
	            byte[] buff = new byte[MESSAGE_SIZE];
	            int num = -1;
	            while (running) {
	            	if ((num = deskin.read(buff)) != -1) {
	            		if (clientout != null) {
	            			clientout.write(buff, 0, num);
	            			//clientout.flush();
	            		}
	            	} else {
	            		//stoprun();
	            		break;
	            	}
	            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("DeskToClientThread end");
			} finally {
				stoprun();
            }
            //stoprun();
        }
    }
    
    Socket desk3389socket = null;
    
    
    //local 3389
    class Desk3389tThread extends Thread {
        //Socket mdesksocket;
        //Socket remotesocket;
        boolean running = true;
        //InputStream deskin;
        //OutputStream remoteout;

        public Desk3389tThread(/*Socket desksocket, Socket remotesocket*/) {
            //this.mdesksocket = desksocket;
            //this.remotesocket = remotesocket;
        }

        public void stoprun() {
        	running = false;
        	try {
        		if (deskin != null) {
        			deskin.close();
        		}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	try {
        		if (deskout != null) {
        			deskout.close();
        		}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	try {
        		if (desksocket != null) {
    				desksocket.close();
    			}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	desksocket = null;
        	deskout = null;
        	deskin = null;
        }
        
        @Override
        public void run() {
            super.run();
            try {
            	if (deskin == null) {
            		System.out.println("deskin null");
            		return;
            	}
	            byte[] buff = new byte[MESSAGE_SIZE];
	            int num = -1;
	            while (running) {
	            	if ((num = deskin.read(buff)) != -1) {
	            		if (clientout != null) {
	            			clientout.write(buff, 0, num);
	            			//clientout.flush();
	            		}
	            	} else {
	            		//stoprun();
	            		break;
	            	}
	            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("DeskToClientThread end");
			} finally {
				stoprun();
            }
            //stoprun();
        }
    }
}
