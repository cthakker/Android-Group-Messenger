package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Comparator;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.PriorityQueue;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    String port;
    int seq_no = 0;
    int seq_del = 0;

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        port = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final EditText et = (EditText) findViewById(R.id.editText1);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        Button sendButton =(Button)findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msgtxt = et.getText().toString();
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgtxt,port);
                et.setText("");
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int proposed_priority = 0;
            PriorityQueue<Message> pq = new PriorityQueue<Message>(50, new Comparator<Message>(){
                @Override
                public int compare(Message lhs, Message rhs)
                {
                    if (lhs.getPriority()!= rhs.getPriority()) {
                        return lhs.getPriority() - rhs.getPriority();
                    } else
                    {
                        if(lhs.getPort() > rhs.getPort())
                        {
                            return 1;
                        }
                        else
                        {
                            return -1;
                        }
                    }
                }
            });

            try
            {
                while(true)
                {
                    try
                    {
                        Socket socketin = serverSocket.accept();
                        socketin.setSoTimeout(2000);
                        ObjectInputStream objin = new ObjectInputStream(socketin.getInputStream());
                        Message msg_rec = (Message)objin.readObject();

                        if(msg_rec.getMsgtype().equals("HbTest"))
                        {
                            socketin.close();
                            continue;
                        }

                        if(msg_rec.getMsgtype().equals("Message"))
                        {
                            msg_rec.setPriority(proposed_priority);
                            pq.add(msg_rec);
                            proposed_priority++;
                            ObjectOutputStream objout = new ObjectOutputStream(socketin.getOutputStream());
                            objout.writeObject(msg_rec);
                            objout.close();
                        }

                        if(msg_rec.getMsgtype().equals("Priority"))
                        {
                            if(msg_rec.getPriority() >= proposed_priority )
                            {
                                proposed_priority = Math.max(msg_rec.getPriority(), proposed_priority) + 1;
                            }

                            Message tempmsg = null;
                            for(Message m : pq){
                                if(m.getSequence() == msg_rec.getSequence()) {

                                    if(m.getPort() == msg_rec.getPort()){
                                        tempmsg = m;
                                        break;
                                    }

                                }
                            }

                            pq.remove(tempmsg);
                            pq.add(msg_rec);

                            while(!(pq.isEmpty()))
                            {
                                Message msg = pq.peek();
                                if(msg.isDelivered())
                                {
                                    pq.poll();
                                    publishProgress(msg.getMessage());

                                }
                                else
                                {
                                    try
                                    {
                                        Message msgtest = new Message();
                                        msgtest.setPort(12000);
                                        msgtest.setMessage("FailureHbTest");
                                        msgtest.setMsgtype("HbTest");
                                        msgtest.setPriority(1);
                                        msgtest.setSequence(1);
                                        msgtest.setDelivered(false);
                                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (msg.getPort()*2));
                                        socket.setSoTimeout(2000);
                                        ObjectOutputStream objout = new ObjectOutputStream(socket.getOutputStream());
                                        objout.writeObject(msgtest);
                                        objout.close();
                                        socket.close();
                                    break;
                                    }
                                    catch (Exception e)
                                    {
                                        pq.poll();
                                    }
                                }
                            }
                        }
                        socketin.close();
                    }
                    catch (Exception e)
                    {
                        Log.e(" Inner Exception ", e.toString());
                    }
                }
            }
            catch(Exception e)
            {
                Log.e(" Outer Exception", e.toString());
            }
            return null;
        }

        protected void onProgressUpdate(String... values) {
            /*
             * The following code displays what is received in doInBackground().
             */

            Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

            String msg = values[0];
            TextView txt_view = (TextView)findViewById(R.id.textView1);
            txt_view.append(msg + "\n");
            ContentValues newValues = new ContentValues();

            newValues.put("key", String.valueOf(seq_del));
            newValues.put("value", msg);
            Uri newUri = getContentResolver().insert( providerUri, newValues );
            seq_del++;

        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */


    private  class ClientTask extends AsyncTask<String, String ,Void> {

        @Override
        protected Void doInBackground(String... params) {

            String msg_send_text = params[0];
            Message msg_send = new Message();
            msg_send.setPort(Integer.parseInt(params[1]));
            msg_send.setMessage(msg_send_text);
            msg_send.setMsgtype("Message");
            msg_send.setSequence(seq_no);
            msg_send.setPriority(-1);
            msg_send.setDelivered(false);
            seq_no++;

            String[] remotePort = {"11108","11112","11116","11120","11124"};

            for (int i = 0; i < remotePort.length; i++) {
                try {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort[i]));
                    socket.setSoTimeout(2000);
                    ObjectOutputStream objout = new ObjectOutputStream(socket.getOutputStream());   // Sending Msg
                    objout.writeObject(msg_send);
                    objout.flush();
                    ObjectInputStream objin = new ObjectInputStream(socket.getInputStream());       // Receiving Msg
                    Message received_msg = (Message) objin.readObject();

                    msg_send.setPriority(Math.max(received_msg.getPriority(), msg_send.getPriority()));

                    objout.close();
                    socket.close();
                } catch (Exception e) {

                    Log.e("Exception First caught", e.toString());
                }

            }

            msg_send.setDelivered(true);
            msg_send.setMsgtype("Priority");

            for (int i = 0; i < remotePort.length; i++) {
                try
                {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort[i]));
                    socket.setSoTimeout(2000);
                    ObjectOutputStream objout = new ObjectOutputStream(socket.getOutputStream());   //Sending Msg with Final Priority
                    objout.writeObject(msg_send);
                    objout.close();
                    socket.close();
                } catch (Exception e) {
                    Log.e("Exception final stage", e.toString());
                }

            }
            return null;
        }

    }

}
