import java.sql.*;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** One-time bootstrap for a fresh database. Never accepts a password on the command line. */
class SetAdminPassword {
    static String env(String key){String value=System.getenv(key);if(value==null||value.isBlank())throw new IllegalStateException("Missing environment variable: "+key);return value;}
    public static void main(String[] args) throws Exception {
        var console=System.console();if(console==null)throw new IllegalStateException("Run from an interactive terminal.");
        String url=env("HM_DB_URL");if(!url.startsWith("jdbc:mysql:"))throw new IllegalArgumentException("MySQL database required");
        char[] first=console.readPassword("New headquarters admin password (12-64 characters): ");
        char[] second=console.readPassword("Repeat password: ");
        try{
            if(first==null||second==null||first.length<12||first.length>64||!Arrays.equals(first,second))throw new IllegalArgumentException("Password length or confirmation is invalid");
            String hash=new BCryptPasswordEncoder(12).encode(new String(first));
            try(var connection=DriverManager.getConnection(url,env("HM_DB_USER"),env("HM_DB_PASSWORD"));var statement=connection.prepareStatement("UPDATE system_users SET password=? WHERE id=1 AND tenant_id=1 AND username='admin' AND password='!BOOTSTRAP_DISABLED!' AND deleted=0")){
                statement.setString(1,hash);if(statement.executeUpdate()!=1)throw new IllegalStateException("Fresh locked administrator not found; no account changed");
            }
            console.printf("Headquarters administrator initialized. No password or hash was logged.%n");
        } finally {if(first!=null)Arrays.fill(first,'\0');if(second!=null)Arrays.fill(second,'\0');}
    }
}
