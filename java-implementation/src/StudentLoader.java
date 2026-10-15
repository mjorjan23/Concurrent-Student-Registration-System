import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class StudentLoader
{
    public static ArrayList<StudentInput> load(String studentFile)
    {
        ArrayList<StudentInput> students = new ArrayList<StudentInput>();

        try(BufferedReader reader = new BufferedReader(new FileReader(studentFile)))
        {
            String line = reader.readLine();
            int lineNumber = 1;

            while((line = reader.readLine()) != null)
            {
                lineNumber++;

                if(line.trim().isEmpty())
                    continue;

                String[] parts = line.split(",");

                if(parts.length != 4)
                    throw new RuntimeException("Invalid student row at line " + lineNumber + ": " + line);

                int studentId = Integer.parseInt(parts[0].trim());
                String name = parts[1].trim();
                double gpa = Double.parseDouble(parts[2].trim());
                boolean pays = Boolean.parseBoolean(parts[3].trim());

                students.add(new StudentInput(new Student(studentId, name, gpa), pays));
            }
        }
        catch(IOException e)
        {
            throw new RuntimeException("Could not load student file: " + studentFile, e);
        }
        catch(NumberFormatException e)
        {
            throw new RuntimeException("Student file contains an invalid number: " + studentFile, e);
        }

        return students;
    }
}
