package pa;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.ConnectException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Server{
	static StopWait sw;
	static GoBackN gbn;
	static Socket socket;

	public static void main(String[] args) throws ConnectException {
		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(9998);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			socket = serverSocket.accept();
			socket.setSendBufferSize(3);
			int mode =getMode();

			if(mode == 1){
				stopWaitReceiver();
			}
			else{
				goBackNReceiver();
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static String msgReceiver() throws IOException{
		InputStream in = socket.getInputStream();
		DataInputStream dis = new DataInputStream(in);  // �⺻�� ������ ó���ϴ� ������Ʈ��
		String rec = "";
		try{
			rec = dis.readUTF();
		}
		catch(SocketTimeoutException ste){

		}

		return rec;
	}

	public static int intReceiver() throws IOException{
		InputStream in = socket.getInputStream();
		DataInputStream dis = new DataInputStream(in);  // �⺻�� ������ ó���ϴ� ������Ʈ��

		int rec = 0;
		try{
			rec = dis.readInt();
		}
		catch(SocketTimeoutException ste){

		}

		return rec;
	}

	public static void sender(String sendMsg) throws IOException{
		// ������ ��½�Ʈ���� ��´�.
		OutputStream out = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out); // �⺻�� ������ ó���ϴ� ������Ʈ��

		dos.writeUTF(sendMsg);
	}

	private static void syncSWNum() throws IOException{
		sw.setDataSize(intReceiver());
		sw.setLostSeq(intReceiver());
		sw.setErrorSeq(intReceiver());
		sw.setLostAck(intReceiver());
	}
	private static void syncGBNNum() throws IOException{
		gbn.setDataSize(intReceiver());
		gbn.setLostSeq(intReceiver());
		gbn.setErrorSeq(intReceiver());
		gbn.setLostAck(intReceiver());
		gbn.setErrorAck(intReceiver());
	}

	private static int getMode() throws IOException{
		return intReceiver(); // Ŭ���̾�Ʈ�� ��� ����ȭ
	}

	private static void stopWaitReceiver() throws IOException, InterruptedException{
		sw = new StopWait("");
		syncSWNum();

		int maxCount = sw.getDataSize();


		while(sw.getCount()<maxCount){
			sw.recData(msgReceiver());
			Thread.sleep(200);
			if(sw.ackSend()){
				sender(sw.getAckNum()); //ack�� ������.
			}
			// ack ���� ���й�ȣ�̸� ack�� ������ �ʴ´�.
		}

		System.out.println("Success Receive!! - "+sw.getRecData());
		socket.close();
	}

	private static void goBackNReceiver() throws IOException, InterruptedException{
		gbn = new GoBackN("");
		syncGBNNum();
		int maxCount = gbn.getDataSize();

		while(gbn.getCount() < maxCount){
			gbn.recData(msgReceiver());//DATA���� ���� üũ
			if(gbn.sendAck()){ // ACK�۽� üũ
				sender(gbn.getSendData());
			}
		}
		System.out.println("Success Recv!! \n"+gbn.getRecData());

	}
}
