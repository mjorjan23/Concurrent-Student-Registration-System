public class ApprovalEvent
{
    public static final ApprovalEvent POISON_PILL = new ApprovalEvent(null, null, false);

    private final Student student;
    private final Course course;
    private final boolean studentPays;

    public ApprovalEvent(Student student, Course course, boolean studentPays)
    {
        this.student = student;
        this.course = course;
        this.studentPays = studentPays;
    }

    public Student getStudent()
    {
        return student;
    }

    public Course getCourse()
    {
        return course;
    }

    public boolean doesStudentPay()
    {
        return studentPays;
    }
}
