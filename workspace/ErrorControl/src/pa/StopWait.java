package pa;

import java.util.Scanner;

public class StopWait {
	private static final long TIMEOUT = 2000; 

	private String seqNum;
	private String ackNum;
	private String sendData;
	private String []sendDataArr;
	private String recData;

	private int lostSeq;
	private int errorSeq;
	private int lostAck;
	private int count;
	private int dataSize;

	private boolean resendFlag;

	public StopWait(String sendData){
		this.seqNum = "0";
		this.ackNum = "0";
		this.sendData = sendData; 
		this.sendDataArr = sendData.split(" ");
		this.recData = "";//data�� ù��°�� seq �Ǵ� ack�� ����

		this.lostSeq = -1;
		this.errorSeq = -1;
		this.lostAck = -1;
		this.count = 0;
		this.dataSize = sendDataArr.length;

		this.resendFlag = false;
	}

	///////////////////////Receiver��/////////////////////////////////////////////////
	public void recData(String data){

		this.seqNum = data.substring(0,1);//���� data�� 0��°�� seq�� ���
		String temp = data.substring(1);
		
		if(this.errorSeq == count){
			this.errorSeq = -1; //������ȣ�� �ʱ�ȭ
			this.ackNum = "NAK";
			System.out.println("Recv error frame : "+this.seqNum+" data - "+temp);
			return ;
		}
		else if(this.lostAck == count){
			return;
		}
		this.setRecData(this.getRecData() + temp+" ");
		if(seqNum.equals("0")){ 
			this.ackNum = "1";
		}
		else if(seqNum.equals("1")){
			this.ackNum = "0";
		}
		System.out.println("Recv Seq #: "+this.seqNum+" data - "+temp);
	}

	public boolean ackSend(){
		boolean successSend = true;
		if(this.lostAck == count){ //ack�� �Ҿ������
			this.lostAck = -1;
			successSend = false;
			return successSend;
		}
		else if(this.ackNum.equals("NAK")){  //NAK�� �����ϸ� ������ ���������� count�� �ȿø���.
			successSend = true;
		}
		else{ //ack������ �����̸� count++
			this.count++;
			successSend = true;
		}
		System.out.println("Send Ack #: "+this.ackNum);
		return successSend;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////Sender��///////////////////////////////////////////////////////////
	public boolean sendData() throws InterruptedException{
		boolean successSend = true;

		if(!resendFlag){
			setSendData(this.seqNum+sendDataArr[count]);

			if(this.lostSeq == count){//�Ҿ� ������ �Ǵ� �������� ��� false ����
				this.lostSeq = -1; //������ȣ �ʱ�ȭ

				System.out.println("SendSeq#: "+this.seqNum+" SendData - "+this.sendData+" : This PKT will be lost!");
				resendFlag = true;
				successSend = false;
			}
			else if(this.errorSeq == count){//���� seq�� ������
				this.errorSeq = -1;//������ȣ��  �ʱ�ȭ

				System.out.println("Send error MSG : SendData - "+this.sendData);
				resendFlag = true;
				successSend = true;
			}
			else if(this.lostAck == count){ //ack�� �Ҿ������ ����
				this.lostAck= -1;//������ȣ��  �ʱ�ȭ

				System.out.println("This PKT`s Ack will be lost  : SendData - "+this.sendData);
				resendFlag = true;
				successSend = true;
			}
			else{//�Ҿ������ �ʴ°�� count++
				this.count++;
				System.out.println("SendSeq#: "+this.seqNum+" SendData - "+this.sendData);
			}
			return successSend;
		}
		else{
			System.out.println("==Re-Send== : "+this.sendData);
			this.count++;
			resendFlag = !resendFlag;
			return true;
		}
	}
	
	public boolean recAck(String ack){
		if(ack.equals("0") || ack.equals("1")){
			this.ackNum = ack;

			System.out.println("RecvAck#: "+this.ackNum);
			this.seqNum = this.ackNum;
			return true;
		}
		else if(ack.equals("NAK")){ //������ ���
			System.out.println("RECV NAK");
			return false;
		}
		return false;
	}



	//////////////////////////////////////////////////////////////////////

	public void setError(){
		Scanner sc = new Scanner(System.in);
		System.out.print("Error Seq #:");
		this.errorSeq += sc.nextInt();
		System.out.print("Loss Seq #:");
		this.lostSeq += sc.nextInt();
		System.out.print("Loss Ack #:");
		this.lostAck += sc.nextInt();
		sc.close();
	}

	public String getSeqNum(){
		return this.seqNum;
	}

	public void setSeqNum(String seq){
		this.seqNum = seq;
	}

	public String getAckNum(){
		return this.ackNum;
	}

	public void setAckNum(String ack){
		this.seqNum = ack;
	}

	public int getCount() {
		return this.count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getErrorSeq() {
		return errorSeq;
	}

	public void setErrorSeq(int errorSeq) {
		this.errorSeq = errorSeq;
	}

	public int getLostSeq() {
		return lostSeq;
	}

	public void setLostSeq(int lostSeq) {
		this.lostSeq = lostSeq;
	}

	public int getLostAck() {
		return lostAck;
	}

	public void setLostAck(int lostAck) {
		this.lostAck = lostAck;
	}
	public String getSendData() {
		return sendData;
	}
	public void setSendData(String sendData) {
		this.sendData = sendData;
	}
	public int getDataSize() {
		return dataSize;
	}
	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}
	public String getRecData() {
		return recData;
	}
	public void setRecData(String recData) {
		this.recData = recData;
	}
}