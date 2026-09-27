/*
 * Copyright 2012-2026, Dyanet Inc., Akber A. Choudhry,
 *   and other individual contributors identified by the
 *   @authors tag in each source artefact.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *   either express or implied.
 *   See the License for the specific language governing permissions
 *   and limitations under the License.
 */

package com.dyanet.osrs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyanet.osrs.xcp.XcpRequest;
import com.dyanet.osrs.xcp.XcpResponse;

/**
 * A queue that sends commands to OpenSRS strictly one at a time, in the order they were
 * submitted. A command starts only after the previous one has completely finished (reply
 * received, retries done), so a slow OpenSRS never has several of this session's commands in
 * flight, and later commands never overtake earlier ones.
 *
 * <p><b>If a command fails, the queue is emptied.</b> Every command still waiting behind it is
 * cancelled without being sent, its future completes with an
 * {@link OsrsRequestCancelledException}, and the session's {@linkplain #onFlush(Consumer) flush
 * listener} receives a {@link QueueFlush} whose {@link QueueFlush#message() message} explains,
 * in plain language, that the later requests were removed to keep the account consistent.
 * Commands submitted after that run normally.
 *
 * <pre>{@code
 * try (OsrsSession s = client.openSession("order 1042")) {
 *     s.onFlush(f -> notifyUser(f.message()));
 *     s.submit("register example.com", c -> Domains.on(c).register(...));
 *     s.submit("set DNS for example.com", c -> Dns.on(c).setZone(...));
 *     s.drain();   // wait for both, or for the failure
 * }
 * }</pre>
 *
 * <p>A "failure" is any exception from the command: OpenSRS reporting {@code is_success=0}
 * (through {@link OsrsClient#execute(XcpRequest)}), a transport or protocol error, or an
 * exception thrown by a submitted function. A lookup that finds a name taken is an answer, not
 * a failure.
 *
 * <p>Sessions are thread-safe and cheap; each runs its queue on its own virtual thread while it
 * has work. Different sessions (and direct calls on the client) are independent.
 */
