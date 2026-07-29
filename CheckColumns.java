import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckColumns {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:dm://localhost:5238/BEMS";
        String username = "sysdba";
        String password = "Liming@2026";

        Class.forName("dm.jdbc.driver.DmDriver");
        Connection conn = DriverManager.getConnection(url, username, password);
        Statement stmt = conn.createStatement();
        
        String sql = "SELECT COLUMN_NAME FROM SYS.DBA_TAB_COLUMNS WHERE TABLE_NAME = 'DEVICE' AND COLUMN_NAME IN ('LINE_NAME', 'LOOP_NO', 'MANUFACTURER', 'DEVICE_MODEL', 'INSTALL_DATE')";
        ResultSet rs = stmt.executeQuery(sql);
        
        System.out.println("Device表中已存在的列：");
        while (rs.next()) {
            System.out.println("  - " + rs.getString(1));
        }
        
        rs.close();
        stmt.close();
        conn.close();
    }
}
