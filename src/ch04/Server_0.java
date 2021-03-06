package ch04;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * 포트를 열어 웹 브라우저의 요청을 표준 출력에 남기는 예제
 */
public class Server_0 {

	private ServerSocket serverSocket;
	
	public static void main(String[] args) throws IOException {
		Server_0 server = new Server_0();
		server.boot();
	}
	
	private void boot() throws IOException {
		serverSocket = new ServerSocket(8000);
		Socket socket = serverSocket.accept();
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		int oneInt = -1;
		while(-1 != (oneInt = in.read())) {
			System.out.print((char)oneInt);
		}
		out.close();
		in.close();
		socket.close();
		
	}
	
}
