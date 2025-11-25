package fp.io;

import fp.util.Either;
import fp.util.Right;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ActorTest {

    private static final Logger LOG = Logger.getLogger(ActorTest.class.getName());

    final static DefaultPlatform platform = new DefaultPlatform();

    @AfterAll
    public static void setUp() {
        platform.shutdown();
    }

    @Test
    void testActor() {
        final Stream<Statement> stream = Stream.of(
                new Inc(10),
                new Dec(7),
                new Inc(3),
                new Inc(5),
                new Dec(2),
                new Dec(4),
                new Inc(9),
                new Dec(7),
                new Inc(8),
                new Dec(6)
        );
        IO<Void, Integer> io = IO.effectTotal(() ->
                Actor.unbounded(0, this::calcState)
        ).flatMap(actor ->
                IO.sequence(stream.map(statement ->
                                IO.effectTotal(() -> actor.sendMessage(statement)).delay(100_000_000L)
                        ))
                        .fork()
                        .flatMap(fiber ->
                                IO.call(actor::run)
                                        .timeout(1_100_000_000L)
                                        .recoverCause(result -> {
                                            fiber.join();
                                            return IO.succeed(actor.get());
                                        })
                        ));

        final Runtime defaultRuntime =
                new DefaultRuntime(null, platform);

        final Either<Cause<Void>, Integer> name =
                defaultRuntime.unsafeRun(io);

        Assertions.assertEquals(Right.of(9), name);
    }

    public interface Statement {
    }

    private record Inc(int value) implements Statement {
    }

    private record Dec(int value) implements Statement {
    }

    private Integer calcState(
            final Integer state,
            final Statement statement
    ) {
        return switch (statement) {
            case Inc inc -> state + inc.value;
            case Dec dec -> state - dec.value;
            default -> state;
        };
    }

    private static class Actor<STATE, T> {
        private final LinkedBlockingDeque<T> blockingDeque;
        private final BiFunction<STATE, T, STATE> handleMessage;
        private STATE state;

        public Actor(
                final LinkedBlockingDeque<T> blockingDeque,
                final STATE init,
                final BiFunction<STATE, T, STATE> handleMessage
        ) {
            this.blockingDeque = blockingDeque;
            this.state = init;
            this.handleMessage = handleMessage;
            LOG.fine(() -> "Actor created with init value: " + init);
        }

        public static <STATE, T> Actor<STATE, T> unbounded(
                final STATE init,
                final BiFunction<STATE, T, STATE> handleMessage
        ) {
            return new Actor<>(new LinkedBlockingDeque<>(), init, handleMessage);
        }

        public IO<Void, STATE> run() {
            try {
                LOG.fine(() -> "Actor wait next value");
                final T first = blockingDeque.take();
                LOG.fine(() -> "Actor got value: " + first);
                state = handleMessage.apply(state, first);
                LOG.fine(() -> "Actor's new state: " + state);
                return IO.call(this::run);
            } catch (InterruptedException ignored) {
                LOG.fine(() -> "Actor has stopped with state: " + state);
                return IO.succeed(state);
            }
        }

        public void sendMessage(T value) {
            LOG.fine(() -> "Actor receive message: " + value);
            blockingDeque.offer(value);
        }

        public STATE get() {
            return state;
        }
    }
}
