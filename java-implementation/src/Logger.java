import java.time.LocalDateTime;

public class Logger
{
    public static synchronized void log(String message)
    {
        System.out.println( "|" + LocalDateTime.now()+ "|:   " + message);
    }
}