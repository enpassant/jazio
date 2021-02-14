package fp.util;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public final class StreamUtil {
    public static <T, R> Function<Stream<T>, R> reduce(
        R init,
        BiFunction<R, T, R> fn
    ) {
        return items -> {
            R result = items.reduce(
                init,
                fn,
                (u1, u2) -> u1
            );
            items.close();
            return result;
        };
    }

    public static <T> Consumer<Stream<T>> consume(
        Consumer<Stream<T>> fn
    ) {
        return items -> {
            fn.accept(items);
            items.close();
        };
    }

    public static <T> void consumeToOutputStream(
        OutputStream os,
        Stream<T> stream,
        String prefix,
        String suffix,
        Consumer<Tuple2<PrintWriter, T>> fn
    ) {
        try ( PrintWriter writer = new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter( os, Charset.forName("UTF-8") )
        ) ) ) {
            writer.print(prefix);
            stream.forEach(item -> fn.accept(Tuple2.of(writer, item)));
            stream.close();
            writer.print( suffix );
        }
    }
}

