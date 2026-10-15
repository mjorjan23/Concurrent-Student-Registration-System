import java.util.ArrayList;

public class ProfessorApprovalWorkflow
{
    private Teacher professor;
    private LicenseService licenseService;
    private ArrayList<Integer> approvedStudentIds;
    private AppConfig config;

    public ProfessorApprovalWorkflow(Teacher professor, LicenseService licenseService, AppConfig config)
    {
        this.professor = professor;
        this.licenseService = licenseService;
        this.config = config;
        approvedStudentIds = new ArrayList<Integer>();
    }

    public synchronized ApprovalStatus approveStudent(Student student, Course course)
    {
        Logger.log("APPROVAL WORKFLOW STARTED for " + student.getName());

        try
        {
            DelayHelper.pause("Workflow 2 pre-approval", config.getPreApprovalDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return ApprovalStatus.NOT_ENROLLED;
        }

        if(!student.hasCourse(course))
        {
            Logger.log(professor.getName() + " cannot approve " + student.getName()
                    + " because the student is not enrolled in " + course.getCourseName() + ".");
            Logger.log("APPROVAL WORKFLOW ENDED for " + student.getName());
            return ApprovalStatus.NOT_ENROLLED;
        }

        if(hasApprovedStudent(student))
        {
            Logger.log(student.getName() + " has already been approved by " + professor.getName() + ".");
            Logger.log("APPROVAL WORKFLOW ENDED for " + student.getName());
            return ApprovalStatus.DUPLICATE;
        }

        try
        {
            DelayHelper.pause("Inside approval critical section", config.getInsideApprovalDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return ApprovalStatus.NOT_ENROLLED;
        }

        approvedStudentIds.add(student.getStudentId());
        Logger.log(professor.getName() + " approved " + student.getName() + ".");

        licenseService.issueLicense(student, course);

        try
        {
            DelayHelper.pause("Workflow 2 post-approval", config.getPostApprovalDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return ApprovalStatus.NOT_ENROLLED;
        }

        Logger.log("APPROVAL WORKFLOW ENDED for " + student.getName());
        return ApprovalStatus.APPROVED;
    }

    private boolean hasApprovedStudent(Student student)
    {
        for(Integer id : approvedStudentIds)
        {
            if(id == student.getStudentId())
                return true;
        }

        return false;
    }

    public synchronized int getApprovalCount()
    {
        return approvedStudentIds.size();
    }
}
