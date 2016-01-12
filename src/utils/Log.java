/**
 * @(#)Log.java	1.26 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package utils;
import java.io.*;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Set;
import java.text.MessageFormat;


/**
 * A class for error logs. Reports errors and warnings, and
 *  keeps track of error numbers and positions.
 */
public class Log implements LayoutCharacters {

    /**
     * The context key for the log.
     */
    private static final Context.Key logKey = new Context.Key();

    /**
     * The context key for the output PrintWriter.
     */
    public static final Context.Key outKey = new Context.Key();
    public final PrintWriter errWriter;
    public final PrintWriter warnWriter;
    public final PrintWriter noticeWriter;

    /**
     * The maximum number of errors/warnings that are reported,
     *  can be reassigned from outside.
     */
    private final int MaxErrors;
    private final int MaxWarnings;

    /**
     * Switch: prompt user on each error.
     */
    public boolean promptOnError;

    /**
     * Switch: emit warning messages.
     */
    public boolean emitWarnings;

    /**
     * Construct a log with given I/O redirections.
     */
    protected Log(Context context, PrintWriter errWriter, PrintWriter warnWriter,
            PrintWriter noticeWriter) {
        super();
        context.put(logKey, this);
        this.errWriter = errWriter;
        this.warnWriter = warnWriter;
        this.noticeWriter = noticeWriter;
        Options options = Options.instance(context);
        this.promptOnError = options.get("-prompt") != null;
        this.emitWarnings = options.get("-nowarn") == null;
        
        this.MaxErrors = getIntOption(options, "-Xmaxerrs", 100);
        this.MaxWarnings = getIntOption(options, "-Xmaxwarns", 100);
    }

