package ch04;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysorForChunked {
	
	private ServerSocket serverSocket;
	private static final byte CR = '\r';
	private static final byte LF = '\n';
	
	public static void main(String[] args) throws IOException {
		AnalysorForChunked server = new AnalysorForChunked();
		server.boot();
	}
	
	private void boot() throws IOException {
		serverSocket = new ServerSocket(8000);
		Socket socket = serverSocket.accept();
		
		InputStream in = socket.getInputStream();
		int oneInt = -1;
		byte oldByte = (byte) -1;
		StringBuilder sb = new StringBuilder();
		int lineNumber = 0;
		boolean bodyFlag = false;
		
		String method = null;
		String requestUrl = null;
		String httpVersion = null;
		
		int contentLength = -1;
		int bodyRead = 0;
		
		List<Byte> bodyByteList = null;
		Map<String, String> headerMap = new HashMap<String, String>();
		
		int readSize = 0;
		byte[] readBuffer = new byte[1024];
		boolean isTerminal = false;
		
		while( 0 < (readSize = in.read(readBuffer))) {
			for (int i = 0; i < readSize; i++) {
				byte thisByte = (byte)readBuffer[i];
				System.out.print((char)thisByte);
				if(bodyFlag) {
					bodyRead ++;
					bodyByteList.add(thisByte);
					if(bodyRead >= contentLength) {
						isTerminal = true;
						break;
					}
				} else {
					if (thisByte == AnalysorForChunked.LF 
							&& thisByte == AnalysorForChunked.CR) {
						String oneLine = sb.substring(0, sb.length()-1);
						lineNumber ++;
						if (lineNumber == 1) {
							int firstBreak = oneLine.indexOf(" ");
							int secondBerak = oneLine.indexOf(" ");
							method = oneLine.substring(0, firstBreak);
							requestUrl = oneLine.substring(firstBreak+1, secondBerak);
							httpVersion = oneLine.substring(secondBerak+1);
						} else {
							if(oneLine.length() <= 0) {
								bodyFlag = true;
								if("GET".equals(method)) {
									isTerminal = true;
									break;
								}
								String transferEncoding = headerMap.get("Transfer-Encoding");
								if(transferEncoding.equals("chunked")) {
									bodyFlag = true;
								}
							}
							int indexOfColon = oneLine.indexOf(":");
							String headerName = oneLine.substring(0, indexOfColon);
							String headerValue = oneLine.substring(indexOfColon+1);
							headerMap.put(headerName, headerValue);
						}
						sb.setLength(0);;
					} else {
						sb.append((char)thisByte);
					}
				}
				oldByte = (byte)thisByte;
			}
			if(isTerminal) {
				break;
			}
		}
		
		in.close();
		socket.close();

		System.out.printf("METHOD: %s REQ: %s HTTP VER. %s\n", method, requestUrl, httpVersion);
	}

}
