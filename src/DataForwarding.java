
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
 
 
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

 
public class DataForwarding {
 
	//static int[] writeport = {6666,6667,6668,6669,6610,6611};//本地服务端口
	static int[] writeport = {19905, 19906, 19907, 19908, 19909};//本地服务端口
 
	static Map<Integer,DataForwardingServerSocket> WriteServerSocketMap = new HashMap<Integer,DataForwardingServerSocket>();
	
	
	 class DataForwardingClientSocket{
		
		private Socket socket;
		private int LocalPort;
		private SocketTimerTask socketTimerTask;
		private boolean isRun;
		
		public DataForwardingClientSocket(Socket socket){
			this.socket = socket;
			this.socketTimerTask = new SocketTimerTask(this);
			setRun(true);
		}

		public boolean isRun() {
			return isRun;
		}

		public void setRun(boolean isRun) {
			this.isRun = isRun;
		}
 
		public int getLocalPort() {
			return LocalPort;
		}

		public void setLocalPort(int localPort) {
			LocalPort = localPort;
		}

		public SocketTimerTask getSocketTimerTask() {
			return socketTimerTask;
		}

		public Socket getSocket() {
			return socket;
		}
 
		public void setSocket(Socket socket) {
			this.socket = socket;
		}
	}
	
	 class DataForwardingServerSocket{
		
		private ServerSocket serverSocket;
		private List<DataForwardingClientSocket> dataForwardingClientSocketsList = new ArrayList<DataForwarding.DataForwardingClientSocket>();
		
		public DataForwardingServerSocket(ServerSocket serverSocket){
			this.serverSocket = serverSocket;
		}
 
		public ServerSocket getServerSocket() {
			return serverSocket;
		}
 
		public void setServerSocket(ServerSocket serverSocket) {
			this.serverSocket = serverSocket;
		}
 
		public List<DataForwardingClientSocket> getDataForwardingClientSocketsList() {
			return dataForwardingClientSocketsList;
		}
 
		public void setDataForwardingClientSocketsList(List<DataForwardingClientSocket> dataForwardingClientSocketsList) {
			this.dataForwardingClientSocketsList = dataForwardingClientSocketsList;
		}
	}

	/*public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		DataForwarding dataForwarding = new DataForwarding();
		dataForwarding.startServer();
	}*/
	
