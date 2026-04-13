import java.util.*;
import java.io.*;

public class AnotherBadExample {
    
    Vector items = new Vector();
    
    public void processData() {
        String data = "";
        for (int i = 0; i < 100; i++) {
            data = data + "value" + i;
        }
        
        if (items.size() == 0) {
            return;
        }
        
        Enumeration e = items.elements();
        while (e.hasMoreElements()) {
            Object item = e.nextElement();
            System.out.println(item.toString());
        }
    }
    
    public void badSwitchExample(int type) {
        switch (type) {
            case 1:
                System.out.println("Type 1");
                break;
            case 2:
                System.out.println("Type 2");
                break;
        }
    }
    
    public Boolean checkCondition() {
        return new Boolean(true);
    }
}