public final class OsrsSession implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OsrsSession.class);

    private record Task<T>(String description, Function<OsrsClient, T> command, CompletableFuture<T> future) {
    }

    private final OsrsClient client;
    private final String name;
    private final Deque<Task<?>> queue = new ArrayDeque<>();
    private volatile Consumer<QueueFlush> flushListener = f -> { };
    private volatile QueueFlush lastFlush;
    private boolean running;
    private boolean closed;

    OsrsSession(OsrsClient client, String name) {
        this.client = Objects.requireNonNull(client, "client");
        this.name = name == null || name.isBlank() ? "session" : name;
    }

    /**
     * @return the session's name, used in messages
     */
    public String getName() {
        return name;
    }

    /**
     * @param listener called (on the session's thread) each time a failure empties the queue
     * @return this session
     */
    public OsrsSession onFlush(Consumer<QueueFlush> listener) {
        this.flushListener = Objects.requireNonNull(listener, "listener");
        return this;
    }

    /**
     * @return the most recent flush, or {@code null} if no command has failed yet
     */
    public QueueFlush getLastFlush() {
        return lastFlush;
    }

    /**
     * Queues a command; {@link OsrsClient#execute(XcpRequest)} semantics (a failed reply fails
     * the future and empties the queue).
     *
     * @param request the command
     * @return completes with the reply once the command has run
     */
    public CompletableFuture<XcpResponse> submit(XcpRequest request) {
        return submit(describe(request), c -> c.execute(request));
    }

    /**
     * Queues any work that uses the client, typically a command-family call.
     *
     * @param description a short, human-readable name for the command, used in messages
     *                    (e.g. {@code "renew example.com for 2 years"}); don't include secrets
     * @param command     the work; it runs alone, after everything submitted before it
     * @param <T>         the result type
     * @return completes with the command's result once it has run
     * @throws IllegalStateException if the session is closed
     */
    public <T> CompletableFuture<T> submit(String description, Function<OsrsClient, T> command) {
        Objects.requireNonNull(command, "command");
        Task<T> t = new Task<>(description == null || description.isBlank() ? "request" : description,
            command, new CompletableFuture<>());
        boolean start;
        synchronized (queue) {
            if (closed) {
                throw new IllegalStateException("The session \"" + name + "\" is closed");
            }
            queue.addLast(t);
            start = !running;
            running = true;
        }
        if (start) {
            Thread.ofVirtual().name("osrs-session-" + name).start(this::runQueue);
        }
        return t.future;
    }

    /**
     * Queues a command and waits for it.
     *
     * @param request the command
     * @return the reply
     * @throws OsrsException                 the command's own failure
     * @throws OsrsRequestCancelledException if an earlier command failed and this one was cancelled
     */
    public XcpResponse call(XcpRequest request) {
        return join(submit(request));
    }

    /**
     * Queues work and waits for it.
     *
     * @param description a short, human-readable name for the command
     * @param command     the work
     * @param <T>         the result type
     * @return its result
     */
    public <T> T call(String description, Function<OsrsClient, T> command) {
        return join(submit(description, command));
    }

    /**
     * @return how many commands are waiting (not counting one that is running)
     */
    public int pending() {
        synchronized (queue) {
            return queue.size();
        }
    }

    /**
     * Waits until every command submitted so far has finished or been cancelled.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public void drain() throws InterruptedException {
        CompletableFuture<?> last;
        synchronized (queue) {
            last = queue.peekLast() == null ? null : queue.peekLast().future;
        }
        if (last != null) {
            try {
                last.get();
            } catch (ExecutionException | CancellationException ignored) {
                // the outcome is on each command's own future
            }
        }
        synchronized (queue) {
            while (running) {
                queue.wait();
            }
        }
    }

    /**
     * Stops accepting commands. Commands already queued but not started are cancelled (not sent);
     * a command that is running finishes.
     */
    @Override
    public void close() {
        List<Task<?>> dropped;
        synchronized (queue) {
            closed = true;
            dropped = new ArrayList<>(queue);
            queue.clear();
        }
        for (Task<?> t : dropped) {
            t.future.completeExceptionally(new OsrsRequestCancelledException(t.description, name));
        }
    }

    private void runQueue() {
        while (true) {
            Task<?> t;
            synchronized (queue) {
                t = queue.pollFirst();
                if (t == null) {
                    running = false;
                    queue.notifyAll();
                    return;
                }
            }
            run(t);
        }
    }

    private <T> void run(Task<T> t) {
        try {
            t.future.complete(t.command.apply(client));
        } catch (Throwable e) {
            t.future.completeExceptionally(e);
            flush(t.description, e);
        }
    }

    private void flush(String failed, Throwable failure) {
        List<Task<?>> dropped;
        synchronized (queue) {
            dropped = new ArrayList<>(queue);
            queue.clear();
        }
        QueueFlush f = new QueueFlush(name, failed, failure,
            dropped.stream().map(Task::description).toList());
        lastFlush = f;
        if (dropped.isEmpty()) {
            log.warn("OpenSRS session \"{}\": \"{}\" failed", name, failed);
        } else {
            log.warn("OpenSRS session \"{}\": \"{}\" failed; cancelled {} queued request(s)",
                name, failed, dropped.size());
        }
        for (Task<?> t : dropped) {
            t.future.completeExceptionally(new OsrsRequestCancelledException(t.description, f));
        }
        try {
            flushListener.accept(f);
        } catch (RuntimeException e) {
            log.warn("OpenSRS session \"{}\": flush listener failed", name, e);
        }
    }

    static String describe(XcpRequest r) {
        Object domain = r.getAttributes().get("domain");
        String what = r.getObject() + " " + r.getAction();
        return domain instanceof CharSequence d ? what + " " + d : what;
    }

    private static <T> T join(CompletableFuture<T> f) {
        try {
            return f.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            if (e.getCause() instanceof Error er) {
                throw er;
            }
            throw new OsrsException(e.getCause().getMessage(), e.getCause());
        }
    }

    @Override
    public String toString() {
        return "OsrsSession[" + name + ", pending=" + pending() + "]";
    }
}
