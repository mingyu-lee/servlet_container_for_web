package ch04;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/*
 * 쿼리스트링 방식과 x-www-form-urlencoded 방식의 매개변수 전달에 대응할 수 있게,
 * 특정 메시지 헤더를 체크하고 필요한 경우 메시지 바디를 분석해
 * 웹 브라우저가 전달한 매개변수를 서버 사이드에서 재구성하는 예제
 * 
 * - 구현의 기본적인 접근 방식 -
 * 1. 먼저 소켓 스트림을 열어 클라이언트가 전달한 데이터를 한 바이트씩 읽어간다.
 * 2. 이 때 HTTP 메시지의 내부 상태를 계속 관찰하면서 메시지 종료 조건을 동적으로 판별해
 *    메시지 읽기 종료 시점을 결정하고 HTTP 메시지를 구성한다.
 * 3. 이제 웹 브라우저에서 받은 요청이 무엇인지 결정해 적절한 응답을 보낸다.
 * 
 */
public class Server_3 {

	private ServerSocket serverSocket;
	private static final byte CR = '\r';
	private static final byte LF = '\n';
	
	public static void main(String[] args) throws IOException {
		Server_3 server = new Server_3();
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
		List<Byte> bodyByteList= null;
		Map<String, String> headerMap = new HashMap<String, String>();
		
		while(-1 != (oneInt = in.read())) {
			byte thisByte = (byte)oneInt;
			if(bodyFlag) {
				bodyRead++;
				bodyByteList.add(thisByte);
				if(bodyRead >= contentLength) {
					break;
				}
			} else {
				if(thisByte == Server_3.LF && oldByte == Server_3.CR) {
					String oneLine = sb.substring(0, sb.length()-1);
					lineNumber++;
					if(lineNumber == 1) {
						// 요청의 첫 행, HTTP 메서드, 요청 URL, HTTP 버전을 알아낸다.
						int firstBlank = oneLine.indexOf(" ");
						int secondBlank = oneLine.lastIndexOf(" ");
						method = oneLine.substring(0, firstBlank);
						requestUrl = oneLine.substring(firstBlank+1, secondBlank);
						httpVersion = oneLine.substring(secondBlank+1);
					} else {
						if(oneLine.length() <= 0) {
							bodyFlag = true;
							// 헤더가 끝났다.
							if("GET".equals(method)) {
								break;
							}
							String contentLengthValue = headerMap.get("Content-Length");
							if(contentLengthValue != null) {
								contentLength = Integer.parseInt(contentLengthValue.trim());
								bodyFlag = true;
								bodyByteList = new ArrayList<Byte>();
							}
							continue;
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
			oldByte = (byte)oneInt;
		}
		in.close();
		socket.close();
		
		System.out.printf("METHOD: %s REQ: %s HTTP VER. %s\n", method, requestUrl, httpVersion);
		Map<String, String> paramMap = new HashMap<String, String>();
		int indexOfQuotation = requestUrl.indexOf("?");
		if(indexOfQuotation > 0) {
			StringTokenizer st = new StringTokenizer(requestUrl.substring(indexOfQuotation+1), "&");
			while(st.hasMoreTokens()) {
				String params = st.nextToken();
				paramMap.put(params.substring(0, params.indexOf("=")),
						params.substring(params.indexOf("=")+1));
			}
		}
		System.out.println("Header List");
		Set<String> keySet = headerMap.keySet();
		Iterator<String> keyIter = keySet.iterator();
		while(keyIter.hasNext()) {
			String headerName = keyIter.next();
			System.out.printf(" Key: %s Value: %s\n", headerName, headerMap.get(headerName));
		}
		if(bodyByteList != null) {
			if("application/x-www-form-urlencoded".equals(headerMap.get("Content-Type").trim())) {
				int startIndex = 0;
				byte[] srcBytes = new byte[bodyByteList.size()];
				String currentName = null;
				for(int i=0; i<bodyByteList.size(); i++) {
					byte oneByte = bodyByteList.get(i);
					srcBytes[i] = oneByte;
					if('=' == oneByte) {
						byte[] one = new byte[i-startIndex];
						System.arraycopy(srcBytes, startIndex, one, 0, i-startIndex);
						currentName = URLDecoder.decode(new String(one), "CP949");
						startIndex = i+1;
					} else if('&' == oneByte) {
						byte[] one = new byte[i-startIndex];
						System.arraycopy(srcBytes, startIndex, one, 0, i-startIndex);
						paramMap.put(currentName, URLDecoder.decode(new String(one), "CP949"));
						startIndex = i+1;
					} else if(i == bodyByteList.size()-1) {
						byte[] one = new byte[i-startIndex+1];
						System.arraycopy(srcBytes, startIndex, one, 0, i-startIndex+1);
						paramMap.put(currentName, URLDecoder.decode(new String(one), "CP949"));
						startIndex = i+1;
					}
				}
			} else {
				System.out.print("Message Body-->");
				for(byte oneByte : bodyByteList) {
					System.out.print(oneByte);;
				}
				System.out.println("<--");;
			}
		}
		Set<String> paramKeySet = paramMap.keySet();
		Iterator<String> paramKeyIter = paramKeySet.iterator();
		while(paramKeyIter.hasNext()) {
			String paramName = paramKeyIter.next();
			System.out.printf("paramName: %s paramValue: %s\n", paramName, paramMap.get(paramName));
		}
		System.out.println("End of HTTP Message. ");
	}
	
}