    private int getIntOption(Options options, String optionName, int defaultValue) {
        String s = (String) options.get(optionName);
        try {
            if (s != null)
                return Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    /**
      * The default writer for diagnostics
      */
    static final PrintWriter defaultWriter(Context context) {
        PrintWriter result = (PrintWriter) context.get(outKey);
        if (result == null)
            context.put(outKey, result = new PrintWriter(System.err));
        return result;
    }

    /**
      * Construct a with default settings.
      */
    protected Log(Context context) {
        this(context, defaultWriter(context));
    }

    /**
      * Construct a with all output redirected.
      */
    protected Log(Context context, PrintWriter defaultWriter) {
        this(context, defaultWriter, defaultWriter, defaultWriter);
    }

    /**
      * Get the Log instance for this context.
      */
    public static Log instance(Context context) {
        Log instance = (Log) context.get(logKey);
        if (instance == null)
            instance = new Log(context);
        return instance;
    }

    /**
      * The name of the file that's currently translated.
      */
    private Name sourcename;

    /**
     * The number of errors encountered so far.
     */
    public int nerrors = 0;

    /**
     * The number of warnings encountered so far.
     */
    public int nwarnings = 0;

    /**
     * A set of all errors generated so far. This is used to avoid printing an
     *  error message more than once. For each error, a pair consisting of the
     *  source file name and source code position of the errir is added to the set.
     */
    private Set<Pair> recorded = new HashSet<>();

    /**
     * The buffer containing the file that's currently translated.
     */
    private byte[] buf = null;

    /**
     * The position in the buffer at which last error was reported
     */
    private int bp;

    /**
     * the source line at which last error was reported.
     */
    private int lastLine;

    /**
     * Re-assign source name, returning previous setting.
     */
    public Name useSource(Name name) {
        Name prev = sourcename;
        sourcename = name;
        if (prev != sourcename)
            buf = null;
        return prev;
    }

    /**
      * Return current source name.
      */
    public Name currentSource() {
        return sourcename;
    }

    /**
      * Flush the logs
      */
    public void flush() {
        errWriter.flush();
        warnWriter.flush();
        noticeWriter.flush();
    }

    /**
      * Prompt user after an error.
      */
    public void prompt() {
        if (promptOnError) {
            System.err.println(getLocalizedString("resume.abort"));
            char ch;
            try {
                while (true) {
                    switch (System.in.read()) {
                    case 'a':

                    case 'A':
                        System.exit(-1);
                        return;

                    case 'r':

                    case 'R':
                        return;

                    case 'x':

                    case 'X':
                        throw new AssertionError("user abort");

                    default:

                    }
                }
            } catch (IOException e) {
            }
        }
    }

    /**
      * Print a faulty source code line and point to the error.
      *  @param line    The line number of the printed line in the current buffer.
      *  @param col     The number of the column to be highlighted as error position.
      */
    private void printErrLine(int line, int col, PrintWriter writer) {
        try {
            if (buf == null) {
                FileInputStream in = new FileInputStream(sourcename.toString());
                buf = new byte[in.available()];
                in.read(buf);
                in.close();
                bp = 0;
                lastLine = 1;
            } else if (lastLine > line) {
                bp = 0;
                lastLine = 1;
            }
            while (bp < buf.length && lastLine < line) {
                switch (buf[bp]) {
                case CR:
                    bp++;
                    if (bp < buf.length && buf[bp] == LF)
                        bp++;
                    lastLine++;
                    break;

                case LF:
                    bp++;
                    lastLine++;
                    break;

                default:
                    bp++;

                }
            }
            int lineEnd = bp;
            while (lineEnd < buf.length && buf[lineEnd] != CR && buf[lineEnd] != LF)
                lineEnd++;
            printLines(writer, new String(buf, bp, lineEnd - bp));
            if (col == 0)
                col = 1;
            byte[] ptr = new byte[col];
            for (int i = 0; i < col - 1; i++)
                ptr[i] = (byte)' ';
            ptr[col - 1] = (byte)'^';
            printLines(writer, new String(ptr, 0, col));
        } catch (IOException e) {
            printLines(errWriter, getLocalizedString("source.unavailable"));
        }
        finally { errWriter.flush();
                } }

    /**
      * Print the text of a message, translating newlines approprately
      *  for the platform.
      */
    public static void printLines(PrintWriter writer, String msg) {
        int nl;
        while ((nl = msg.indexOf('\n')) != -1) {
            writer.println(msg.substring(0, nl));
            msg = msg.substring(nl + 1);
        }
        if (msg.length() != 0)
            writer.println(msg);
    }

    /**
      * Print an error or warning message.
      *  @param pos    The source position at which to report the message.
      *  @param msg    The message to report.
      */
    private void printError(int pos, String msg, PrintWriter writer) {
        if (pos == Position.NOPOS) {
            writer.print( getText("compiler.err.error", null, null, null, null, null,
                    null, null));
            printLines(writer, msg);
        } else {
            int line = Position.line(pos);
            int col = Position.column(pos);
            printLines(writer, sourcename + ":" + line + ": " + msg);
            printErrLine(line, col, writer);
        }
        writer.flush();
    }

    /**
      * Report an error, unless another error was already reported at same
      *  source position.
      *  @param pos    The source position at which to report the error.
      *  @param key    The error message to report.
      */
    public void error(int pos, String key) {
        error(pos, key, null, null, null, null, null, null, null);
    }

    public void error(int pos, String key, String arg0) {
        error(pos, key, arg0, null, null, null, null, null, null);
    }

    public void error(int pos, String key, String arg0, String arg1) {
        error(pos, key, arg0, arg1, null, null, null, null, null);
    }

    public void error(int pos, String key, String arg0, String arg1, String arg2) {
        error(pos, key, arg0, arg1, arg2, null, null, null, null);
    }

    public void error(int pos, String key, String arg0, String arg1, String arg2,
            String arg3) {
        error(pos, key, arg0, arg1, arg2, arg3, null, null, null);
    }

    public void error(int pos, String key, String arg0, String arg1, String arg2,
            String arg3, String arg4) {
        error(pos, key, arg0, arg1, arg2, arg3, arg4, null, null);
    }

    public void error(int pos, String key, String arg0, String arg1, String arg2,
            String arg3, String arg4, String arg5) {
        error(pos, key, arg0, arg1, arg2, arg3, arg4, arg5, null);
    }

    public void error(int pos, String key, String arg0, String arg1, String arg2,
            String arg3, String arg4, String arg5, String arg6) {
        if (nerrors < MaxErrors) {
            Pair coords = new Pair(sourcename, new Integer(pos));
            if (!recorded.contains(coords)) {
                recorded.add(coords);
                String msg = getText("compiler.err." + key, arg0, arg1, arg2, arg3,
                        arg4, arg5, arg6);
                printError(pos, msg, errWriter);
                prompt();
                nerrors++;
            }
        }
    }

    /**
      * Report a warning.
      *  @param pos    The source position at which to report the warning.
      *  @param key    The key for the localized warning message.
      */
    public void warning(int pos, String key) {
        warning(pos, key, null, null, null, null);
    }

    public void warning(int pos, String key, String arg0) {
        warning(pos, key, arg0, null, null, null);
    }

    public void warning(int pos, String key, String arg0, String arg1) {
        warning(pos, key, arg0, arg1, null, null);
    }

    public void warning(int pos, String key, String arg0, String arg1, String arg2) {
        warning(pos, key, arg0, arg1, arg2, null);
    }

    public void warning(int pos, String key, String arg0, String arg1,
            String arg2, String arg3) {
        if (nwarnings < MaxWarnings && emitWarnings) {
            String msg = getText("compiler.warn." + key, arg0, arg1, arg2, arg3,
                    null, null, null);
            printError(pos,
                    getText("compiler.warn.warning", null, null, null, null,
                    null, null, null) + msg, warnWriter);
        }
        nwarnings++;
    }

    /**
      * Provide a non-fatal notification.
      *  @param key     The notification to send to System.err.
      */
    public void note(String key) {
        note(key, null);
    }

    public void note(String key, String arg0) {
        if (emitWarnings) {
            noticeWriter.print(
                    getText("compiler.note.note", null, null, null, null, null,
                    null, null));
            String msg = getText("compiler.note." + key, arg0, null, null, null,
                    null, null, null);
            printLines(noticeWriter, msg);
            noticeWriter.flush();
        }
    }

    /**
      * Find a localized string in the resource bundle.
      *  @param key     The key for the localized string.
      */
    public static String getLocalizedString(String key) {
        return getText("compiler.misc." + key, null, null, null, null, null,
                null, null);
    }

    public static String getLocalizedString(String key, String arg0) {
        return getText("compiler.misc." + key, arg0, null, null, null, null,
                null, null);
    }

    public static String getLocalizedString(String key, String arg0, String arg1) {
        return getText("compiler.misc." + key, arg0, arg1, null, null, null,
                null, null);
    }

    public static String getLocalizedString(String key, String arg0, String arg1,
            String arg2) {
        return getText("compiler.misc." + key, arg0, arg1, arg2, null, null,
                null, null);
    }

    public static String getLocalizedString(String key, String arg0, String arg1,
            String arg2, String arg3) {
        return getText("compiler.misc." + key, arg0, arg1, arg2, arg3, null,
                null, null);
    }
    private static final String compilerRB = "com.sun.tools.javac.v8.resources.compiler";
    private static ResourceBundle messageRB;

    /**
     * Initialize ResourceBundle.
     */
    private static void initResource() {
        try {
            messageRB = ResourceBundle.getBundle(compilerRB);
        } catch (MissingResourceException e) {
            throw new Error("Fatal: Resource for compiler is missing");
        }
    }

    /**
      * Get and format message string from resource.
      */
    public static String getText(String key, String arg0, String arg1,
            String arg2, String arg3, String arg4, String arg5, String arg6) {
        if (messageRB == null)
            initResource();
        try {
            String[] args = {arg0, arg1, arg2, arg3, arg4, arg5, arg6};
            return MessageFormat.format(messageRB.getString(key), args);
        } catch (MissingResourceException e) {
            if (arg0 == null)
                arg0 = "null";
            if (arg1 == null)
                arg1 = "null";
            if (arg2 == null)
                arg2 = "null";
            if (arg3 == null)
                arg3 = "null";
            if (arg4 == null)
                arg4 = "null";
            if (arg5 == null)
                arg5 = "null";
            if (arg6 == null)
                arg6 = "null";
            String[] args = {key, arg0, arg1, arg2, arg3, arg4, arg5, arg6};
            String msg = "compiler message file broken: key={0} arguments={1}, {2}, {3}, {4}, {5}, {6}, {7}";
            return MessageFormat.format(msg, args);
        }
    }

    /**
      * print an error or warning message:
      */
    private void printRawError(int pos, String msg) {
        if (pos == Position.NOPOS) {
            printLines(errWriter, "error: " + msg);
        } else {
            int line = Position.line(pos);
            int col = Position.column(pos);
            printLines(errWriter, sourcename + ":" + line + ": " + msg);
            printErrLine(line, col, errWriter);
        }
        errWriter.flush();
    }

    /**
      * report an error:
      */
    public void rawError(int pos, String msg) {
        if (nerrors < MaxErrors) {
            Pair coords = new Pair(sourcename, new Integer(pos));
            if (!recorded.contains(coords)) {
                recorded.add(coords);
                printRawError(pos, msg);
                prompt();
                nerrors++;
            }
        }
        errWriter.flush();
    }

    /**
      * report a warning:
      */
    public void rawWarning(int pos, String msg) {
        if (nwarnings < MaxWarnings && emitWarnings) {
            printRawError(pos, "warning: " + msg);
        }
        nwarnings++;
        errWriter.flush();
    }
}
