public class SoftwareLicense
{
    private String licenseKey;
    private Student student;
    private Course course;

    public SoftwareLicense(String licenseKey, Student student, Course course)
    {
        this.licenseKey = licenseKey;
        this.student = student;
        this.course = course;
    }

    public String getLicenseKey()
    {
        return licenseKey;
    }

    public Student getStudent()
    {
        return student;
    }

    public Course getCourse()
    {
        return course;
    }
}
