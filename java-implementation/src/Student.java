import java.util.ArrayList;

public class Student
{
    private int studentId;
    private String name;
    private double gpa;

    private ArrayList<Course> enrolledCourses;

    public Student(int studentId,
                   String name,
                   double gpa)
    {
        this.studentId = studentId;
        this.name = name;
        this.gpa = gpa;

        enrolledCourses = new ArrayList<Course>();
    }

    public int getStudentId()
    {
        return studentId;
    }

    public void setStudentId(int studentId)
    {
        this.studentId = studentId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public double getGpa()
    {
        return gpa;
    }

    public void setGpa(double gpa)
    {
        this.gpa = gpa;
    }

    public void addCourse(Course course)
    {
        enrolledCourses.add(course);
    }

    public boolean hasCourse(Course course)
    {
        return enrolledCourses.contains(course);
    }

    public ArrayList<Course> getEnrolledCourses()
    {
        return new ArrayList<Course>(enrolledCourses);
    }

    @Override
    public String toString()
    {
        return name +
                " (ID: " + studentId +
                ", GPA: " + gpa + ")";
    }
}