package io.ochubey.appium;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.OutputStream;

import static org.apache.log4j.Logger.getLogger;

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
        this.logger = getLogger("outLog");
        this.level = Level.ALL;
        mem = "";
    }

    /**
     * Writes a byte to the output stream. This method flushes automatically at the end of a line.
     *
     * @param b writer
     */
    @Override
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
