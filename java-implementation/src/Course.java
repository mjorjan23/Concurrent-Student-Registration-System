import java.util.ArrayList;

public class Course
{
    private final int courseId;
    private final String courseName;
    private final Teacher teacher;
    private final int capacity;

    // Course owns only enrollment state. Waitlist state is managed by WaitList.
    private final ArrayList<Student> enrolledStudents;

    public Course(int courseId, String courseName, Teacher teacher, int capacity)
    {
        this.courseId = courseId;
        this.courseName = courseName;
        this.teacher = teacher;
        this.capacity = capacity;
        enrolledStudents = new ArrayList<Student>();
    }

    // Safe enrollment is the only enrollment path used by A2.
    // This protects Course's enrollment invariant:
    // enrollment count <= capacity and one enrollment per student ID.
    public synchronized EnrollmentStatus enrollStudent(Student student)
    {
        return enrollStudent(student, 0, false);
    }

    public synchronized EnrollmentStatus enrollStudent(Student student, int insideEnrollmentDelayMs, boolean randomDelay)
    {
        Logger.log(Thread.currentThread().getName() + " attempting to enroll " + student.getName());

        // Simulate a slow database/network operation while holding the lock.
        // Every thread that enters enrollment must wait here.
        try
        {
            Logger.log(student.getName() + " ENTERED enrollment critical section.");

            DelayHelper.pause(
                    "Inside Course enrollment critical section",
                    insideEnrollmentDelayMs,
                    randomDelay);

            Logger.log(student.getName() + " EXITING enrollment critical section.");
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return EnrollmentStatus.FULL;
        }

        if(hasStudent(student))
        {
            Logger.log(student.getName() + " is already enrolled.");
            return EnrollmentStatus.DUPLICATE;
        }

        if(!hasSeatAvailable())
        {
            Logger.log("Course full. " + student.getName() + " should be handled by the waitlist workflow.");
            return EnrollmentStatus.FULL;
        }

        enrolledStudents.add(student);
        student.addCourse(this);

        Logger.log(student.getName() + " enrolled successfully.");
        Logger.log("Enrollment Count: " + getEnrollmentCount());

        if(overEnrolled())
            Logger.log("OVER-ENROLLED!");
        else
            Logger.log("Student added with no issue.");

        return EnrollmentStatus.ENROLLED;
    }

    private boolean hasSeatAvailable()
    {
        return enrolledStudents.size() < capacity;
    }

    // Checks by student ID instead of object reference.
    private boolean hasStudent(Student student)
    {
        for(Student enrolledStudent : enrolledStudents)
        {
            if(enrolledStudent.getStudentId() == student.getStudentId())
                return true;
        }

        return false;
    }

    public synchronized int getEnrollmentCount()
    {
        return enrolledStudents.size();
    }

    public int getCapacity()
    {
        return capacity;
    }

    public synchronized ArrayList<Student> getEnrolledStudents()
    {
        return new ArrayList<Student>(enrolledStudents);
    }

    public int getCourseId()
    {
        return courseId;
    }

    public String getCourseName()
    {
        return courseName;
    }

    public Teacher getTeacher()
    {
        return teacher;
    }

    public synchronized boolean overEnrolled()
    {
        return enrolledStudents.size() > capacity;
    }
}
