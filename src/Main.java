import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.ho.yaml.Yaml;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;

public class Main extends WebSocketServer{
	public Logger logger = Logger.getLogger(Main.class.getName());
	
	public ConcurrentHashMap<WebSocket, ConnectionInfo> connections = new ConcurrentHashMap<WebSocket, ConnectionInfo>();
	
	public Config config;
	
	public Main(Config config) throws UnknownHostException, FileNotFoundException {
		super(new InetSocketAddress(config.port));
		this.config = config;
		WebSocket.DEBUG = true;
		logger.setLevel(Level.ALL);  
		//logger.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
		
		logger.info(">> Main");
		new SocketProcessorThread(this, connections);
		
		logger.info("<< Main");
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		//Opening new proxy connection
		// TODO Auto-generated method stub
		logger.info(">> onOpen");
		try{
			Socket socket = new Socket(config.targetHost, config.targetPort);
			connections.put(conn, new ConnectionInfo(socket));
			System.out.println(conn);
			
		}catch(Exception e){
			logger.warning(e.getLocalizedMessage());
			e.printStackTrace();
		}
		logger.info("<< onOpen");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		// TODO Auto-generated method stub
		logger.info(">> onClose");
		try{
			logger.info(reason);
			
			if(!connections.contains(conn)){
				logger.info("<< onClose (connection is already marked for remove");
				return;
			}
			connections.get(conn).toDelete = true;
			
		}catch(Exception e){
			logger.warning(e.getLocalizedMessage());
			e.printStackTrace();
		}
		logger.info("<< onClose");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		// TODO Auto-generated method stub
		logger.info(">> onMessage");
		try{
			String messageUTF8 = new String(message.getBytes("UTF-8"));
			Socket socket = connections.get(conn).socket;
			OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
			
			System.out.print(">> ");
			
			//Replacing symbols according to map
			for (CharMap cm : config.charMap){
				messageUTF8 = messageUTF8.replace( (char)cm.websocket, (char)cm.socket);
			}
			//end
			System.out.print(messageUTF8+"\n");
			System.out.println();
			out.write(messageUTF8+"\n");
			
			out.flush();
		}catch(Exception e){
			logger.warning(e.getLocalizedMessage());
			e.printStackTrace();
		}
		logger.info("<< onMessage");
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		logger.info(">> onError");
		// TODO Auto-generated method stub
		System.out.println("Trace");
		ex.printStackTrace();
		System.out.println(ex.getMessage());
		logger.info("<< onError");
	}

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try{
			if(args.length < 1){
				System.out.println("You miss point to configuration file!\n"+
						"You should use this syntax:\n"+
						"java WebSocketProxy.jar config_file_name.ext\n"+
						"Sample configuration file:\n"+
						"target-host: google.com\ntarget-port: 80\nport: 8887\n");
				System.exit(-1);
			}
			Config options = Yaml.loadType(new File(args[0]), Config.class);
			Main main = new Main(options);
			main.start();
		}catch (Exception e){
			e.printStackTrace();
			System.out.println(e.getLocalizedMessage());
		}
	}

}
