import java.net.Socket;


public class ConnectionInfo {
	public ConnectionInfo(Socket socket){
		this.socket = socket;
	}
	public Socket	socket;
	public int 	timeOut = 10000;
	public boolean	toDelete = false;	//true - we need to delete this
}
