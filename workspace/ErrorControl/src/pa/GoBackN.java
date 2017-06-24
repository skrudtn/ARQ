package pa;

import java.util.Scanner;
import java.util.Vector;

public class GoBackN {
	private static final int WINDOWSIZE = 7;
	private static final int ACKCOUNT = 3;

	private Vector<String> buffer; // 재전송을 위한 버퍼

	private String sendData;
	private String []sendDataArr;
	private String recData;
	private String ackMsg;

	private int seqNum;
	private int lastFrameSeq;
	private int lastFrameAck;

	private int lostPKT;
	private int errorPKT;
	private int lostAck;
	private int errorAck;

	private int count;
	private int dataSize;
	private int ackCount; // frame을 몇개 받으면 ack를 보낼지 결정할 변수
	private int curWindowSize;

	private boolean resendFlag;
	public boolean rrFlag;

	public GoBackN(String sendData){
		this.buffer = new Vector<String>();
		this.sendData = sendData; 
		this.sendDataArr = sendData.split(" ");
		this.recData = "";//data의 첫번째는 seq 또는 ack로 통일
		this.ackMsg = "";

		this.seqNum = 0;
		this.lastFrameSeq = -1;
		this.lastFrameAck = 0;

		this.lostPKT = -1;
		this.errorPKT = -1;
		this.lostAck = -1;
		this.errorAck = -1;
		this.count = 0;
		this.dataSize = sendDataArr.length;
		this.ackCount = 0; //frame을 2개받으면 ack보내기
		this.curWindowSize = WINDOWSIZE; //최초 windowsize는 최대사이즈

		this.resendFlag = false;
		this.rrFlag = false;
	}

	///////////////////////Receiver쪽/////////////////////////////////////////////////
	public boolean recData(String data){
		if(this.curWindowSize <= 0 || data.equals(" ")){ //받는window크기가 0보다 커야 받을수 있다.
			return false; //받지 않는다.
		}

		//PKT 분해
		int recSeq = Integer.parseInt(data.substring(0,1)); //data의 첫번째는 seq
		String temp = data.substring(1);

		if(recSeq == 1 && temp.equals("RR")){//RR(p bit 1)을 받으면 RR을 바로 보내야 한다.
			this.ackMsg = "RR0";
			System.out.println("RECV #: RR(p bit 1)");
			return true;
		}

		System.out.println("Rec Seq #: "+recSeq+" data - "+temp);

		if(recSeq == (this.lastFrameSeq+1) % (WINDOWSIZE+1)){//정상적인 패킷이 온경우
			this.lastFrameSeq = recSeq;
			this.recData += temp+" ";
			this.ackMsg = "RR";
			this.ackCount++;
			this.count++;
			return true;
		}
		else if(recSeq == (this.lastFrameSeq+2) % (WINDOWSIZE+1)){ //하나를 건너뛰어 온 경우
			System.out.println("Frame Lost!!");
		}
		else{//정상 패킷이 아닌경우
			System.out.println("Damaged Frame!!");
		}
		this.ackMsg = "REJ";
		return true;
	}

	public boolean sendAck() throws InterruptedException{
		int p=0;
		if(this.lostAck == count){//lost ack이면 ack를 보내지 않는다.
			if(p == 0){
				System.out.println("This ACK will be lost!!");
				p++;
			}
			if(ackMsg.equals("RR0")){
				this.lostAck = -1;
			}
			return false;
		}
		if(this.errorAck == count){//error ack이면 
			this.errorAck = -1;
			System.out.println("Send error Ack !!");
			sendData = "9" + "RR";
			return true;
		}

		if(ackMsg.equals("RR0")){ //RR을 받으면 ackMsg를 RR0으로 바꿧다.
			this.curWindowSize = WINDOWSIZE;
			this.lastFrameAck = (this.lastFrameSeq+1) % (WINDOWSIZE+1);
			sendData = Integer.toString(lastFrameAck) + this.ackMsg;
			System.out.println("SendAck #: "+sendData);
			ackCount=0;
			this.ackMsg = "RR";
			return true;
		}
		else if(this.ackCount == ACKCOUNT){ // 패킷3개를 받으면 재전송
			this.addCurWindowSize(ackCount); //현재 receiver쪽 윈도우 사이즈 증가
			this.lastFrameAck = (this.lastFrameSeq+1) % (WINDOWSIZE+1);
			sendData = Integer.toString(lastFrameAck)+this.ackMsg;
			ackCount=0;
			System.out.println("SendAck #: "+sendData);
			return true;
		}
		else if(this.ackMsg.equals("REJ")){ //잘못된 프레임이 왔을때 REJ를 전송한다.
			sendData = Integer.toString((this.lastFrameSeq+1) % (WINDOWSIZE+1))+this.ackMsg;
			ackMsg = "RR";
			System.out.println("SendAck #:" +sendData);
			return true;
		}

		return false;

	}
	//////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////Sender쪽///////////////////////////////////////////////////////////
	public boolean sendData() throws InterruptedException{
		if(this.sendData.equals("1RR")){
			rrFlag=true;
			return true;
		}
		
		if(this.curWindowSize <= 0){ //보내는 window크기가 0보다 커야 보낼수 있다.
			return false;
		}
		
		if(this.ackMsg.substring(0).equals("9")){
			sendData = "1RR";
			rrFlag = true;
			return true;
		}
		if(rrFlag){
			return false;//rr을 보냈으면 send를 하지않고 빠져나간다.
		}

		sendData = Integer.toString(this.seqNum)+sendDataArr[count]; //보낼 데이터 생성
		buffer.add(sendData); // 재전송을 위해 버퍼에 저장

		this.addSeqNum();
		this.addCount();
		this.subCurWindowSize(); //data를보내면 윈도우 사이즈 감소

		if(this.count == this.lostPKT){
			this.lostPKT = -1;
			System.out.println("SendData #: "+ this.sendData+" - This PKT will be lost!");
			return false;
		}
		else if(this.count == this.errorPKT){//PKT이 error를 낼때
			this.errorPKT = -1;
			sendData = "9"+sendData.substring(1); //데이터의 seq를 9	로 바꿔보냄
			System.out.println("SendData #: "+ this.sendData+" - Send error PKT!!");
			return true;
		}
		System.out.println("SendData #: "+ this.sendData);
		return true;
	}	


