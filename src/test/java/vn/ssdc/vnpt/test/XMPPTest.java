package vn.ssdc.vnpt.test;

import com.google.gson.JsonObject;
import org.jivesoftware.smack.XMPPException;
import vn.ssdc.vnpt.xmpp.XmppService;

/**
 * Created by Admin on 5/24/2017.
 */
public class XMPPTest {
    public static void main(String[] args) {
        JsonObject jo = new JsonObject();
        jo.addProperty("eventTime","2017-06-05T01:00:00");
        jo.addProperty("deviceId","a06518 96318REF_P300 VNPT00a532c2 FW1");
        jo.addProperty("sessionId","ssid1");
        jo.addProperty("type","inform");
        jo.addProperty("detail","CONFIGURATION_FAIL");
        jo.addProperty("tag","tags = Hà Nội , tags = Hải Phòng , tags = Thanh Xuân}]");
        System.out.println(jo);


//        //Test xmpp
//        try {
//            XmppService xmppService = new XmppService();
//            //xmppService.processMessageXMPP("10.84.20.138", 5222, "test1", "123456");
//            xmppService.sendMessage("test1","123456","10.84.20.138",5222,
//                    "test2@ump-devtest" , "test2", "Test Connection");
//        } catch (XMPPException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
