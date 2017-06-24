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

			String sendData = "���� ����� �ʹ� �Ƹ��ٿ� �׷� �ʸ� ����ϸ鼭 ������ �� ���ҰŶ� ���ϴ� �� �׷� �ʸ� ����ϸ鼭 ���� �׸� �� ���� ���� �������� �ʹ� �����ؼ� ���� �������� ���ݾ� ��.";

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
		DataInputStream dis = new DataInputStream(in);  // �⺻�� ������ ó���ϴ� ������Ʈ��
		String receiveMsg = "";

		try{  //������ ���Žð��� 3�ʸ� ������ TIME OUT		
			receiveMsg = dis.readUTF();
		}
		catch(SocketTimeoutException ste){
			System.out.println("TIME OUT !!");
			receiveMsg = "timeout";
		}
		return receiveMsg;
	}

	public static void msgSender(String sendMsg) throws IOException, InterruptedException{
		// ������ ��½�Ʈ���� ��´�.
		OutputStream out = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out); // �⺻�� ������ ó���ϴ� ������Ʈ��

		dos.writeUTF(sendMsg);
	}
	public static void intSender(int sendInt) throws IOException{
		// ������ ��½�Ʈ���� ��´�.
		OutputStream out = socket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out); // �⺻�� ������ ó���ϴ� ������Ʈ��

		dos.writeInt(sendInt);
	}

	private static int getMode() throws IOException{ 
		int mode = 0;
		while(mode != 1 && mode != 2){
			System.out.println("1. Stop & Wait ,  2. Go Back N ");
			mode = sc.nextInt();
		}
		intSender(mode); //��带 �����ϰ� Server�� ��� ����ȭ
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
		//�� �������� ũ�⸦ ����ȭ
		sw.setError();//���������� �����߻� ��ų ��ȣ
		syncSWNum();//�� ������ ������� ������ �߻���Ų ��ȣ�� ������ ����ȭ ����

		sw.sendData();
		msgSender(sw.getSendData()); // ù ������ ����

		while(sw.getCount() < maxCount){
			if(sw.recAck(receiver())){ // ack�� ����
				Thread.sleep(200);
				if(sw.sendData()) //���� ���� ����üũ
				{
					msgSender(sw.getSendData()); //������ ������.
				}
			}
			
			else{ //TIMEOUT �߻��� ������
				sw.sendData();//������
				msgSender(sw.getSendData()); //������
			}
			
		}
		System.out.println("Success Send!!!");
		socket.close();
	}

	private static void goBackNSender(String sendData) throws IOException, InterruptedException{
		gbn = new GoBackN(sendData);

		int maxCount = gbn.getDataSize();
		gbn.setError();//���������� �����߻� ��ų ��ȣ
		syncGBNNum();//�� ������ ������� ������ �߻���Ų ��ȣ�� �������� ����ȭ ����

		gbn.sendData();//���� ����
		msgSender(gbn.getSendData());

		Thread t = new Thread(new Client());	
		t.start();

		while(gbn.getCount() < maxCount){
			if(gbn.sendData()){ //DATA �۽� üũ
				msgSender(gbn.getSendData());//������ �����͸� ������.
			}
			else{ //DATA���� ����
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