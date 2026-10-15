public class EnrollmentChecker
{
    //This class is used to ensure that the student meets the requirements of the course
    private static final double MIN_GPA = 2.0;

    public boolean validate(Student student)
    {
        if(student == null)
        {
            Logger.log("Validation Failed: Student is null.");
            return false;
        }

        if(student.getName() == null || student.getName().trim().isEmpty())
        {
            Logger.log("Validation Failed: Name is blank.");
            return false;
        }

        String id = String.valueOf(student.getStudentId());

        if(id.length() != 8)
        {
            Logger.log("Validation Failed: ID must be 8 digits.");
            return false;
        }

        if(!id.startsWith("100"))
        {
            Logger.log("Validation Failed: ID must start with 100.");
            return false;
        }

        if(student.getGpa() < MIN_GPA)
        {
            Logger.log("Validation Failed: GPA below "
                    + MIN_GPA);
            return false;
        }

        return true;
    }
}