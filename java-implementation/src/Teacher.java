public class Teacher
{
    private int teacherId;
    private String name;

    public Teacher(int teacherId, String name)
    {
        this.teacherId = teacherId;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public int getTeacherId()
    {
        return teacherId;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setTeacherId(int teacherId)
    {
        this.teacherId = teacherId;
    }
}