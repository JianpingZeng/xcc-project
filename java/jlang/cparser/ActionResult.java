package jlang.cparser;

import tools.Util;

/**
 * This class is served as the wrapper which contains a statement or
 * expression, and known if this expression or stmt is invalid or not
 * according invoking some method.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class ActionResult<T>
{
    /**
     * Indicates if this stmt or subExpr is invalid.
     */
    private boolean isInvalid;
    /**
     * Indicates the data of this class enclosing.
     */
    private T data;

    public ActionResult()
    {
        this(false);
    }

    public ActionResult(boolean isInvalid)
    {
        this(null, isInvalid);
    }

    public ActionResult(T data)
    {
        this(data, false);
        Util.assertion(data != null,  "Bad pointer!");
    }

    public ActionResult(T data, boolean isInvalid)
    {
        this.data = data;
        this.isInvalid = isInvalid;
    }

    public boolean isInvalid() { return isInvalid; }
    public T get() { return data; }
    public void set(T newData) { this.data =newData; }

    public boolean isUsable()
    {
        return data != null;
    }


    private static ActionResult emptyInstace = null;

    public static <T> ActionResult empty()
    {
        if (emptyInstace == null)
            emptyInstace = new ActionResult<T>();
        return emptyInstace;
    }

    public void release()
    {
        data = null;
    }
}
