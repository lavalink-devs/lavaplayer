module com.sedmelluq.lava.extensions.youtuberotator {
    requires java.base;
    requires org.slf4j;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;

    exports com.sedmelluq.lava.extensions.youtuberotator;
    exports com.sedmelluq.lava.extensions.youtuberotator.tools;
    exports com.sedmelluq.lava.extensions.youtuberotator.tools.ip;
    exports com.sedmelluq.lava.extensions.youtuberotator.planner;
}
