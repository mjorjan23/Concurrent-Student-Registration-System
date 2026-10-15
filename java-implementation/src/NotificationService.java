public class NotificationService
{
    //This class is just used to notify the student of their final enrollment status
    public void notifyStudent(Student student, Course course, RegistrationResult result)
    {
        if(result == RegistrationResult.ENROLLED)
            Logger.log("NOTIFICATION: " + student.getName() + " has been enrolled in " + course.getCourseName() + ".");
        else if(result == RegistrationResult.WAITLISTED)
            Logger.log("NOTIFICATION: " + student.getName() + " has been added to the waitlist for " + course.getCourseName() + ".");
        else if(result == RegistrationResult.REJECTED)
            Logger.log("NOTIFICATION: " + student.getName() + " was rejected from " + course.getCourseName() + " because the registration request was invalid.");
    }
}
