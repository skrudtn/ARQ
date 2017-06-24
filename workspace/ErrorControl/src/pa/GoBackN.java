package pa;

import java.util.Scanner;
import java.util.Vector;

public class GoBackN {
	private static final int WINDOWSIZE = 7;
	private static final int ACKCOUNT = 3;

	private Vector<String> buffer; // �������� ���� ����

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
	private int ackCount; // frame�� � ������ ack�� ������ ������ ����
	private int curWindowSize;

	private boolean resendFlag;
	public boolean rrFlag;

	public GoBackN(String sendData){
		this.buffer = new Vector<String>();
		this.sendData = sendData; 
		this.sendDataArr = sendData.split(" ");
		this.recData = "";//data�� ù��°�� seq �Ǵ� ack�� ����
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
		this.ackCount = 0; //frame�� 2�������� ack������
		this.curWindowSize = WINDOWSIZE; //���� windowsize�� �ִ������

		this.resendFlag = false;
		this.rrFlag = false;
	}

	///////////////////////Receiver��/////////////////////////////////////////////////
	public boolean recData(String data){
		if(this.curWindowSize <= 0 || data.equals(" ")){ //�޴�windowũ�Ⱑ 0���� Ŀ�� ������ �ִ�.
			return false; //���� �ʴ´�.
		}

		//PKT ����
		int recSeq = Integer.parseInt(data.substring(0,1)); //data�� ù��°�� seq
		String temp = data.substring(1);

		if(recSeq == 1 && temp.equals("RR")){//RR(p bit 1)�� ������ RR�� �ٷ� ������ �Ѵ�.
			this.ackMsg = "RR0";
			System.out.println("RECV #: RR(p bit 1)");
			return true;
		}

		System.out.println("Rec Seq #: "+recSeq+" data - "+temp);

		if(recSeq == (this.lastFrameSeq+1) % (WINDOWSIZE+1)){//�������� ��Ŷ�� �°��
			this.lastFrameSeq = recSeq;
			this.recData += temp+" ";
			this.ackMsg = "RR";
			this.ackCount++;
			this.count++;
			return true;
		}
		else if(recSeq == (this.lastFrameSeq+2) % (WINDOWSIZE+1)){ //�ϳ��� �ǳʶپ� �� ���
			System.out.println("Frame Lost!!");
		}
		else{//���� ��Ŷ�� �ƴѰ��
			System.out.println("Damaged Frame!!");
		}
		this.ackMsg = "REJ";
		return true;
	}

	public boolean sendAck() throws InterruptedException{
		int p=0;
		if(this.lostAck == count){//lost ack�̸� ack�� ������ �ʴ´�.
			if(p == 0){
				System.out.println("This ACK will be lost!!");
				p++;
			}
			if(ackMsg.equals("RR0")){
				this.lostAck = -1;
			}
			return false;
		}
		if(this.errorAck == count){//error ack�̸� 
			this.errorAck = -1;
			System.out.println("Send error Ack !!");
			sendData = "9" + "RR";
			return true;
		}

		if(ackMsg.equals("RR0")){ //RR�� ������ ackMsg�� RR0���� �مf��.
			this.curWindowSize = WINDOWSIZE;
			this.lastFrameAck = (this.lastFrameSeq+1) % (WINDOWSIZE+1);
			sendData = Integer.toString(lastFrameAck) + this.ackMsg;
			System.out.println("SendAck #: "+sendData);
			ackCount=0;
			this.ackMsg = "RR";
			return true;
		}
		else if(this.ackCount == ACKCOUNT){ // ��Ŷ3���� ������ ������
			this.addCurWindowSize(ackCount); //���� receiver�� ������ ������ ����
			this.lastFrameAck = (this.lastFrameSeq+1) % (WINDOWSIZE+1);
			sendData = Integer.toString(lastFrameAck)+this.ackMsg;
			ackCount=0;
			System.out.println("SendAck #: "+sendData);
			return true;
		}
		else if(this.ackMsg.equals("REJ")){ //�߸��� �������� ������ REJ�� �����Ѵ�.
			sendData = Integer.toString((this.lastFrameSeq+1) % (WINDOWSIZE+1))+this.ackMsg;
			ackMsg = "RR";
			System.out.println("SendAck #:" +sendData);
			return true;
		}

		return false;

	}
	//////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////Sender��///////////////////////////////////////////////////////////
	public boolean sendData() throws InterruptedException{
		if(this.sendData.equals("1RR")){
			rrFlag=true;
			return true;
		}
		
		if(this.curWindowSize <= 0){ //������ windowũ�Ⱑ 0���� Ŀ�� ������ �ִ�.
			return false;
		}
		
		if(this.ackMsg.substring(0).equals("9")){
			sendData = "1RR";
			rrFlag = true;
			return true;
		}
		if(rrFlag){
			return false;//rr�� �������� send�� �����ʰ� ����������.
		}

		sendData = Integer.toString(this.seqNum)+sendDataArr[count]; //���� ������ ����
		buffer.add(sendData); // �������� ���� ���ۿ� ����

		this.addSeqNum();
		this.addCount();
		this.subCurWindowSize(); //data�������� ������ ������ ����

		if(this.count == this.lostPKT){
			this.lostPKT = -1;
			System.out.println("SendData #: "+ this.sendData+" - This PKT will be lost!");
			return false;
		}
		else if(this.count == this.errorPKT){//PKT�� error�� ����
			this.errorPKT = -1;
			sendData = "9"+sendData.substring(1); //�������� seq�� 9	�� �ٲ㺸��
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
		
		//ACK ����
		this.ackMsg = ack;
		int ackNum = Integer.parseInt(ack.substring(0,1)); //ACK�� ù��°�� ���� seq��ȣ
		String temp = ack.substring(1);

		if(ackNum == 9){ //ackNum�� 9�̸� errorAck
			sendData = "1RR";
		}
		else if(temp.equals("RR")){ //RR�� ������ ���� �������� �غ��Ѵ�.
			this.addCurWindowSize(ACKCOUNT); //
			System.out.println("Recv Ack #: "+ack);
		}
		else if(temp.equals("RR0")){
			this.curWindowSize += buffer.size();
			this.sendData =  Integer.toString(this.seqNum)+sendDataArr[count]; //���� ������ ����
			rrFlag = false;
			System.out.println("Recv Ack #: "+ack + " (p bit 0)");
		}
		else if(temp.equals("REJ")){ //REJ�� ������ ���ۿ��� ã�� �ٽú�����.
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

		this.buffer.clear(); //ack�� ������ buffer�� ����.
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

	private void addSeqNum() { //seqNum�� 0~7
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
