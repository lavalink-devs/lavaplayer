module com.sedmelluq.lava.common {
    requires java.base;
    requires org.slf4j;
    requires org.apache.commons.io;

    exports com.sedmelluq.lava.common.natives;
    exports com.sedmelluq.lava.common.natives.architecture;
    com.sedmelluq.lava.common.tools;
}
