package aksw.org.sdw.importer.avro;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Test {
    public static void main(String[] args) {
        AtomicReference<String> lol = new AtomicReference<String>();
        lol.set("foo");
        change(lol);
        System.out.println(lol);

    }

    public static void change(AtomicReference<String> lol) {
        AtomicReference<String> olo = lol;
        olo.set("wow");
    }
}
