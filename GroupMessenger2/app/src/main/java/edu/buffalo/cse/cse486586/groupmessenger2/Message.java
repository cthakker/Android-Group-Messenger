package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by chintan on 3/12/16.
 */
class Message implements Serializable {
    int port;
    String message;
    String msgtype;
    int seq_no;
    int priority;
    boolean delivered;

    public String getMsgtype(){
        return msgtype;
    }
    public void setMsgtype(String msgtype){
        this.msgtype=msgtype;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int origin_port) {
        this.port = origin_port;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getSequence() {
        return seq_no;
    }

    public void setSequence(int seq_no) {
        this.seq_no = seq_no;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }


}
