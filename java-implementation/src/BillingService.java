import java.util.ArrayList;

public class BillingService
{
    private static final double COURSE_FEE = 300.00;
    private static final double SOFTWARE_FEE = 75.00;

    private ArrayList<Invoice> invoices;

    public BillingService()
    {
        invoices = new ArrayList<Invoice>();
    }

    public synchronized Invoice generateInvoice(Student student, Course course)
    {
        Invoice existingInvoice = findInvoice(student, course);

        if(existingInvoice != null)
        {
            Logger.log("Invoice already exists for " + student.getName() + ".");
            return existingInvoice;
        }

        Invoice invoice = new Invoice(student, course, COURSE_FEE + SOFTWARE_FEE);
        invoices.add(invoice);

        Logger.log("Invoice generated for " + student.getName()
                + " in the amount of $" + invoice.getAmount() + ".");

        return invoice;
    }

    public synchronized boolean processPayment(Student student, Course course)
    {
        Invoice invoice = findInvoice(student, course);

        if(invoice == null)
        {
            Logger.log("Payment failed. No invoice exists for " + student.getName() + ".");
            return false;
        }

        if(invoice.isPaid())
        {
            Logger.log("Payment skipped. Invoice is already paid for " + student.getName() + ".");
            return true;
        }

        invoice.markPaid();
        Logger.log("Payment processed for " + student.getName() + ".");
        return true;
    }

    public synchronized Invoice findInvoice(Student student, Course course)
    {
        for(Invoice invoice : invoices)
        {
            if(invoice.getStudent().getStudentId() == student.getStudentId()
                    && invoice.getCourse().getCourseName().equals(course.getCourseName()))
            {
                return invoice;
            }
        }

        return null;
    }

    public synchronized ArrayList<Invoice> getInvoices()
    {
        return new ArrayList<Invoice>(invoices);
    }
}
