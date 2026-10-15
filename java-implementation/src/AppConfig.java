import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig
{
    private final int teacherId;
    private final String teacherName;
    private final int courseId;
    private final String courseName;
    private final int capacity;
    private final int approvalWorkers;
    private final int billingWorkers;
    private final String studentFile;
    private final int registrationDelayMs;
    private final int approvalDelayMs;
    private final int billingDelayMs;
    private final boolean randomDelay;
    private final int repeatStudentSubmissions;
    private final String asyncMode;
    private final boolean failApproval;
    private final boolean failBilling;
    private final int failAfterEvents;
    private final String failStudent;
    private final int maxRetries;
    private final boolean concurrentDuplicateRace;

    // Optional realistic-latency testing hooks. Defaults are 0, so old configs still work.
    private final int preValidationDelayMs;
    private final int postValidationDelayMs;
    private final int preEnrollmentDelayMs;
    private final int insideEnrollmentDelayMs;
    private final int postEnrollmentDelayMs;
    private final int preQueuePublishDelayMs;

    private final int preApprovalDelayMs;
    private final int insideApprovalDelayMs;
    private final int postApprovalDelayMs;
    private final int preBillingPublishDelayMs;

    private final int preBillingDelayMs;
    private final int insideBillingDelayMs;
    private final int postBillingDelayMs;

    private AppConfig(int teacherId,
                      String teacherName,
                      int courseId,
                      String courseName,
                      int capacity,
                      int approvalWorkers,
                      int billingWorkers,
                      String studentFile,
                      int registrationDelayMs,
                      int approvalDelayMs,
                      int billingDelayMs,
                      boolean randomDelay,
                      int repeatStudentSubmissions,
                      String asyncMode,
                      boolean failApproval,
                      boolean failBilling,
                      int failAfterEvents,
                      String failStudent,
                      int maxRetries,
                      boolean concurrentDuplicateRace,
                      int preValidationDelayMs,
                      int postValidationDelayMs,
                      int preEnrollmentDelayMs,
                      int insideEnrollmentDelayMs,
                      int postEnrollmentDelayMs,
                      int preQueuePublishDelayMs,
                      int preApprovalDelayMs,
                      int insideApprovalDelayMs,
                      int postApprovalDelayMs,
                      int preBillingPublishDelayMs,
                      int preBillingDelayMs,
                      int insideBillingDelayMs,
                      int postBillingDelayMs)
    {
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.capacity = capacity;
        this.approvalWorkers = approvalWorkers;
        this.billingWorkers = billingWorkers;
        this.studentFile = studentFile;
        this.registrationDelayMs = registrationDelayMs;
        this.approvalDelayMs = approvalDelayMs;
        this.billingDelayMs = billingDelayMs;
        this.randomDelay = randomDelay;
        this.repeatStudentSubmissions = repeatStudentSubmissions;
        this.asyncMode = asyncMode;
        this.failApproval = failApproval;
        this.failBilling = failBilling;
        this.failAfterEvents = failAfterEvents;
        this.failStudent = failStudent;
        this.maxRetries = maxRetries;
        this.concurrentDuplicateRace = concurrentDuplicateRace;

        this.preValidationDelayMs = preValidationDelayMs;
        this.postValidationDelayMs = postValidationDelayMs;
        this.preEnrollmentDelayMs = preEnrollmentDelayMs;
        this.insideEnrollmentDelayMs = insideEnrollmentDelayMs;
        this.postEnrollmentDelayMs = postEnrollmentDelayMs;
        this.preQueuePublishDelayMs = preQueuePublishDelayMs;

        this.preApprovalDelayMs = preApprovalDelayMs;
        this.insideApprovalDelayMs = insideApprovalDelayMs;
        this.postApprovalDelayMs = postApprovalDelayMs;
        this.preBillingPublishDelayMs = preBillingPublishDelayMs;

        this.preBillingDelayMs = preBillingDelayMs;
        this.insideBillingDelayMs = insideBillingDelayMs;
        this.postBillingDelayMs = postBillingDelayMs;
    }

    public static AppConfig load(String configFile)
    {
        Properties properties = new Properties();

        try(FileInputStream input = new FileInputStream(configFile))
        {
            properties.load(input);
        }
        catch(IOException e)
        {
            throw new RuntimeException("Could not load config file: " + configFile, e);
        }

        return new AppConfig(
                readInt(properties, "teacherId"),
                readString(properties, "teacherName"),
                readInt(properties, "courseId"),
                readString(properties, "courseName"),
                readInt(properties, "capacity"),
                readInt(properties, "approvalWorkers"),
                readInt(properties, "billingWorkers"),
                readString(properties, "studentFile"),
                readOptionalInt(properties, "registrationDelayMs", 0),
                readOptionalInt(properties, "approvalDelayMs", 0),
                readOptionalInt(properties, "billingDelayMs", 0),
                readOptionalBoolean(properties, "randomDelay", false),
                readOptionalInt(properties, "repeatStudentSubmissions", 1),
                readOptionalString(properties, "async.mode", "safe"),
                readOptionalBooleanAlias(properties, "failApproval", "FAIL_APPROVAL", false),
                readOptionalBooleanAlias(properties, "failBilling", "FAIL_BILLING", false),
                readOptionalIntAllowNegativeOneAlias(properties, "failAfterEvents", "TIMEOUT_AFTER", -1),
                readOptionalStringAlias(properties, "failStudent", "FAIL_STUDENT", ""),
                readOptionalIntAlias(properties, "maxRetries", "MAX_RETRIES", 3),
                readOptionalBoolean(properties, "concurrentDuplicateRace", false),

                readOptionalInt(properties, "preValidationDelayMs", 0),
                readOptionalInt(properties, "postValidationDelayMs", 0),
                readOptionalInt(properties, "preEnrollmentDelayMs", 0),
                readOptionalInt(properties, "insideEnrollmentDelayMs", 0),
                readOptionalInt(properties, "postEnrollmentDelayMs", 0),
                readOptionalInt(properties, "preQueuePublishDelayMs", 0),

                readOptionalInt(properties, "preApprovalDelayMs", 0),
                readOptionalInt(properties, "insideApprovalDelayMs", 0),
                readOptionalInt(properties, "postApprovalDelayMs", 0),
                readOptionalInt(properties, "preBillingPublishDelayMs", 0),

                readOptionalInt(properties, "preBillingDelayMs", 0),
                readOptionalInt(properties, "insideBillingDelayMs", 0),
                readOptionalInt(properties, "postBillingDelayMs", 0)
        );
    }


    private static String firstPresent(Properties properties, String preferredKey, String legacyKey)
    {
        String value = properties.getProperty(preferredKey);
        if(value == null || value.trim().isEmpty())
            value = properties.getProperty(legacyKey);
        return value;
    }

    private static boolean readOptionalBooleanAlias(Properties properties, String preferredKey,
                                                    String legacyKey, boolean defaultValue)
    {
        String value = firstPresent(properties, preferredKey, legacyKey);
        return value == null || value.trim().isEmpty() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private static String readOptionalStringAlias(Properties properties, String preferredKey,
                                                  String legacyKey, String defaultValue)
    {
        String value = firstPresent(properties, preferredKey, legacyKey);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static int readOptionalIntAlias(Properties properties, String preferredKey,
                                            String legacyKey, int defaultValue)
    {
        String value = firstPresent(properties, preferredKey, legacyKey);
        if(value == null || value.trim().isEmpty())
            return defaultValue;
        try
        {
            int parsedValue = Integer.parseInt(value.trim());
            if(parsedValue < 0)
                throw new RuntimeException("Config value cannot be negative: " + preferredKey);
            return parsedValue;
        }
        catch(NumberFormatException e)
        {
            throw new RuntimeException("Config value must be an integer: " + preferredKey, e);
        }
    }

    private static int readOptionalIntAllowNegativeOneAlias(Properties properties, String preferredKey,
                                                             String legacyKey, int defaultValue)
    {
        String value = firstPresent(properties, preferredKey, legacyKey);
        if(value == null || value.trim().isEmpty())
            return defaultValue;
        try
        {
            int parsedValue = Integer.parseInt(value.trim());
            if(parsedValue < -1)
                throw new RuntimeException("Config value cannot be less than -1: " + preferredKey);
            return parsedValue;
        }
        catch(NumberFormatException e)
        {
            throw new RuntimeException("Config value must be an integer: " + preferredKey, e);
        }
    }

    private static String readString(Properties properties, String key)
    {
        String value = properties.getProperty(key);

        if(value == null || value.trim().isEmpty())
            throw new RuntimeException("Missing required config value: " + key);

        return value.trim();
    }

    private static int readInt(Properties properties, String key)
    {
        try
        {
            return Integer.parseInt(readString(properties, key));
        }
        catch(NumberFormatException e)
        {
            throw new RuntimeException("Config value must be an integer: " + key, e);
        }
    }

    private static int readOptionalInt(Properties properties, String key, int defaultValue)
    {
        String value = properties.getProperty(key);

        if(value == null || value.trim().isEmpty())
            return defaultValue;

        try
        {
            int parsedValue = Integer.parseInt(value.trim());

            if(parsedValue < 0)
                throw new RuntimeException("Config value cannot be negative: " + key);

            return parsedValue;
        }
        catch(NumberFormatException e)
        {
            throw new RuntimeException("Config value must be an integer: " + key, e);
        }
    }


    private static int readOptionalIntAllowNegativeOne(Properties properties, String key, int defaultValue)
    {
        String value = properties.getProperty(key);

        if(value == null || value.trim().isEmpty())
            return defaultValue;

        try
        {
            int parsedValue = Integer.parseInt(value.trim());
            if(parsedValue < -1)
                throw new RuntimeException("Config value cannot be less than -1: " + key);
            return parsedValue;
        }
        catch(NumberFormatException e)
        {
            throw new RuntimeException("Config value must be an integer: " + key, e);
        }
    }

    private static String readOptionalString(Properties properties, String key, String defaultValue)
    {
        String value = properties.getProperty(key);

        if(value == null || value.trim().isEmpty())
            return defaultValue;

        return value.trim();
    }

    private static boolean readOptionalBoolean(Properties properties, String key, boolean defaultValue)
    {
        String value = properties.getProperty(key);

        if(value == null || value.trim().isEmpty())
            return defaultValue;

        return Boolean.parseBoolean(value.trim());
    }

    public int getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public int getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public int getCapacity() { return capacity; }
    public int getApprovalWorkers() { return approvalWorkers; }
    public int getBillingWorkers() { return billingWorkers; }
    public String getStudentFile() { return studentFile; }
    public int getRegistrationDelayMs() { return registrationDelayMs; }
    public int getApprovalDelayMs() { return approvalDelayMs; }
    public int getBillingDelayMs() { return billingDelayMs; }
    public boolean isRandomDelay() { return randomDelay; }
    public String getAsyncMode() { return asyncMode; }
    public boolean isFailApproval() { return failApproval; }
    public boolean isFailBilling() { return failBilling; }
    public int getFailAfterEvents() { return failAfterEvents; }
    public int getTimeoutAfter() { return failAfterEvents; }
    public String getFailStudent() { return failStudent; }
    public int getMaxRetries() { return maxRetries; }
    public boolean isConcurrentDuplicateRace() { return concurrentDuplicateRace; }

    public int getRepeatStudentSubmissions()
    {
        if(repeatStudentSubmissions < 1)
            throw new RuntimeException("repeatStudentSubmissions must be at least 1.");

        return repeatStudentSubmissions;
    }

    public int getPreValidationDelayMs() { return preValidationDelayMs; }
    public int getPostValidationDelayMs() { return postValidationDelayMs; }
    public int getPreEnrollmentDelayMs() { return preEnrollmentDelayMs; }
    public int getInsideEnrollmentDelayMs() { return insideEnrollmentDelayMs; }
    public int getPostEnrollmentDelayMs() { return postEnrollmentDelayMs; }
    public int getPreQueuePublishDelayMs() { return preQueuePublishDelayMs; }

    public int getPreApprovalDelayMs() { return preApprovalDelayMs; }
    public int getInsideApprovalDelayMs() { return insideApprovalDelayMs; }
    public int getPostApprovalDelayMs() { return postApprovalDelayMs; }
    public int getPreBillingPublishDelayMs() { return preBillingPublishDelayMs; }

    public int getPreBillingDelayMs() { return preBillingDelayMs; }
    public int getInsideBillingDelayMs() { return insideBillingDelayMs; }
    public int getPostBillingDelayMs() { return postBillingDelayMs; }
}
