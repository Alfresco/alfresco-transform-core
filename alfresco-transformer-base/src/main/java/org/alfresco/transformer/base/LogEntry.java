/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer.base;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides setter and getter methods to allow the current Thread to set various log properties and for these
 * values to be retrieved. The {@link #complete()} method should be called at the end of a request to flush the
 * current entry to an internal log Collection of the latest entries. The {@link #getLog()} method is used to obtain
 * access to this collection.
 */
public class LogEntry
{
    private static final AtomicInteger count = new AtomicInteger(0);
    private static final Deque<LogEntry> log = new ConcurrentLinkedDeque<>();
    private static final int MAX_LOG_SIZE = 10;

    private static ThreadLocal<LogEntry> currentLogEntry = new ThreadLocal<LogEntry>()
    {
        @Override
        protected LogEntry initialValue()
        {
            LogEntry logEntry = new LogEntry();
            if (log.size() >= MAX_LOG_SIZE)
            {
                log.removeLast();
            }
            log.addFirst(logEntry);
            return logEntry;
        }
    };

    private final int id = count.incrementAndGet();
    private final long start = System.currentTimeMillis();
    private int statusCode;

    private long durationStreamIn;
    private long durationTransform;
    private long durationStreamOut;

    private String source;
    private long sourceSize;
    private String target;
    private long targetSize;
    private String options;
    private String message;

    public static Collection<LogEntry> getLog()
    {
        return log;
    }

    public static void start()
    {
        currentLogEntry.get();
    }

    public static void setSource(String source, long sourceSize)
    {
        LogEntry logEntry = currentLogEntry.get();
        logEntry.source = getExtension(source);
        logEntry.sourceSize = sourceSize;
        logEntry.durationStreamIn = System.currentTimeMillis() - logEntry.start;
    }

    public static void setTarget(String target)
    {
        currentLogEntry.get().target = getExtension(target);
    }

    private static String getExtension(String filename)
    {
        int i = filename.lastIndexOf('.');
        if (i != -1)
        {
            filename = filename.substring(i+1);
        }
        return filename;
    }

    public static void setTargetSize(long targetSize)
    {
        currentLogEntry.get().targetSize = targetSize;
    }

    public static void setOptions(String options)
    {
        currentLogEntry.get().options = options;
    }

    public static void setStatusCodeAndMessage(int statusCode, String message)
    {
        LogEntry logEntry = currentLogEntry.get();
        logEntry.statusCode = statusCode;
        logEntry.message = message;
        logEntry.durationTransform = System.currentTimeMillis() - logEntry.start - logEntry.durationStreamIn;
    }

    public static void complete()
    {
        LogEntry logEntry = currentLogEntry.get();
        logEntry.durationStreamOut = System.currentTimeMillis() - logEntry.start - logEntry.durationStreamIn - logEntry.durationTransform;
        currentLogEntry.remove();
    }

    public int getId()
    {
        return id;
    }

    public Date getDate()
    {
        return new Date(start);
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getDuration()
    {
        return time(durationStreamIn  +  durationTransform  +  durationStreamOut)+" ("+
                time(durationStreamIn)+' '+time(durationTransform)+' '+time(durationStreamOut)+")";
    }

    public long getDurationStreamIn()
    {
        return durationStreamIn;
    }

    public long getDurationTransform()
    {
        return durationTransform;
    }

    public long getDurationStreamOut()
    {
        return durationStreamOut;
    }

    public String getSource()
    {
        return source;
    }

    public String getSourceSize()
    {
        return size(sourceSize);
    }

    public String getTarget()
    {
        return target;
    }

    public String getTargetSize()
    {
        return size(targetSize);
    }

    public String getOptions()
    {
        return options;
    }

    public String getMessage()
    {
        return message;
    }

    private String time(long ms)
    {
        return size(ms, "1 ms",
            new String[] { "ms",  "s",   "min",       "hr" },
            new long[]   {       1000, 60*1000, 60*60*1000, Long.MAX_VALUE});
    }

    private String size(long size)
    {
        return size(size, "1 byte",
            new String[] { "bytes", "KB",      "MB",           "GB",                "TB" },
            new long[]   {          1024, 1024*1024, 1024*1024*1024, 1024*1024*1024*1024, Long.MAX_VALUE });
    }

    private String size(long size, String singleValue, String[] units, long[] dividers)
    {
        if (size == 1)
        {
            return singleValue;
        }
        long divider = 1;
        for(int i = 0; i < units.length-1; i++)
        {
            long nextDivider = dividers[i];
            if(size < nextDivider)
            {
                return unitFormat(size, divider, units[i]);
            }
            divider = nextDivider;
        }
        return unitFormat(size, divider, units[units.length-1]);
    }

    private String unitFormat(long size, long divider, String unit)
    {
        size = size * 10 / divider;
        int decimalPoint = (int) size % 10;

        StringBuilder sb = new StringBuilder();
        sb.append(size/10);
        if (decimalPoint != 0)
        {
            sb.append(".");
            sb.append(decimalPoint);
        }
        sb.append(' ');
        sb.append(unit);

        return sb.toString();
    }
}
