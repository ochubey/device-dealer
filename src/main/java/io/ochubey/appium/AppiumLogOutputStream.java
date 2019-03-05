package io.ochubey.appium;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.OutputStream;

/**
 * Created by o.chubey on 4/19/18.
 */
public class AppiumLogOutputStream extends OutputStream {
    /**
     * The logger where to log the written bytes.
     */
    private Logger logger;

    /**
     * The level.
     */
    private Level level;

    /**
     * The internal memory for the written bytes.
     */
    private String mem;

    /**
     * Creates a new log output stream which logs bytes.
     */
    public AppiumLogOutputStream() {
        setLogger(org.apache.log4j.Logger.getLogger("outLog"));
        setLevel(Level.ALL);
        mem = "";
    }

    /**
     * Sets the logger where to log the bytes.
     *
     * @param logger the logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns the logger.
     *
     * @return DOCUMENT ME!
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets the logging level.
     *
     * @param level the level
     */
    private void setLevel(Level level) {
        this.level = level;
    }

    /**
     * Writes a byte to the output stream. This method flushes automatically at the end of a line.
     *
     * @param b writer
     */
    public void write(int b) {
        flush();
    }

    /**
     * Flushes the output stream.
     */
    @Override
    public void flush() {
        logger.log(level, mem);
        mem = "";
    }
}
