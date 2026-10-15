public class StudentInput
{
    private final Student student;
    private final boolean pays;

    public StudentInput(Student student, boolean pays)
    {
        this.student = student;
        this.pays = pays;
    }

    public Student getStudent()
    {
        return student;
    }

    public boolean getPays()
    {
        return pays;
    }
}
