import java.util.ArrayList;

public class GradeHoldService
{
    private ArrayList<Integer> studentsOnHold;

    public GradeHoldService()
    {
        studentsOnHold = new ArrayList<Integer>();
    }

    public synchronized void placeHold(Student student)
    {
        if(!hasHold(student))
        {
            studentsOnHold.add(student.getStudentId());
            Logger.log("Grade hold placed for " + student.getName() + ".");
        }
    }

    public synchronized void releaseHold(Student student)
    {
        for(int i = 0; i < studentsOnHold.size(); i++)
        {
            if(studentsOnHold.get(i) == student.getStudentId())
            {
                studentsOnHold.remove(i);
                Logger.log("Grade hold released for " + student.getName() + ".");
                return;
            }
        }
    }

    public synchronized boolean hasHold(Student student)
    {
        for(Integer id : studentsOnHold)
        {
            if(id == student.getStudentId())
                return true;
        }

        return false;
    }

    public synchronized int getHoldCount()
    {
        return studentsOnHold.size();
    }
}
