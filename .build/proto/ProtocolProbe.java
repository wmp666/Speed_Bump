import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 被测试协议调起后，把收到的命令行参数写到文件，用于验证 %1 的传递是否完整。 */
public class ProtocolProbe {

    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("argc=").append(args.length).append(System.lineSeparator());
        for (int i = 0; i < args.length; i++) {
            sb.append("  arg[").append(i).append("] = [").append(args[i]).append("]")
              .append(System.lineSeparator());
        }
        String out = System.getProperty("probe.out", "protocol_out.txt");
        Path p = Paths.get(out);
        Files.writeString(p, sb.toString());
    }
}
