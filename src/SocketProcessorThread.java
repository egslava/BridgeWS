import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;


/**
 * Class, that testing in cycle - are there any incoming message?
 * @author Slava
 *
 */
public class SocketProcessorThread extends Thread{
	private static Logger logger = 
			Logger.getLogger(SocketProcessorThread.class.getName());
	
	private ConcurrentHashMap<WebSocket, ConnectionInfo> connections;
	private Main parentThread;
	//private ConcurrentHashMap<WebSocket, Socket> queueForRemove = new ConcurrentHashMap<WebSocket, Socket>();
	
	public SocketProcessorThread(Main parentThread, ConcurrentHashMap<WebSocket, ConnectionInfo> connections) {
		this.parentThread = parentThread;
		
		logger.setParent(parentThread.logger);
		logger.setUseParentHandlers(true);
		logger.setLevel(Level.OFF);
		logger.info(">> SocketProcessorThread");
		this.connections = connections;
		this.start();
		logger.info("<< SocketProcessorThread");
	}
	
	@Override
	public void run() {
		
		// TODO Auto-generated method stub
		super.run();
		//Iterator<WebSocket> iterator = connections.keySet().iterator();
		while(true){
			for(WebSocket connection: connections.keySet()){
				try {
					ConnectionInfo connectionInfo = connections.get(connection);
					
					if(tryClose(connection, connectionInfo))
						tryRead(connection, connectionInfo);
					
					
				//}catch (ConcurrentModificationException e){
					//Normal situation, we just removed hashmap-item
				}catch (Exception e){
					logger.info(e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
			
			//removing dead connections
			for(WebSocket connection: connections.keySet()){
				try{
					if(connections.get(connection).toDelete)
						removeConnection(connection, connections.get(connection));	
				}catch(Exception e){
					System.out.println(e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
			
			//queueForRemove.clear();
			
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.info(e.getLocalizedMessage());
				e.printStackTrace();
			}
			
		}
	}
	
	private boolean tryClose(WebSocket connection, ConnectionInfo connectionInfo) throws IOException{
		
		Socket socket = connectionInfo.socket;
		if(!socket.isConnected() || connection.isClosed()){
			logger.info("tryClose: Connection was marked for removing");
			connections.get(connection).toDelete = true;
			return false;
		}
		
		return true;
		
	}
	private void tryRead(WebSocket connection, ConnectionInfo connectionInfo) throws IOException, IllegalArgumentException, InterruptedException{
		InputStream inStream = connectionInfo.socket.getInputStream();
		
		try{
			while(inStream.available() > 0){
				byte b[] = new byte[inStream.available()];
				inStream.read(b);
				
				if(logger.getLevel() != Level.OFF){
					System.out.print("<< ");
					System.out.print(new String(b, "UTF-8"));
					System.out.println();	
				}
				
				
				String s = new String(b, "UTF-8");
				for(CharMap cm : parentThread.config.charMap){
					s = s.replace( (char)cm.socket, (char) cm.websocket);	
				}
				
				connection.send(s);
			}	
		}catch (Exception e){
			logger.info(e.getLocalizedMessage());
			e.printStackTrace();
			
			tryClose(connection, connectionInfo);
		}
		
	}
	
	private void removeConnection(WebSocket connection, ConnectionInfo connectionInfo) throws IOException{
		logger.info(">> removeConnection");

		try{
			Socket socket = connectionInfo.socket;
			if( ! socket.isClosed()){
				socket.shutdownInput();
				socket.shutdownOutput();
				
				if(! socket.isClosed())
					socket.close();	
			}
		}finally{
			connections.remove(connection);
		}
		connections.remove(connection);
		logger.info("<< removeConnection");
	}
	
}