	private void startServer(){//启动所有本地服务
		for (int port : writeport) {
			try {
				WriteServerSocketMap.put(port, new DataForwardingServerSocket(new ServerSocket(port)));
				new ReadThread(WriteServerSocketMap.get(port).getServerSocket()).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class ReadThread extends Thread{//每个服务创建一个独立线程
		ServerSocket serverSocket;
		public ReadThread(ServerSocket serverSocket){
			this.serverSocket = serverSocket;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			try {
				while(true){
						Socket socket = serverSocket.accept();
						DataForwardingClientSocket dataForwardingClientSocket = new DataForwardingClientSocket(socket);
						dataForwardingClientSocket.setLocalPort(socket.getLocalPort());
						WriteServerSocketMap.get(socket.getLocalPort()).getDataForwardingClientSocketsList().add(dataForwardingClientSocket);
						if(WriteServerSocketMap.get(serverSocket.getLocalPort()).getDataForwardingClientSocketsList().size() > 500){
							ColseSocket(dataForwardingClientSocket,null);
						}else{
							new ReadDataThread(dataForwardingClientSocket).start();
						}
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class SocketTimerTask extends TimerTask{//socket定时器 超时踢下线
		DataForwardingClientSocket dataForwardingClientSocket;
		int second = 60;
		Timer timer;
		public SocketTimerTask(DataForwardingClientSocket dataForwardingClientSocket){
			this.dataForwardingClientSocket = dataForwardingClientSocket;
			timer = new Timer();
			timer.schedule(this, 0,1000);
		}
		
		public void setTime(int second){
			this.second = second;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try{
//				System.out.println(this.dataForwardingClientSocket.getSocket().getPort()+": "+second);
//				sendData(dataForwardingClientSocket,("超时时间: "+second).getBytes("gbk"));
				if (second-- < 1) {//超时退出
//					sendData(dataForwardingClientSocket,"timeout exit".getBytes());
					ColseSocket(dataForwardingClientSocket,null);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
	
	}

	private void ColseSocket(DataForwardingClientSocket dataForwardingClientSocket,byte[] mas){
		Socket socket = null;
		try{
			if(dataForwardingClientSocket.isRun){
				dataForwardingClientSocket.setRun(false);
				WriteServerSocketMap.get(dataForwardingClientSocket.getLocalPort()).getDataForwardingClientSocketsList().remove(dataForwardingClientSocket);
				dataForwardingClientSocket.getSocketTimerTask().cancel();
				socket = dataForwardingClientSocket.getSocket();
				System.out.println(dataForwardingClientSocket.getSocket().getPort()+"断开连接");
				
				if(mas!=null && !mas.equals("")){
					sendData(dataForwardingClientSocket,mas);
				}
				socket.close();
				dataForwardingClientSocket = null;
			}
		}catch(Exception e){
			e.printStackTrace();
			try {
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		for (int port : writeport) {
			DataForwardingServerSocket dSocket = WriteServerSocketMap.get(port);
			System.out.println(("本地端口"+port+": 还有"+dSocket.getDataForwardingClientSocketsList().size()+"个外部连接"));
		}
		
	}

	private synchronized void sendData(DataForwardingClientSocket dataForwardingClientSocket,byte[] buff){
		try{
			dataForwardingClientSocket.getSocket().getOutputStream().write(buff);
		}catch(Exception e){
			e.printStackTrace();
			ColseSocket(dataForwardingClientSocket,null);
		}
	}

	private class ReadDataThread extends Thread{//每个socket一个独立的线程
		DataForwardingClientSocket dataForwardingClientSocket;
		Socket mDesktop = null;
		InputStream mDesktopIn = null;
		OutputStream mDesktopOut = null;
		boolean desktopRuning = false;
		public ReadDataThread(DataForwardingClientSocket dataForwardingClientSocket){
			this.dataForwardingClientSocket = dataForwardingClientSocket;
		}
		
		private void monitorDesktop() {
            if (!desktopRuning) {
            	return;
            }
			desktopRuning = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
			
				}
			}).start();
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			List<DataForwardingClientSocket> dataForwardingClientSocketList = WriteServerSocketMap.get(dataForwardingClientSocket.getSocket().getLocalPort()).getDataForwardingClientSocketsList();
			try{
				InputStream inputStream = dataForwardingClientSocket.getSocket().getInputStream();
				long sj1 = System.currentTimeMillis();//开始计时  报文接收超出当前时间1秒则清空buff
				byte buff[] = new byte[]{};
				int socketByte;
				while(this.dataForwardingClientSocket.isRun && (socketByte = inputStream.read()) != -1){				
					buff = byteMerger(buff, new byte[]{(byte) socketByte});
					if(this.dataForwardingClientSocket.getLocalPort() == writeport[0]){//6666 端口为代理功能 目前不支持https代理 没时间研究
						String item = new String(buff);
						if(item.contains("\n\r")){
							buff = new byte[]{};
//							System.out.println(item);
							new SendHttpThread(this.dataForwardingClientSocket,item).start();
							buff = new byte[]{};
						}
					}else if(this.dataForwardingClientSocket.getLocalPort() == writeport[1]){//6667 socket转发功能 当前端口下的所有的连接可以相互通信（群发）
						this.dataForwardingClientSocket.getSocketTimerTask().setTime(60);
						if (mDesktop != null) {
							mDesktop = new Socket("127.0.0.1", 3389);
						}
						if (mDesktop.isConnected() == true) {
							monitorDesktop();
							//mDesktop.getOutputStream().write(buff, 0, socketByte);
							//mDesktop.getOutputStream().flush();
						}
//						if(isComplete(buff)){
//							byte[] data = buff;
//						System.out.print((char)socketByte);
							/*for (DataForwardingClientSocket dataForwardingClientSocket : dataForwardingClientSocketList) {
//								if(this.dataForwardingClientSocket.getSocket().getPort() != clientSocket.getPort() || !this.dataForwardingClientSocket.getSocket().getInetAddress().getHostName().equals(clientSocket.getInetAddress().getHostName())){//如果是自己就不发送
									if(this.dataForwardingClientSocket.isRun){
										sendData(dataForwardingClientSocket,new byte[]{(byte) socketByte});
									}
//								}
							}*/
							buff = new byte[]{};
//						}
					}else{
						ColseSocket(this.dataForwardingClientSocket,"此端口功能还未开放".getBytes());
					}
//					System.out.println();
//					if((System.currentTimeMillis()-sj1)>=5000)//超出当前时间5秒  
//					{	
//						sj1  = System.currentTimeMillis();		//重新计时
//						sendData(dataForwardingClientSocket, "writer timeout".getBytes());
//						buff = new byte[]{};
//					}	
				}
				ColseSocket(this.dataForwardingClientSocket,null);
			}catch(IOException e){
				ColseSocket(this.dataForwardingClientSocket,null);
			}
		}
	}
	
	 private static OkHttpClient mHttpClient = new OkHttpClient();
	 private class SendHttpThread extends Thread{//代理线程
			DataForwardingClientSocket dataForwardingClientSocket;
			String data;
			public SendHttpThread(DataForwardingClientSocket dataForwardingClientSocket,String data){
				this.dataForwardingClientSocket = dataForwardingClientSocket;
				this.data = data;
			}
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				try{
					try{
						String get = "";
						get = data.substring(0,data.indexOf("\r"));
						data.replace(get, "");
						get = data.substring(data.indexOf(" ")+1,data.length());
						get = get.substring(0, get.indexOf(" "));
						if(data.contains("GET")){
							
							Request request = new Request.Builder()
								 .url(get)
								 .build();
							
							mHttpClient.newCall(request).enqueue(new Callback() {
								
								public void onFailure(Call call, IOException e) {

								}
								
								public void onResponse(Call call, Response response) throws IOException {
								 	try {
								 		sendData(dataForwardingClientSocket,response.body().bytes());
										ColseSocket(dataForwardingClientSocket,null);	
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
							 	}
							});
						} else if (data.contains("CONNECT")) {
							
						}

					} catch(Exception e){
						
					}

				}catch(Exception e){
					
				}
			}
		}

	/*报文校验规则*/
	private boolean isComplete(byte[] buff){
		try{
//			System.out.println("+++++++++++++++++");
//			byte[] type = new byte[]{buff[1],buff[2]};	//获取报文长度
//			int datal = Integer.parseInt(Integer.toHexString(buff[1] & 0xFF)+Integer.toHexString(buff[0] & 0xFF), 16);	//报文长度为 数据体长度+报头+包异或值
//
//			System.out.println(datal);
//			
//			if(buff[0] == 0x02 && buff.length > 5){
//				return true;
//			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
//	static byte[] getAscii(String s){
//		
//		byte[] b = s.getBytes();
//		
//		byte[] in = new byte[b.length];
//		for (int i = 0; i < in.length; i++) {
//		in[i] = b[i];
////		System.out.println(Integer.toString(in[i], 0x10));
//		}
//		return b;
//		
//	}
	
	/**
	 * int转成4个字节的byte[]
	 * 16进制码表示，低字节在前*/
	private byte[] intToByteArray1(int i) {
		byte[] result = new byte[4];
		result[3] = (byte)((i >> 24) & 0xFF);
		result[2] = (byte)((i >> 16) & 0xFF);
		result[1] = (byte)((i >> 8) & 0xFF);
		result[0] = (byte)(i & 0xFF);
		return result;
	}
	
	/**
	 *  合并两个byte数组  */
	private byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
		if(byte_1 != null && byte_2 == null){
			return byte_1;
		}
		if(byte_1 == null && byte_2 != null){
			return byte_2;
		}
		if(byte_1 == null || byte_2 == null){
			return new byte[]{};
		}
		byte[] byte_3 = new byte[byte_1.length + byte_2.length];
		System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
		//System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
		return byte_3;
	}
}
