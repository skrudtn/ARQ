package pa;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client implements Runnable {
	static final int TIMEOUT = 2000;

	static Scanner sc = new Scanner(System.in);
	static Socket socket;
	static StopWait sw;
	static GoBackN gbn;

	public static void main(String[] args) {
		try {
			String serverIP = "127.0.0.1";
			socket = new Socket(serverIP, 9998);
			socket.setSoTimeout(TIMEOUT);

			String sendData = "웃는 모습이 너무 아름다운 그런 너를 기억하면서 괜찮아 넌 잘할거라 말하던 또 그런 너를 기억하면서 뭐가 그리 내 눈에 깊이 박혔는지 너무 선명해서 이젠 보낼수가 없잖아 끝.";

			int mode =getMode();
			if(mode == 1){
				stopWaitSender(sendData);
			}
			else{
				goBackNSender(sendData);
			}

		} catch (Exception e) {
		}
	}

	public static String receiver() throws IOException{
		InputStream in = socket.getInputStream();
		DataInputStream dis = new DataInputStream(in);  // 기본형 단위로 처리하는 보조스트림
		String receiveMsg = "";

		try{  //전송후 수신시간이 3초를 지나면 TIME OUT		
			receiveMsg = dis.readUTF();
		}
		catch(SocketTimeoutException ste){
			System.out.println("TIME OUT !!");
			receiveMsg = "timeout";
		}
		return receiveMsg;
	}

	public static void msgSender(String sendMsg) throws IOException, InterruptedException{
		// 소켓의 출력스트림을 얻는다.
		OutputStream out = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out); // 기본형 단위로 처리하는 보조스트림

		dos.writeUTF(sendMsg);
	}
	public static void intSender(int sendInt) throws IOException{
		// 소켓의 출력스트림을 얻는다.
		OutputStream out = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out); // 기본형 단위로 처리하는 보조스트림

		dos.writeInt(sendInt);
	}

	private static int getMode() throws IOException{ 
		int mode = 0;
		while(mode != 1 && mode != 2){
			System.out.println("1. Stop & Wait ,  2. Go Back N ");
			mode = sc.nextInt();
		}
		intSender(mode); //모드를 결정하고 Server와 모드 동기화
		return mode;
	}

	private static void syncSWNum() throws IOException{
		intSender(sw.getDataSize());
		intSender(sw.getLostSeq());
		intSender(sw.getErrorSeq());
		intSender(sw.getLostAck());
	}
	private static void syncGBNNum() throws IOException{
		intSender(gbn.getDataSize());
		intSender(gbn.getLostSeq());
		intSender(gbn.getErrorSeq());
		intSender(gbn.getLostAck());
		intSender(gbn.getErrorAck());
	}

	private static void stopWaitSender(String sendData) throws IOException, InterruptedException{
		sw = new StopWait(sendData);
		int maxCount = sw.getDataSize();
		//총 데이터의 크기를 동기화
		sw.setError();//고의적으로 에러발생 시킬 번호
		syncSWNum();//총 데이터 사이즈와 에러를 발생시킨 번호를 서버와 동기화 해줌

		sw.sendData();
		msgSender(sw.getSendData()); // 첫 프레임 전송

		while(sw.getCount() < maxCount){
			if(sw.recAck(receiver())){ // ack를 수신
				Thread.sleep(200);
				if(sw.sendData()) //전송 성공 유무체크
				{
					msgSender(sw.getSendData()); //받으면 보낸다.
				}
			}
			
			else{ //TIMEOUT 발생시 재전송
				sw.sendData();//재전송
				msgSender(sw.getSendData()); //재전송
			}
			
		}
		System.out.println("Success Send!!!");
		socket.close();
	}

	private static void goBackNSender(String sendData) throws IOException, InterruptedException{
		gbn = new GoBackN(sendData);

		int maxCount = gbn.getDataSize();
		gbn.setError();//고의적으로 에러발생 시킬 번호
		syncGBNNum();//총 데이터 사이즈와 에러를 발생시킨 번호를 수신측과 동기화 해줌

		gbn.sendData();//최초 전송
		msgSender(gbn.getSendData());

		Thread t = new Thread(new Client());	
		t.start();

		while(gbn.getCount() < maxCount){
			if(gbn.sendData()){ //DATA 송신 체크
				msgSender(gbn.getSendData());//실제로 데이터를 보낸다.
			}
			else{ //DATA전송 실패
				if(gbn.rrFlag) continue;
			}
			Thread.sleep(200);
		}

		System.out.println("Success Send!!");

	}

	@Override
	public void run() {
		int maxCount = gbn.getDataSize();
		while(gbn.getCount()<maxCount){
			try {
				gbn.recAck(receiver());
			} catch (IOException e) {
			}
		}
	}

}