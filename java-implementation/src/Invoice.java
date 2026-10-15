public class Invoice
{
    private Student student;
    private Course course;
    private double amount;
    private boolean paid;

    public Invoice(Student student, Course course, double amount)
    {
        this.student = student;
        this.course = course;
        this.amount = amount;
        paid = false;
    }

    public Student getStudent()
    {
        return student;
    }

    public Course getCourse()
    {
        return course;
    }

    public double getAmount()
    {
        return amount;
    }

    public boolean isPaid()
    {
        return paid;
    }

    public void markPaid()
    {
        paid = true;
    }
}
