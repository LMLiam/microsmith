import java.nio.file.Files;
import java.nio.file.Path;

public class Sleep {
    public static void main(String[] args) throws Exception {
        Files.writeString(Path.of(args[0]), String.valueOf(ProcessHandle.current().pid()));
        Thread.sleep(600_000);
    }
}
