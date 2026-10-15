import java.util.ArrayList;

public class LicenseService
{
    private ArrayList<SoftwareLicense> issuedLicenses;

    public LicenseService()
    {
        issuedLicenses = new ArrayList<SoftwareLicense>();
    }

    public synchronized SoftwareLicense issueLicense(Student student, Course course)
    {
        SoftwareLicense existingLicense = findLicense(student, course);

        if(existingLicense != null)
        {
            Logger.log("Software license already exists for " + student.getName() + ".");
            return existingLicense;
        }

        String key = "LIC-" + course.getCourseName().replace(" ", "-").toUpperCase()
                + "-" + student.getStudentId();

        SoftwareLicense license = new SoftwareLicense(key, student, course);
        issuedLicenses.add(license);

        Logger.log("Software license issued to " + student.getName()
                + ": " + license.getLicenseKey());

        return license;
    }

    private SoftwareLicense findLicense(Student student, Course course)
    {
        for(SoftwareLicense license : issuedLicenses)
        {
            if(license.getStudent().getStudentId() == student.getStudentId()
                    && license.getCourse().getCourseName().equals(course.getCourseName()))
            {
                return license;
            }
        }

        return null;
    }

    public synchronized int getLicenseCount()
    {
        return issuedLicenses.size();
    }

    public synchronized ArrayList<SoftwareLicense> getIssuedLicenses()
    {
        return new ArrayList<SoftwareLicense>(issuedLicenses);
    }
}
