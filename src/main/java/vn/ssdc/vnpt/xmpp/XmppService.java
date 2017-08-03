package vn.ssdc.vnpt.xmpp;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Admin on 5/24/2017.
 */
public class XmppService {
    private static final Logger logger = LoggerFactory.getLogger(XmppService.class);

    static BlockingQueue blockingQueue = new ArrayBlockingQueue(5000);

    public void processMessageXMPP(String strHost, Integer intPort, String strUserName, String strPassword) throws XMPPException, InterruptedException {
        //Deque Thread
        Thread readBq = new Thread() {
            public void run() {
                try {
                    while (true) {
                        if (blockingQueue.size() > 0) {
                            //TODO : PROCESS MESSAGE RECEIVE HERE
                            System.out.println(blockingQueue.take());
                        }
                    }
                } catch (InterruptedException v) {
                    System.out.println(v);
                }
            }
        };
        readBq.start();

        //Listen And Put To Queue
        receiveMessage(strHost, intPort, strUserName, strPassword);
    }

    public void receiveMessage(String strHost, Integer intPort, String strUserName, String strPassword) throws XMPPException, InterruptedException {
        // set piority
        XmppManager xmppManager = new XmppManager(strHost, intPort);
        xmppManager.init();

        // connect to server
        ConnectionConfiguration config = new ConnectionConfiguration(strHost, intPort);
        XMPPConnection connection = new XMPPConnection(config);
        connection.connect();
        connection.login(strUserName, strPassword); // TODO: change user and pass

        // register listeners
        connection.getChatManager().addChatListener(new ChatManagerListener() {
            @Override
            public void chatCreated(Chat chat, boolean b) {
                chat.addMessageListener(new MessageListener() {
                    @Override
                    public void processMessage(Chat chat, Message message) {
//                        System.out.println("Received message: "
//                                + (message != null ? message.getBody() : "NULL"));
                        try {
                            blockingQueue.put((message != null ? message.getBody() : "NULL"));
                        } catch (InterruptedException ex) {
                            logger.error("XMPP ERROR : " + ex);
                        }

                    }
                });
            }
        });
        //while true to keep connection run
        while (true) {
            Thread.sleep(60000L);
        }
    }

    public void sendMessage(String UserNameSend, String PasswordSend, String Host, Integer Port,
                            String BuddyJID, String BuddyName, String Message) throws Exception {
//        String username = "test1";
//        String password = "123456";

        XmppManager xmppManager = new XmppManager(Host, Port);

        xmppManager.init();
        xmppManager.performLogin(UserNameSend, PasswordSend);

//        String buddyJID = "test2";
//        String buddyName = "test2";
        xmppManager.createEntry(BuddyJID, BuddyName);


//        xmppManager.sendMessage("Mai Quoc Khanh", "test2@ump-devtest");
        xmppManager.sendMessage(Message, BuddyJID);

        boolean isRunning = true;

        while (isRunning) {
            Thread.sleep(50);
        }
        xmppManager.destroy();
    }
}