	public boolean recAck(String ack){
		if(ack.equals("timeout")){
			sendData = "1RR";
			return true;
		}
		if(this.curWindowSize >= WINDOWSIZE || ack.equals("")){ 
			return false;
		}
		
		//ACK 분해
		this.ackMsg = ack;
		int ackNum = Integer.parseInt(ack.substring(0,1)); //ACK의 첫번째는 받을 seq번호
		String temp = ack.substring(1);

		if(ackNum == 9){ //ackNum이 9이면 errorAck
			sendData = "1RR";
		}
		else if(temp.equals("RR")){ //RR을 받으면 다음 프레임을 준비한다.
			this.addCurWindowSize(ACKCOUNT); //
			System.out.println("Recv Ack #: "+ack);
		}
		else if(temp.equals("RR0")){
			this.curWindowSize += buffer.size();
			this.sendData =  Integer.toString(this.seqNum)+sendDataArr[count]; //보낼 데이터 생성
			rrFlag = false;
			System.out.println("Recv Ack #: "+ack + " (p bit 0)");
		}
		else if(temp.equals("REJ")){ //REJ를 받으면 버퍼에서 찾아 다시보낸다.
			for(int i=buffer.size(); i>0; i++){
				if(Integer.parseInt(buffer.elementAt(0).substring(0,1)) == ackNum){
					this.seqNum = ackNum;
					break;
				}
				buffer.remove(0);
			}
			count -= buffer.size();
			curWindowSize += buffer.size();
			System.out.println("Recv Ack #: "+ack);
		}

		this.buffer.clear(); //ack를 받으면 buffer를 비운다.
		return true;
	}
	//////////////////////////////////////////////////////////////////////


	public void setError(){
		Scanner sc = new Scanner(System.in);
		System.out.print("Error frame #:");
		this.errorPKT = sc.nextInt();
		
		System.out.print("Loss frame #:");
		this.lostPKT = sc.nextInt();
		
		System.out.print("Error Ack #:");
		this.errorAck = sc.nextInt();
		
		System.out.print("Loss Ack #:");
		this.lostAck = sc.nextInt();
		
		sc.close();
	}

	public int getSeqNum(){
		return this.seqNum;
	}

	public void setSeqNum(int seq){
		this.seqNum = seq;
	}

	private void addSeqNum() { //seqNum은 0~7
		this.seqNum = (this.seqNum+1) % (WINDOWSIZE+1);

	}
	public int getAckNum(){
		return this.lastFrameAck;
	}

	public void setAckNum(int ack){
		this.seqNum = ack;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	public void addCount(){
		this.count++;
	}

	public int getErrorSeq() {
		return errorPKT;
	}

	public void setErrorSeq(int errorSeq) {
		this.errorPKT = errorSeq;
	}

	public int getLostSeq() {
		return lostPKT;
	}

	public void setLostSeq(int lostSeq) {
		this.lostPKT = lostSeq;
	}

	public int getErrorAck() {
		return errorAck;
	}

	public void setErrorAck(int errorAck) {
		this.errorAck = errorAck;
	}

	public int getLostAck() {
		return lostAck;
	}

	public void setLostAck(int lostAck) {
		this.lostAck = lostAck;
	}

	public int getAckCount() {
		return ackCount;
	}

	public void setAckCount(int ackCount) {
		this.ackCount = ackCount;
	}

	private void addCurWindowSize(int ackCount) {
		this.curWindowSize += ackCount;
	}

	private void subCurWindowSize() {
		this.curWindowSize--;
	}

	public String getSendData() {
		return sendData;
	}

	public void setSendData(String sendData) {
		this.sendData = sendData;
	}

	public String [] getSendDataArr() {
		return sendDataArr;
	}

	public void setSendDataArr(String [] sendDataArr) {
		this.sendDataArr = sendDataArr;
	}

	public String getRecData() {
		return recData;
	}

	public void setRecData(String recData) {
		this.recData = recData;
	}

	public int getDataSize() {
		return dataSize;
	}

	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}
}
