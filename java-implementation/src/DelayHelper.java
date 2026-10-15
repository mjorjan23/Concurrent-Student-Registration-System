import java.util.concurrent.ThreadLocalRandom;

public class DelayHelper
{
    public static void pause(String workflowName, int delayMs, boolean randomDelay) throws InterruptedException
    {
        if(delayMs <= 0)
            return;

        int actualDelayMs = delayMs;

        if(randomDelay)
            actualDelayMs = ThreadLocalRandom.current().nextInt(delayMs + 1);

        Logger.log(workflowName + " artificial delay: " + actualDelayMs + " ms");
        Thread.sleep(actualDelayMs);
    }
}
