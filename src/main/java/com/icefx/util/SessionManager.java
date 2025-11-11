package com.icefx.util;

import com.icefx.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple session manager tracking the authenticated user.
 */
public final class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final AtomicReference<Session> CURRENT = new AtomicReference<>();

    private SessionManager() {
        // Utility class
    }

    public static void startSession(User user) {
        Objects.requireNonNull(user, "user");
        Session session = new Session(user, Instant.now());
        CURRENT.set(session);
        logger.info("Session started for user {} ({})", user.getUserCode(), user.getRole());
    }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(CURRENT.get()).map(Session::user);
    }

    public static Optional<Instant> getLoginTime() {
        return Optional.ofNullable(CURRENT.get()).map(Session::loginTime);
    }

    public static boolean isLoggedIn() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.getAndSet(null);
        logger.info("Session cleared");
    }

    private record Session(User user, Instant loginTime) { }
}
