import java.nio.file.Files;
import java.nio.file.Path;

public class Detach {
    public static void main(String[] args) throws Exception {
        if (args.length > 2) {
            Files.writeString(Path.of(args[1]), "started");
            Thread.sleep(20_000);
            return;
        }
        String java = ProcessHandle.current().info().command().get();
        new ProcessBuilder(java, args[0], args[0], args[1], "child").inheritIO().start();
    }
}
