import com.chatapp.server.dao.MessageDAO;
import com.chatapp.server.db.SQLiteConnection;
import com.chatapp.shared.Message;
import java.util.List;

public class TestDB {
    public static void main(String[] args) {
        try {
            SQLiteConnection.initialize();
            List<Message> hist = MessageDAO.getChatHistory("moi3", "group", true);
            System.out.println("History size: " + hist.size());
            for (Message m : hist) {
                System.out.println(m.getSender() + " -> " + m.getReceiver() + ": " + m.getContent());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
