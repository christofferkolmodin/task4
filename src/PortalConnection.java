
import java.sql.*; // JDBC stuff.
import java.util.Properties;
public class PortalConnection {
    // Set this to e.g. "portal" if you have created a database named portal
// Leave it blank to use the default database of your database user
    static final String DBNAME = "";
    // For connecting to the portal database on your local machine
    static final String DATABASE = "jdbc:postgresql://localhost/"+DBNAME;
    static final String USERNAME = "postgres";
    static final String PASSWORD = "postgres";
    // For connecting to the chalmers database server (from inside chalmers)
    // static final String DATABASE = "jdbc:postgresql://brage.ita.chalmers.se/";
    // static final String USERNAME = "tda357_nnn";
    // static final String PASSWORD = "yourPasswordGoesHere";
    // This is the JDBC connection object you will be using in your methods.
    private Connection conn;
    public PortalConnection() throws SQLException, ClassNotFoundException {
        this(DATABASE, USERNAME, PASSWORD);
    }
    // Initializes the connection, no need to change anything here
    public PortalConnection(String db, String user, String pwd) throws
            SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pwd);
        conn = DriverManager.getConnection(db, props);
    }

    // Register a student on a course, returns a tiny JSON document (as a String)
    public String register(String student, String courseCode) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Registrations VALUES(?, ?)");) {
            ps.setString(1, student);
            ps.setString(2, courseCode);
            System.out.println("Query: " + ps);
            int rowsAffected = ps.executeUpdate();
            System.out.println(rowsAffected);

            if (rowsAffected > 0) {
                return "{\"success\":true}";
            } else {
                return "{\"success\":false, \"error\":\"Registration failed.\"}";
            }
        }
        catch (SQLException e) {
            return "{\"success\":false, \"error\":\"" + getError(e) + "\"}";
        }
    }

    // Unregister a student from a course, returns a tiny JSON document (as a String)
    public String unregister(String student, String courseCode){
        String query = "DELETE FROM Registrations WHERE student='"+student+
                "' AND course='"+courseCode+"'";
        try (Statement s = conn.createStatement();){
            int rowsAffected = s.executeUpdate(query);
            System.out.println("Deleted "+rowsAffected+" registrations.");

            if (rowsAffected > 0) {
                return "{\"success\":true}";
            } else {
                return "{\"success\":false, \"error\":\" Failed to unregister student.\"}";
            }
//        try (PreparedStatement ps = conn.prepareStatement(
//                "DELETE FROM REGISTRATIONS WHERE student=? AND course=?");) {
//            ps.setString(1, student);
//            ps.setString(2, courseCode);
//
//            System.out.println("Query: " + ps);
//
//            int rowsAffected = ps.executeUpdate();
//            System.out.println("Number of students deleted: " + rowsAffected);
//
//            if (rowsAffected > 0) {
//                return "{\"success\":true}";
//            } else {
//                return "{\"success\":false, \"error\":\" Failed to unregister student.\"}";
//            }
        }
        catch (SQLException e) {
                return "{\"success\":false, \"error\":\" Failed to unregister student : (\"}";
            }
        }

        // Return a JSON document containing lots of information about a student, it
        //    should validate against the schema found in information_schema.json
        public String getInfo(String student) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT jsonb_build_object(" +
                            "'student',sid," +
                            "'name',sname," +
                            "'login',login," +
                            "'program',program," +
                            "'branch',branch," +
                            "'finished',(SELECT COALESCE(jsonb_agg(jsonb_build_object(" +
                                    "'course',courseName," +
                                    "'code',course," +
                                    "'credits',credits," +
                                    "'grade',grade)" +
                                    "),'[]') FROM FinishedCourses WHERE student = BI.sid)," +
                            "'registered',(SELECT COALESCE(jsonb_agg(jsonb_build_object(" +
                                    "'course',cname," +
                                    "'code',course," +
                                    "'status',status," +
                                    "'position',(SELECT position FROM WaitingList WL WHERE WL.student=R.student AND course=R.course))" +
                                    "),'[]')" +
                                    "FROM Registrations AS R, Courses WHERE R.student = BI.sid AND R.course = code)," +
                            "'seminarCourses',seminarCourses," +
                            "'mathCredits',mathCredits," +
                            "'totalCredits',totalCredits," +
                            "'canGraduate',qualified" +
                            ") AS jsondata FROM BasicInformation BI, PathToGraduation PTG WHERE BI.sid=PTG.student AND BI.sid=?;");) {

                st.setString(1, student);
                ResultSet rs = st.executeQuery();
                if (rs.next())
                    return rs.getString("jsondata");
                else
                    return "{\"student\":\"does not exist :(\"}";

            }
            catch (SQLException e) {
                return "{\"success\":false, \"error\":\"" + getError(e) + "\"}";
            }
        }

        // This is a hack to turn an SQLException into a JSON string error message. No
        // need to change.
        public static String getError(SQLException e){
            String message = e.getMessage();
            int ix = message.indexOf('\n');
            if (ix > 0) message = message.substring(0, ix);
            message = message.replace("\"","\\\"");
            return message;
        }
    }
