import java.util.ArrayList;

public class WaitList
{
    // WaitList owns only waitlist state.
    private final ArrayList<Student> students;

    public WaitList()
    {
        students = new ArrayList<Student>();
    }

    public synchronized boolean addStudent(Student student)
    {
        if(!hasStudent(student))
        {
            students.add(student);
            Logger.log(student.getName() + " added to the waitlist.");
            return true;
        }
        else
        {
            Logger.log(student.getName() + " is already on the waitlist.");
            return false;
        }
    }

    // Checks by student ID instead of object reference.
    private boolean hasStudent(Student student)
    {
        for(Student waitListedStudent : students)
        {
            if(waitListedStudent.getStudentId() == student.getStudentId())
                return true;
        }
        return false;
    }

    public synchronized int size()
    {
        return students.size();
    }

    public synchronized ArrayList<Student> getStudents()
    {
        return new ArrayList<Student>(students);
    }
}
