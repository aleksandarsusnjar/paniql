package net.susnjar.paniql.pricing;

public enum WorkType {
    INSTANCE_ACCESS("access", "Raw#", "Total count of instance accesses."),
    TRIVIAL_RETURN("trivial", "Basic", "Trivial derivations from prefetched data."),
    LOCAL_CALL("local", "Local", "Same-machine in- or inter-process calls."),
    BULK_JOIN("join", "Joins", "Bulk data gathers such as DB joins."),
    EFFICIENT_REMOTE_API_CALL("fast", "Fast", "Efficient remote API calls, eg. DB, GraphQL."),
    INEFFICIENT_REMOTE_API_CALL("slow", "Slow", "Inefficient remote API calls, e.g. REST.");

    private final String id;
    private final String heading;
    private final String description;

    WorkType(String id, String heading, String description) {
        this.id = id;
        this.heading = heading;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getHeading() {
        return heading;
    }

    public String getDescription() {
        return description;
    }

    public static int getMaxHeadingLength() {
        int max = 0;
        for (WorkType workType: values()) {
            final int len = workType.getHeading().length();
            if (max < len) max = len;
        }
        return max;
    }
}
