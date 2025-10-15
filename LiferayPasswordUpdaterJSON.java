// File: LiferayPasswordUpdaterJSON.java
import java.sql.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.*;

public class LiferayPasswordUpdaterJSON {

    public static void main(String[] args) {
        String configPath = "dbconfig.json";
        if (args.length >= 1) {
            configPath = args[0];
        }

        // 1) Read JSON config (simple, dependency-free parser for 3 keys)
        String dbUrl = null, dbUser = null, dbPass = null;
        try {
            String json = readFileToString(configPath);
            dbUrl = extractJsonString(json, "db_url");
            dbUser = extractJsonString(json, "db_user");
            dbPass = extractJsonString(json, "db_pass");
        } catch (IOException e) {
            System.err.println("Config file error: " + e.getMessage());
            System.err.println("Expected a JSON file with keys: db_url, db_user, db_pass");
            System.exit(2);
        }

        if (dbUrl == null || dbUser == null || dbPass == null) {
            System.err.println("Config file missing required keys (db_url/db_user/db_pass).");
            System.exit(3);
        }

        // 2) Read screenName and new password from terminal
        String screenName = null;
        String newPassword = null;
        try {
            Console console = System.console();
            if (console != null) {
                screenName = console.readLine("Enter screenName: ").trim();
                char[] pwChars = console.readPassword("Enter new password (input hidden): ");
                newPassword = (pwChars == null) ? "" : new String(pwChars);
            } else {
                // fallback for IDEs where System.console() is null
                Scanner scanner = new Scanner(System.in);
                System.out.print("Enter screenName: ");
                screenName = scanner.nextLine().trim();
                System.out.print("Enter new password (will be visible): ");
                newPassword = scanner.nextLine();
            }
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
            System.exit(4);
        }

        if (screenName == null || screenName.isEmpty()) {
            System.err.println("screenName cannot be empty.");
            System.exit(5);
        }

        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            // (Optional) You don't need to explicitly register driver if ojdbc jar is on classpath.
            // Class.forName("oracle.jdbc.OracleDriver"); // alternative if you want explicit load

            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            conn.setAutoCommit(false);

            String selectSql = "SELECT userId, screenName, password_, passwordencrypted, emailAddress FROM user_ WHERE screenName = ?";
            selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setString(1, screenName);
            rs = selectStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("User with screenName='" + screenName + "' not found. Aborting.");
                return;
            }

            long userId = rs.getLong("userId");
            String sn = rs.getString("screenName");
            String oldPassword = rs.getString("password_");
            int oldEnc = rs.getInt("passwordencrypted");
            String email = rs.getString("emailAddress");

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFile = "user_backup_" + sanitizeFileName(screenName) + "_" + timestamp + ".csv";
            try (PrintWriter pw = new PrintWriter(new FileWriter(backupFile))) {
                pw.println("userId,screenName,emailAddress,old_password,passwordencrypted");
                pw.printf("%d,%s,%s,%s,%d%n", userId, sn, email, escapeCsv(oldPassword), oldEnc);
            }
            System.out.println("Backup written to: " + backupFile);

            String updateSql = "UPDATE user_ SET password_ = ?, passwordencrypted = 0 WHERE screenName = ?";
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, newPassword);
            updateStmt.setString(2, screenName);

            int updated = updateStmt.executeUpdate();
            if (updated != 1) {
                System.err.println("Warning: UPDATE affected " + updated + " rows. Rolling back.");
                conn.rollback();
                return;
            }

            conn.commit();
            System.out.println("Update successful. userId=" + userId + ", screenName=" + screenName);
            System.out.println("Note: password_ column now contains the provided value and passwordencrypted set to 0.");
            System.out.println("Important: If Liferay caches or an index needs refresh, restart/refresh portal caches as needed.");

        } catch (SQLException | IOException ex) {
            System.err.println("Error: " + ex.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException r) { System.err.println("Rollback failed: " + r.getMessage()); }
        } finally {
            closeQuiet(rs);
            closeQuiet(selectStmt);
            closeQuiet(updateStmt);
            closeQuiet(conn);
        }
    }

    // --- utility helpers ---
    private static String readFileToString(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    // very small JSON string extractor (assumes simple flat JSON with double-quoted keys)
    private static String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String sanitizeFileName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static void closeQuiet(AutoCloseable ac) {
        if (ac == null) return;
        try { ac.close(); } catch (Exception e) { /* ignore */ }
    }
}
