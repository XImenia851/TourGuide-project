package gpsUtilService;

import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // GpsUtil formats/re-parses doubles internally using the JVM's default
        // locale. On a French-locale machine that produces "-152,530476"
        // (comma decimal separator), which Double.parseDouble then rejects.
        // Forcing US locale here avoids the crash regardless of the OS locale.
        Locale.setDefault(Locale.US);
        SpringApplication.run(Application.class, args);
    }

}