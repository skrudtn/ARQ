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
		this.recData = "";//data의 첫번째는 seq 또는 ack로 통일

		this.lostSeq = -1;
		this.errorSeq = -1;
		this.lostAck = -1;
		this.count = 0;
		this.dataSize = sendDataArr.length;

		this.resendFlag = false;
	}

	///////////////////////Receiver쪽/////////////////////////////////////////////////
	public void recData(String data){

		this.seqNum = data.substring(0,1);//받은 data의 0번째는 seq로 약속
		String temp = data.substring(1);
		
		if(this.errorSeq == count){
			this.errorSeq = -1; //에러번호를 초기화
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
		if(this.lostAck == count){ //ack를 잃어버리면
			this.lostAck = -1;
			successSend = false;
			return successSend;
		}
		else if(this.ackNum.equals("NAK")){  //NAK를 전송하면 전송은 성공이지만 count는 안올린다.
			successSend = true;
		}
		else{ //ack전송이 정상이면 count++
			this.count++;
			successSend = true;
		}
		System.out.println("Send Ack #: "+this.ackNum);
		return successSend;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////Sender쪽///////////////////////////////////////////////////////////
	public boolean sendData() throws InterruptedException{
		boolean successSend = true;

		if(!resendFlag){
			setSendData(this.seqNum+sendDataArr[count]);

			if(this.lostSeq == count){//잃어 버려야 되는 프레임일 경우 false 리턴
				this.lostSeq = -1; //오류번호 초기화

				System.out.println("SendSeq#: "+this.seqNum+" SendData - "+this.sendData+" : This PKT will be lost!");
				resendFlag = true;
				successSend = false;
			}
			else if(this.errorSeq == count){//에러 seq를 보낼때
				this.errorSeq = -1;//에러번호를  초기화

				System.out.println("Send error MSG : SendData - "+this.sendData);
				resendFlag = true;
				successSend = true;
			}
			else if(this.lostAck == count){ //ack를 잃어버리는 오류
				this.lostAck= -1;//에러번호를  초기화

				System.out.println("This PKT`s Ack will be lost  : SendData - "+this.sendData);
				resendFlag = true;
				successSend = true;
			}
			else{//잃어버리지 않는경우 count++
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
		else if(ack.equals("NAK")){ //에러값 통신
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