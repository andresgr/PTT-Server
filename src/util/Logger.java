package util;

import java.util.logging.*;

/**
 * Standard logging methods.
 *
 * @author Andres Garcia
 */
public class Logger
{
    private java.util.logging.Logger loggerDelegate = null;

    /**
     * Base constructor
     *
     * @param logger the implementation specific logger delegate that this
     * Logger instance should be created around.
     */
    private Logger(java.util.logging.Logger logger)
    {
        this.loggerDelegate = logger;
    }

    /**
     * Find or create a logger for the specified class.  If a logger has
     * already been created for that class it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the logging configuration and it will be configured
     * to also send logging output to its parent's handlers.
     * <p>
     * @param clazz The creating class.
     * <p>
     * @return a suitable Logger
     * @throws NullPointerException if the name is null.
     */
    public static Logger getLogger(Class clazz)
        throws NullPointerException
    {
        return getLogger(clazz.getName());
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the logging configuration and it will be configured
     * to also send logging output to its parent's handlers.
     * <p>
     * @param name A name for the logger. This should be a dot-separated name
     * and should normally be based on the class name of the creator, such as
     * "es.umu.mvia.imsclient.MyFunnyClass"
     * <p>
     * @return a suitable Logger
     * @throws NullPointerException if the name is null.
     */
    public static Logger getLogger(String name)
        throws NullPointerException
    {
        return new Logger(java.util.logging.Logger.getLogger(name));

    }


    /**
     * Logs an entry in the calling method.
     */
    public void logEntry()
    {
        if (loggerDelegate.isLoggable(Level.FINEST)) {
            StackTraceElement caller = new Throwable().getStackTrace()[1];
            //loggerDelegate.log(Level.FINEST, "[entry] " + caller.getMethodName());
            System.out.println("[entry] " + caller.getMethodName());
        }
    }

    /**
     * Logs exiting the calling method
     */
    public void logExit()
    {
        if (loggerDelegate.isLoggable(Level.FINEST)) {
            StackTraceElement caller = new Throwable().getStackTrace()[1];
            //loggerDelegate.log(Level.FINEST, "[exit] " + caller.getMethodName());
            System.out.println("[exit] " + caller.getMethodName());
        }
    }

    /**
     * Check if a message with a TRACE level would actually be logged by this
     * logger.
     * <p>
     * @return true if the TRACE level is currently being logged
     */
    public boolean isTraceEnabled()
    {
        return loggerDelegate.isLoggable(Level.FINER);
    }

    /**
     * Log a TRACE message.
     * <p>
     * If the logger is currently enabled for the TRACE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    public void trace(Object msg)
    {
        //loggerDelegate.finer(msg!=null?msg.toString():"null");
        System.out.println("[trace] " + msg.toString());
    }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * @param msg   The message to log
     * @param   t   Throwable associated with log message.
     */
    public void trace(Object msg, Throwable t)
    {
        //loggerDelegate.log(Level.FINER, msg!=null?msg.toString():"null", t);
        System.out.println("[trace] " + msg.toString());
    }

    /**
     * Check if a message with a DEBUG level would actually be logged by this
     * logger.
     * <p>
     * @return true if the DEBUG level is currently being logged
     */
    public boolean isDebugEnabled()
    {
        return loggerDelegate.isLoggable(Level.FINE);
    }

    /**
     * Log a DEBUG message.
     * <p>
     * If the logger is currently enabled for the DEBUG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    public void debug(Object msg)
    {
        //loggerDelegate.fine(msg!=null?msg.toString():"null");
        System.out.println("[debug] " + msg.toString());
    }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * @param msg    The message to log
     * @param t  Throwable associated with log message.
     */
    public void debug(Object msg, Throwable t)
    {
        //loggerDelegate.log(Level.FINE, msg!=null?msg.toString():"null", t);
        System.out.println("[debug] " + msg.toString());
    }

    /**
     * Check if a message with an INFO level would actually be logged by this
     * logger.
     *
     * @return true if the INFO level is currently being logged
     */
    public boolean isInfoEnabled()
    {
        return loggerDelegate.isLoggable(Level.INFO);
    }

    /**
     * Log a INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    public void info(Object msg)
    {
        //loggerDelegate.info(msg!=null?msg.toString():"null");
        System.out.println("[info] " + msg.toString());
    }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void info(Object msg, Throwable t)
    {
        //loggerDelegate.log(Level.INFO, msg!=null?msg.toString():"null", t);
        System.out.println("[info] " + msg.toString());
    }

    /**
     * Log a WARN message.
     * <p>
     * If the logger is currently enabled for the WARN message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    public void warn(Object msg)
    {
        //loggerDelegate.warning(msg!=null?msg.toString():"null");
        System.out.println("[warn] " + msg.toString());
    }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void warn(Object msg, Throwable t)
    {
        //loggerDelegate.log(Level.WARNING, msg!=null?msg.toString():"null", t);
        System.out.println("[warn] " + msg.toString());
    }

    /**
     * Log a ERROR message.
     * <p>
     * If the logger is currently enabled for the ERROR message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    public void error(Object msg)
    {
        //loggerDelegate.severe(msg!=null?msg.toString():"null");
        System.out.println("[error] " + msg.toString());
    }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void error(Object msg, Throwable t)
    {
        //loggerDelegate.log(Level.SEVERE, msg!=null?msg.toString():"null", t);
        System.out.println("[error] " + msg.toString());
    }

    /**
     * Log a FATAL message.
     * <p>
     * If the logger is currently enabled for the FATAL message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log
     */
    public void fatal(Object msg)
    {
        //loggerDelegate.severe(msg!=null?msg.toString():"null");
        System.out.println("[fatal] " + msg.toString());
    }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void fatal(Object msg, Throwable t)
    {
        //loggerDelegate.log(Level.SEVERE, msg!=null?msg.toString():"null", t);
        System.out.println("[fatal] " + msg.toString());
    }

    /**
     * Set logging level for all handlers to FATAL
     */
    public void setLevelFatal()
    {
        setLevel(Level.SEVERE);
    }

    /**
     * Set logging level for all handlers to ERROR
     */
    public void setLevelError()
    {
        setLevel(Level.SEVERE);
    }

    /**
     * Set logging level for all handlers to WARNING
     */
    public void setLevelWarn()
    {
        setLevel(Level.WARNING);
    }

    /**
     * Set logging level for all handlers to INFO
     */
    public void setLevelInfo()
    {
        setLevel(Level.INFO);
    }

    /**
     * Set logging level for all handlers to DEBUG
     */
    public void setLevelDebug()
    {
        setLevel(Level.FINE);
    }

    /**
     * Set logging level for all handlers to TRACE
     */
    public void setLevelTrace()
    {
        setLevel(Level.FINER);
    }

    /**
     * Set logging level for all handlers to ALL (allow all log messages)
     */
    public void setLevelAll()
    {
        setLevel(Level.ALL);
    }

    /**
     * Set logging level for all handlers to OFF (allow no log messages)
     */
    public void setLevelOff()
    {
        setLevel(Level.OFF);
    }

    /**
     * Set logging level for all handlers to <tt>level</tt>
     *
     * @param level the level to set for all logger handlers
     */
    private void setLevel(java.util.logging.Level level)
    {
        Handler[] handlers = loggerDelegate.getHandlers();
        for (int i = 0; i < handlers.length; i++)
        {
            handlers[i].setLevel(level);
        }
        loggerDelegate.setLevel(level);
    }
}
