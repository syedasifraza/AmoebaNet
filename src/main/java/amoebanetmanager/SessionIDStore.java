package amoebanetmanager;


class SessionIDStore {
    private static Long sessionId;

    private static boolean queryId;

    boolean isQueryId() {
        return queryId;
    }

    void setQueryId(boolean queryId) {
        SessionIDStore.queryId = queryId;
    }

    Long getSessionId() {
        return sessionId;
    }

    void setSessionId(Long sessionId) {
        SessionIDStore.sessionId = sessionId;
    }

}
